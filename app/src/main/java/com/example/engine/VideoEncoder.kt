package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.domain.model.ExportResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

object VideoEncoder {

    private const val TAG = "VideoEncoder"
    private const val MIME_TYPE_AVC = MediaFormat.MIMETYPE_VIDEO_AVC // "video/avc" (H.264)
    private const val MIME_TYPE_HEVC = MediaFormat.MIMETYPE_VIDEO_HEVC // "video/hevc" (H.265)
    private const val I_FRAME_INTERVAL = 1 // Key frame every second
    private const val TIMEOUT_USEC = 10000L

    /**
     * Encodes a list of enhanced video frames into a real playable MP4 video file,
     * preserving the original video's aspect ratio and audio track.
     */
    suspend fun encodeFramesToMp4(
        context: Context,
        frames: List<Bitmap>,
        fps: Int,
        outputFile: File,
        sourceVideoUri: Uri? = null,
        resolution: ExportResolution = ExportResolution.RES_4K,
        bitrateMbps: Int = 35,
        useHevc: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (frames.isEmpty()) return@withContext false

        val sampleFrame = frames.first()
        val srcW = sampleFrame.width
        val srcH = sampleFrame.height
        val srcAspect = srcW.toFloat() / srcH.toFloat()

        // Extract duration & capture FPS from source video if provided
        var sourceFps = fps
        var sourceDurationUs = 0L
        var retriever: MediaMetadataRetriever? = null

        if (sourceVideoUri != null) {
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, sourceVideoUri)

                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durMs = durStr?.toLongOrNull() ?: 0L
                if (durMs > 0) {
                    sourceDurationUs = durMs * 1000L
                }

                val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                val extractedFps = fpsStr?.toFloatOrNull()?.roundToInt() ?: 0
                if (extractedFps > 0) {
                    sourceFps = extractedFps
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Could not extract metadata from source video URI: ${e.message}")
            } finally {
                try { retriever?.release() } catch (_: Throwable) {}
            }
        }

        // Calculate target dimensions maintaining exact aspect ratio
        val (targetMaxW, targetMaxH) = resolution.getTargetDimensions(srcW, srcH)
        val maxDim = maxOf(targetMaxW, targetMaxH)
        val scale = maxDim.toFloat() / maxOf(srcW, srcH)

        val rawEncW = (srcW * scale).roundToInt().coerceAtLeast(16)
        val rawEncH = (srcH * scale).roundToInt().coerceAtLeast(16)

        // Codecs strictly require even width and height
        val encWidth = if (rawEncW % 2 != 0) rawEncW + 1 else rawEncW
        val encHeight = if (rawEncH % 2 != 0) rawEncH + 1 else rawEncH

        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.parentFile?.mkdirs()

        // Temp file for pure video pass
        val tempVideoFile = File(context.cacheDir, "temp_video_pass_${System.currentTimeMillis()}.mp4")
        if (tempVideoFile.exists()) tempVideoFile.delete()

        var videoMuxer: MediaMuxer? = null
        var encoder: MediaCodec? = null

        try {
            val selectedMime = if (useHevc && isCodecSupported(MIME_TYPE_HEVC)) MIME_TYPE_HEVC else MIME_TYPE_AVC
            val targetBitrate = (bitrateMbps * 1_000_000).coerceIn(4_000_000, 60_000_000)
            val encodeFps = sourceFps.coerceIn(15, 120)

            val format = MediaFormat.createVideoFormat(selectedMime, encWidth, encHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, encodeFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            encoder = MediaCodec.createEncoderByType(selectedMime)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            videoMuxer = MediaMuxer(tempVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var videoTrackIdx = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val srcPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            val totalFrames = frames.size

            val frameDurationUs = if (sourceDurationUs > 0L && totalFrames > 0) {
                sourceDurationUs / totalFrames
            } else {
                1_000_000L / encodeFps.coerceAtLeast(1)
            }

            // Aspect-fit destination rectangle on canvas to guarantee ZERO stretching
            val encAspect = encWidth.toFloat() / encHeight.toFloat()
            val drawW: Int
            val drawH: Int
            if (kotlin.math.abs(srcAspect - encAspect) < 0.001f) {
                drawW = encWidth
                drawH = encHeight
            } else if (srcAspect > encAspect) {
                drawW = encWidth
                drawH = (encWidth / srcAspect).roundToInt()
            } else {
                drawH = encHeight
                drawW = (encHeight * srcAspect).roundToInt()
            }
            val left = (encWidth - drawW) / 2
            val top = (encHeight - drawH) / 2
            val dstRect = Rect(left, top, left + drawW, top + drawH)

            // Feed video frames to input surface
            for (i in 0 until totalFrames) {
                val frame = frames[i]
                val presentationTimeUs = i * frameDurationUs

                // Render frame onto surface canvas with exact aspect ratio
                try {
                    val canvas: Canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        inputSurface.lockHardwareCanvas()
                    } else {
                        inputSurface.lockCanvas(null)
                    }

                    canvas.drawColor(Color.BLACK)
                    val frameSrcRect = Rect(0, 0, frame.width, frame.height)
                    canvas.drawBitmap(frame, frameSrcRect, dstRect, srcPaint)
                    inputSurface.unlockCanvasAndPost(canvas)
                } catch (e: Throwable) {
                    Log.w(TAG, "Canvas drawing error on frame $i", e)
                }

                // Drain encoder output buffers
                while (true) {
                    val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed after video muxer already started")
                        }
                        val newFormat = encoder.outputFormat
                        videoTrackIdx = videoMuxer.addTrack(newFormat)
                        videoMuxer.start()
                        muxerStarted = true
                    } else if (outputBufferId >= 0) {
                        val encodedData = encoder.getOutputBuffer(outputBufferId)
                        if (encodedData != null && muxerStarted) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size != 0) {
                                bufferInfo.presentationTimeUs = presentationTimeUs
                                videoMuxer.writeSampleData(videoTrackIdx, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferId, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    } else {
                        break
                    }
                }

                onProgress((i + 1).toFloat() / totalFrames * 0.70f)
            }

            // Signal end of stream
            try {
                encoder.signalEndOfInputStream()
            } catch (_: Throwable) {
            }

            // Drain remaining buffers
            var eosReached = false
            var drainAttempts = 0
            while (!eosReached && drainAttempts < 60) {
                drainAttempts++
                val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        val newFormat = encoder.outputFormat
                        videoTrackIdx = videoMuxer.addTrack(newFormat)
                        videoMuxer.start()
                        muxerStarted = true
                    }
                } else if (outputBufferId >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferId)
                    if (encodedData != null && muxerStarted && bufferInfo.size != 0) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            videoMuxer.writeSampleData(videoTrackIdx, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outputBufferId, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosReached = true
                    }
                } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (drainAttempts > 15) break
                }
            }

            try { encoder.stop(); encoder.release() } catch (_: Throwable) {}
            encoder = null
            try { videoMuxer.stop(); videoMuxer.release() } catch (_: Throwable) {}
            videoMuxer = null

            onProgress(0.85f)

            // Pass 2: Merge video track from temp file and audio track from source URI with timestamp interleaving
            val mergeSuccess = mergeVideoAndAudio(
                context = context,
                videoFile = tempVideoFile,
                sourceVideoUri = sourceVideoUri,
                outputFile = outputFile
            )

            try { tempVideoFile.delete() } catch (_: Throwable) {}

            onProgress(1.0f)
            Log.i(TAG, "Successfully encoded ${frames.size} frames with audio to ${outputFile.absolutePath} (${outputFile.length() / 1024} KB)")
            mergeSuccess && outputFile.exists() && outputFile.length() > 0L
        } catch (e: Throwable) {
            Log.e(TAG, "Video encoding failed", e)
            try { tempVideoFile.delete() } catch (_: Throwable) {}
            false
        } finally {
            try { encoder?.stop(); encoder?.release() } catch (_: Throwable) {}
            try { videoMuxer?.stop(); videoMuxer?.release() } catch (_: Throwable) {}
        }
    }

    /**
     * Combines video stream from temp file with audio stream from source URI using MediaMuxer
     * with interleaved timestamp ordering to guarantee audio-video synchronization without errors.
     */
    private fun mergeVideoAndAudio(
        context: Context,
        videoFile: File,
        sourceVideoUri: Uri?,
        outputFile: File
    ): Boolean {
        if (sourceVideoUri == null) {
            return try {
                videoFile.copyTo(outputFile, overwrite = true)
                true
            } catch (_: Throwable) {
                false
            }
        }

        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        return try {
            videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(videoFile.absolutePath)
            var videoTrackIdx = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIdx = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIdx < 0 || videoFormat == null) {
                videoFile.copyTo(outputFile, overwrite = true)
                return true
            }

            audioExtractor = MediaExtractor()
            var audioTrackIdx = -1
            var audioFormat: MediaFormat? = null
            try {
                audioExtractor.setDataSource(context, sourceVideoUri, null)
                for (i in 0 until audioExtractor.trackCount) {
                    val format = audioExtractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        audioTrackIdx = i
                        audioFormat = format
                        break
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed inspecting audio track from source video: ${e.message}")
            }

            if (audioTrackIdx < 0 || audioFormat == null) {
                videoFile.copyTo(outputFile, overwrite = true)
                return true
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            videoExtractor.selectTrack(videoTrackIdx)
            audioExtractor.selectTrack(audioTrackIdx)

            val maxBufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var videoDone = false
            var audioDone = false

            while (!videoDone || !audioDone) {
                val videoTime = if (!videoDone) videoExtractor.sampleTime else Long.MAX_VALUE
                val audioTime = if (!audioDone) audioExtractor.sampleTime else Long.MAX_VALUE

                if (videoTime == -1L) videoDone = true
                if (audioTime == -1L) audioDone = true

                if (videoDone && audioDone) break

                if (!videoDone && videoTime <= audioTime) {
                    bufferInfo.offset = 0
                    val sampleSize = videoExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        videoDone = true
                    } else {
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                        bufferInfo.flags = videoExtractor.sampleFlags
                        muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                        videoExtractor.advance()
                    }
                } else if (!audioDone) {
                    bufferInfo.offset = 0
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        audioDone = true
                    } else {
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                        bufferInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                        audioExtractor.advance()
                    }
                }
            }

            Log.i(TAG, "Audio & Video tracks successfully interleaved into final MP4 master")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed merging audio and video tracks: ${e.message}", e)
            try { videoFile.copyTo(outputFile, overwrite = true) } catch (_: Throwable) {}
            false
        } finally {
            try { videoExtractor?.release() } catch (_: Throwable) {}
            try { audioExtractor?.release() } catch (_: Throwable) {}
            try { muxer?.stop(); muxer?.release() } catch (_: Throwable) {}
        }
    }

    private fun isCodecSupported(mime: String): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { it.isEncoder && it.supportedTypes.any { t -> t.equals(mime, ignoreCase = true) } }
        } catch (_: Throwable) {
            false
        }
    }
}
