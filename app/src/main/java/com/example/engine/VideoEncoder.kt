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
        val (targetW, targetH) = resolution.getTargetDimensions(sampleFrame.width, sampleFrame.height)
        // Codecs strictly require even width and height
        val encWidth = if (targetW % 2 != 0) targetW + 1 else targetW
        val encHeight = if (targetH % 2 != 0) targetH + 1 else targetH

        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.parentFile?.mkdirs()

        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null
        var extractor: MediaExtractor? = null

        try {
            val selectedMime = if (useHevc && isCodecSupported(MIME_TYPE_HEVC)) MIME_TYPE_HEVC else MIME_TYPE_AVC
            val targetBitrate = (bitrateMbps * 1_000_000).coerceIn(4_000_000, 60_000_000)

            val format = MediaFormat.createVideoFormat(selectedMime, encWidth, encHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps.coerceIn(15, 120))
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            encoder = MediaCodec.createEncoderByType(selectedMime)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Inspect source video for audio track to preserve
            var audioTrackIdxInSource = -1
            var audioMuxerTrackIdx = -1
            if (sourceVideoUri != null) {
                try {
                    extractor = MediaExtractor()
                    extractor.setDataSource(context, sourceVideoUri, null)
                    for (i in 0 until extractor.trackCount) {
                        val trackFormat = extractor.getTrackFormat(i)
                        val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            audioTrackIdxInSource = i
                            extractor.selectTrack(i)
                            audioMuxerTrackIdx = muxer.addTrack(trackFormat)
                            Log.d(TAG, "Preserving original audio track ($mime) from source video")
                            break
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not extract audio track from source video: ${e.message}")
                    extractor?.release()
                    extractor = null
                    audioTrackIdxInSource = -1
                    audioMuxerTrackIdx = -1
                }
            }

            var videoMuxerTrackIdx = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val srcPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            val totalFrames = frames.size
            val frameDurationUs = 1_000_000L / fps.coerceAtLeast(1)

            // Feed video frames to input surface
            for (i in 0 until totalFrames) {
                val frame = frames[i]
                val presentationTimeUs = i * frameDurationUs

                // Render frame onto surface canvas
                try {
                    val canvas: Canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        inputSurface.lockHardwareCanvas()
                    } else {
                        inputSurface.lockCanvas(null)
                    }

                    canvas.drawColor(Color.BLACK)
                    val srcRect = Rect(0, 0, frame.width, frame.height)
                    val dstRect = Rect(0, 0, encWidth, encHeight)
                    canvas.drawBitmap(frame, srcRect, dstRect, srcPaint)
                    inputSurface.unlockCanvasAndPost(canvas)
                } catch (e: Throwable) {
                    Log.w(TAG, "Canvas drawing error on frame $i", e)
                }

                // Drain encoder output buffers
                while (true) {
                    val outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed after muxer already started")
                        }
                        val newFormat = encoder.outputFormat
                        videoMuxerTrackIdx = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outputBufferId >= 0) {
                        val encodedData = encoder.getOutputBuffer(outputBufferId)
                        if (encodedData != null && muxerStarted) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size != 0) {
                                bufferInfo.presentationTimeUs = presentationTimeUs
                                muxer.writeSampleData(videoMuxerTrackIdx, encodedData, bufferInfo)
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

                onProgress((i + 1).toFloat() / totalFrames * 0.85f)
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
                        videoMuxerTrackIdx = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outputBufferId >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferId)
                    if (encodedData != null && muxerStarted && bufferInfo.size != 0) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            muxer.writeSampleData(videoMuxerTrackIdx, encodedData, bufferInfo)
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

            // Mux preserved audio track if present
            if (muxerStarted && audioMuxerTrackIdx >= 0 && extractor != null) {
                try {
                    val maxAudioBufferSize = 256 * 1024
                    val audioBuffer = ByteBuffer.allocateDirect(maxAudioBufferSize)
                    val audioBufferInfo = MediaCodec.BufferInfo()
                    val totalDurationUs = totalFrames * frameDurationUs

                    while (true) {
                        val sampleSize = extractor.readSampleData(audioBuffer, 0)
                        if (sampleSize < 0) break
                        val sampleTimeUs = extractor.sampleTime
                        if (sampleTimeUs > totalDurationUs + 500_000L) {
                            // Clip audio to video length
                            break
                        }

                        audioBufferInfo.offset = 0
                        audioBufferInfo.size = sampleSize
                        audioBufferInfo.presentationTimeUs = sampleTimeUs
                        audioBufferInfo.flags = extractor.sampleFlags

                        muxer.writeSampleData(audioMuxerTrackIdx, audioBuffer, audioBufferInfo)
                        extractor.advance()
                    }
                    Log.d(TAG, "Successfully muxed source audio track into enhanced MP4 master")
                } catch (e: Throwable) {
                    Log.w(TAG, "Error while muxing audio track: ${e.message}")
                }
            }

            onProgress(1.0f)
            Log.i(TAG, "Successfully encoded ${frames.size} frames to ${outputFile.absolutePath} (${outputFile.length() / 1024} KB)")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Video encoding failed", e)
            false
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Throwable) {
            }
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Throwable) {
            }
            try {
                extractor?.release()
            } catch (_: Throwable) {
            }
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
