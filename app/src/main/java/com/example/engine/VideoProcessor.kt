package com.example.engine

import android.graphics.Bitmap
import com.example.domain.model.EnhancementConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class VideoEnhanceProgress(
    val currentFrame: Int,
    val totalFrames: Int,
    val progressFraction: Float,
    val statusText: String,
    val currentFrameBitmap: Bitmap?,
    val isPaused: Boolean = false,
    val speedFps: Float = 0f
)

data class VideoEnhanceResult(
    val enhancedFrames: List<Bitmap>,
    val processingTimeMs: Long,
    val originalFps: Int,
    val outputFps: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val estimatedOutputSizeMb: Float
)

class VideoProcessor {

    private val isCancelled = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    fun cancel() {
        isCancelled.set(true)
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
    }

    /**
     * Parallel video frame processing engine. Uses available CPU cores to batch-process
     * video keyframes concurrently while maintaining frame ordering and smooth live progress feedback.
     */
    suspend fun processFrames(
        frames: List<Bitmap>,
        config: EnhancementConfig,
        onProgress: (VideoEnhanceProgress) -> Unit
    ): VideoEnhanceResult? = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        isCancelled.set(false)
        isPaused.set(false)

        val total = frames.size
        if (total == 0) return@withContext null

        val processedFrames = arrayOfNulls<Bitmap>(total)
        val completedCount = AtomicInteger(0)
        val concurrency = 2.coerceAtMost(Runtime.getRuntime().availableProcessors())

        // Process in concurrent chunks of size 'concurrency'
        for (chunkStart in frames.indices step concurrency) {
            if (isCancelled.get()) return@withContext null

            while (isPaused.get()) {
                if (isCancelled.get()) return@withContext null
                val count = completedCount.get()
                onProgress(
                    VideoEnhanceProgress(
                        currentFrame = count,
                        totalFrames = total,
                        progressFraction = count.toFloat() / total,
                        statusText = "Paused at frame $count of $total",
                        currentFrameBitmap = processedFrames.filterNotNull().lastOrNull() ?: frames.first(),
                        isPaused = true
                    )
                )
                delay(150)
            }

            val chunkEnd = (chunkStart + concurrency).coerceAtMost(total)
            coroutineScope {
                val jobs = (chunkStart until chunkEnd).map { idx ->
                    async {
                        if (isCancelled.get()) return@async null
                        val source = frames[idx]
                        val enhanced = ImageProcessor.enhance(source, config) { _, _ -> }
                        processedFrames[idx] = enhanced.enhancedBitmap
                        val done = completedCount.incrementAndGet()
                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                        val fps = if (elapsedSec > 0) done / elapsedSec else 0f
                        val frac = done.toFloat() / total
                        onProgress(
                            VideoEnhanceProgress(
                                currentFrame = done,
                                totalFrames = total,
                                progressFraction = frac,
                                statusText = "Enhancing frame $done/$total (${String.format("%.1f", fps)} fps)",
                                currentFrameBitmap = enhanced.enhancedBitmap,
                                isPaused = false,
                                speedFps = fps
                            )
                        )
                    }
                }
                jobs.awaitAll()
            }
        }

        if (isCancelled.get()) return@withContext null

        val finalFrames = processedFrames.filterNotNull()
        if (finalFrames.isEmpty()) return@withContext null

        val elapsed = System.currentTimeMillis() - startTime
        val sampleOut = finalFrames.first()
        val estimatedMb = (total * 0.35f).coerceAtLeast(1.2f)

        VideoEnhanceResult(
            enhancedFrames = finalFrames,
            processingTimeMs = elapsed,
            originalFps = 30,
            outputFps = config.videoFpsTarget,
            frameWidth = sampleOut.width,
            frameHeight = sampleOut.height,
            estimatedOutputSizeMb = estimatedMb
        )
    }
}
