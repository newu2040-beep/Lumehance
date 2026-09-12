package com.example.engine

import android.graphics.Bitmap
import com.example.domain.model.EnhancementConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

data class VideoEnhanceProgress(
    val currentFrame: Int,
    val totalFrames: Int,
    val progressFraction: Float,
    val statusText: String,
    val currentFrameBitmap: Bitmap?,
    val isPaused: Boolean = false
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

    suspend fun processFrames(
        frames: List<Bitmap>,
        config: EnhancementConfig,
        onProgress: (VideoEnhanceProgress) -> Unit
    ): VideoEnhanceResult? = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        isCancelled.set(false)
        isPaused.set(false)

        val total = frames.size
        val outputFrames = ArrayList<Bitmap>(total)

        for (i in frames.indices) {
            if (isCancelled.get()) {
                return@withContext null
            }

            while (isPaused.get()) {
                if (isCancelled.get()) return@withContext null
                onProgress(
                    VideoEnhanceProgress(
                        currentFrame = i + 1,
                        totalFrames = total,
                        progressFraction = (i.toFloat() / total),
                        statusText = "Paused at frame ${i + 1} of $total",
                        currentFrameBitmap = if (outputFrames.isNotEmpty()) outputFrames.last() else frames[i],
                        isPaused = true
                    )
                )
                delay(200)
            }

            val sourceFrame = frames[i]
            // Frame enhancement: apply denoise + super-res / sharpen
            val enhancedOutput = ImageProcessor.enhance(sourceFrame, config) { frac, desc ->
                // internal frame progress
            }
            outputFrames.add(enhancedOutput.enhancedBitmap)

            val frac = (i + 1).toFloat() / total
            onProgress(
                VideoEnhanceProgress(
                    currentFrame = i + 1,
                    totalFrames = total,
                    progressFraction = frac,
                    statusText = "Processing frame ${i + 1} of $total (${(frac * 100).toInt()}%)",
                    currentFrameBitmap = enhancedOutput.enhancedBitmap,
                    isPaused = false
                )
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        val sampleOut = outputFrames.first()

        // Estimate size based on bitrate: (width * height * fps * bitrate_factor)
        val estimatedMb = (total * 0.45f).coerceAtLeast(1.5f)

        VideoEnhanceResult(
            enhancedFrames = outputFrames,
            processingTimeMs = elapsed,
            originalFps = 30,
            outputFps = config.videoFpsTarget,
            frameWidth = sampleOut.width,
            frameHeight = sampleOut.height,
            estimatedOutputSizeMb = estimatedMb
        )
    }
}
