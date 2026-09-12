package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale
import kotlin.math.roundToInt

data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val frameCount: Int,
    val fps: Int,
    val hasAudio: Boolean,
    val frames: List<Bitmap>
)

object MediaHelper {

    private const val TAG = "MediaHelper"

    /**
     * Determines whether the given URI points to a video or a photo.
     */
    fun isVideoUri(context: Context, uri: Uri): Boolean {
        try {
            val type = context.contentResolver.getType(uri)
            if (type != null) {
                if (type.startsWith("video/", ignoreCase = true)) return true
                if (type.startsWith("image/", ignoreCase = true)) return false
            }

            val uriString = uri.toString().lowercase(Locale.ROOT)
            val videoExtensions = listOf(".mp4", ".mkv", ".webm", ".3gp", ".mov", ".avi", ".m4v", "video")
            if (videoExtensions.any { uriString.contains(it) }) return true

            // Try detecting with MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                retriever.release()
                if (hasVideo == "yes" || hasVideo != null) {
                    return true
                }
            } catch (_: Throwable) {
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed detecting media type for $uri", e)
        }
        return false
    }

    /**
     * Memory-safe bitmap loader with automatic downsampling to prevent OutOfMemory errors
     * on large camera photos (e.g. 48MP/108MP) and automatic EXIF orientation correction,
     * strictly preserving the exact aspect ratio.
     */
    suspend fun loadOptimizedBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2560
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First pass: inspect image dimensions without loading pixels into memory
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            var stream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream, null, boundsOptions)
            stream?.close()

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight

            if (origWidth <= 0 || origHeight <= 0) {
                Log.w(TAG, "Invalid dimensions for URI: $uri ($origWidth x $origHeight)")
                return@withContext null
            }

            // Calculate optimal power-of-2 inSampleSize
            var sampleSize = 1
            while ((origWidth / sampleSize) > maxDimension * 1.5 || (origHeight / sampleSize) > maxDimension * 1.5) {
                sampleSize *= 2
            }

            // Second pass: decode pixel data with calculated sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }

            stream = context.contentResolver.openInputStream(uri)
            var decodedBitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            stream?.close()

            if (decodedBitmap == null) return@withContext null

            // Check and apply EXIF orientation
            try {
                val exifStream = context.contentResolver.openInputStream(uri)
                if (exifStream != null) {
                    val exifInterface = ExifInterface(exifStream)
                    val orientation = exifInterface.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    exifStream.close()

                    val rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }

                    if (rotationDegrees != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        val rotated = Bitmap.createBitmap(
                            decodedBitmap, 0, 0,
                            decodedBitmap.width, decodedBitmap.height,
                            matrix, true
                        )
                        if (rotated != decodedBitmap) {
                            decodedBitmap.recycle()
                            decodedBitmap = rotated
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Could not apply EXIF orientation", e)
            }

            // Scale down to maxDimension preserving exact aspect ratio
            val currentMax = maxOf(decodedBitmap.width, decodedBitmap.height)
            if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax
                val targetW = (decodedBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (decodedBitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
                if (scaled != decodedBitmap) {
                    decodedBitmap.recycle()
                    decodedBitmap = scaled
                }
            }

            decodedBitmap
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError loading bitmap from $uri", oom)
            System.gc()
            null
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading bitmap from $uri", e)
            null
        }
    }

    /**
     * Extracts sampled keyframes from a video URI across its duration for real-time
     * on-device playback, frame inspection, and neural enhancement with true aspect ratio.
     */
    suspend fun extractVideoFrames(
        context: Context,
        uri: Uri,
        targetFrameCount: Int = 30,
        maxDimension: Int = 1080
    ): VideoMetadata? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 3000L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rawW = widthStr?.toIntOrNull() ?: 1280
            val rawH = heightStr?.toIntOrNull() ?: 720

            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotationDegrees = rotationStr?.toIntOrNull() ?: 0

            val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            val detectedFps = fpsStr?.toFloatOrNull()?.roundToInt()?.coerceIn(15, 60) ?: 30

            val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val hasAudio = hasAudioStr != null && (hasAudioStr.equals("yes", ignoreCase = true) || hasAudioStr == "1")

            val isRotated90or270 = rotationDegrees == 90 || rotationDegrees == 270
            val naturalW = if (isRotated90or270) rawH else rawW
            val naturalH = if (isRotated90or270) rawW else rawH

            // Calculate frame count based on actual video duration to preserve smooth playback and timing
            val durationSec = durationMs / 1000f
            val countToExtract = ((durationSec * detectedFps).toInt().coerceIn(16, 120))

            // Calculate target dimensions maintaining exact natural aspect ratio
            val maxEdge = maxOf(naturalW, naturalH)
            val scale = if (maxEdge > maxDimension) maxDimension.toFloat() / maxEdge else 1.0f
            val targetW = (naturalW * scale).toInt().coerceAtLeast(160)
            val targetH = (naturalH * scale).toInt().coerceAtLeast(160)

            val frames = mutableListOf<Bitmap>()
            val durationUs = (durationMs.coerceAtLeast(500L) * 1000L)
            val intervalUs = if (countToExtract > 1) {
                durationUs / (countToExtract - 1)
            } else 1000L

            for (i in 0 until countToExtract) {
                val timeUs = (i * intervalUs).coerceIn(0L, durationUs)
                var frameBitmap: Bitmap? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    try {
                        frameBitmap = retriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            targetW,
                            targetH
                        )
                    } catch (_: Throwable) {
                    }
                }

                if (frameBitmap == null) {
                    try {
                        frameBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (frameBitmap != null && (frameBitmap.width > targetW || frameBitmap.height > targetH)) {
                            val scaled = Bitmap.createScaledBitmap(frameBitmap, targetW, targetH, true)
                            if (scaled != frameBitmap) {
                                frameBitmap.recycle()
                                frameBitmap = scaled
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }

                if (frameBitmap != null) {
                    // Enforce correct display orientation if rotation was not auto-applied
                    val frameIsLandscape = frameBitmap.width > frameBitmap.height
                    val naturalIsPortrait = naturalH > naturalW
                    if (isRotated90or270 && frameIsLandscape && naturalIsPortrait) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        val rotated = Bitmap.createBitmap(
                            frameBitmap, 0, 0,
                            frameBitmap.width, frameBitmap.height,
                            matrix, true
                        )
                        if (rotated != frameBitmap) {
                            frameBitmap.recycle()
                            frameBitmap = rotated
                        }
                    }
                    frames.add(frameBitmap)
                }
            }

            if (frames.isEmpty()) {
                val singleFrame = retriever.getFrameAtTime()
                if (singleFrame != null) {
                    frames.add(singleFrame)
                }
            }

            if (frames.isEmpty()) {
                val placeholder = generateVideoFallbackFrame(targetW, targetH, 0)
                frames.add(placeholder)
            }

            VideoMetadata(
                durationMs = durationMs,
                width = frames.first().width,
                height = frames.first().height,
                frameCount = frames.size,
                fps = detectedFps,
                hasAudio = hasAudio,
                frames = frames
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error extracting video frames from $uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    private val thumbnailCache = androidx.collection.LruCache<String, Bitmap>(20)

    suspend fun extractVideoThumbnail(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = uri.toString()
        thumbnailCache.get(cacheKey)?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val rotationDegrees = rotationStr?.toIntOrNull() ?: 0

            var frame = retriever.getFrameAtTime(500000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (frame != null && rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, matrix, true)
                if (rotated != frame) {
                    frame.recycle()
                    frame = rotated
                }
            }

            if (frame != null) {
                thumbnailCache.put(cacheKey, frame)
            }
            frame
        } catch (e: Throwable) {
            Log.w(TAG, "Failed extracting video thumbnail for $uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun generateVideoFallbackFrame(w: Int, h: Int, index: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w.coerceAtLeast(480), h.coerceAtLeast(360), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.parseColor("#0D1117"))
        paint.color = Color.parseColor("#388BFD")
        paint.textSize = 28f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Video Stream Frame #$index", w / 2f, h / 2f, paint)
        return bitmap
    }
}
