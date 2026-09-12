package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.domain.model.ExportResolution
import com.example.engine.VideoEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

object MediaGallerySaver {

    private const val TAG = "MediaGallerySaver"
    const val CHANNEL_ID = "lumenhance_auto_save"
    private const val CHANNEL_NAME = "Lumenhance Auto-Save & Exports"
    private const val NOTIFICATION_ID_PHOTO = 1001
    private const val NOTIFICATION_ID_VIDEO = 1002

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when enhanced photos or videos are automatically saved to your Gallery"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    suspend fun savePhotoToGallery(
        context: Context,
        bitmap: Bitmap,
        title: String = "Enhanced_Photo",
        format: String = "JPEG",
        quality: Int = 96,
        resolution: ExportResolution = ExportResolution.RES_4K,
        notifyUser: Boolean = true
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            initNotificationChannel(context)
            val timestamp = System.currentTimeMillis()
            val cleanTitle = title.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val isPng = format.contains("PNG", ignoreCase = true)
            val isWebp = format.contains("WEBP", ignoreCase = true)
            val extension = if (isPng) "png" else if (isWebp) "webp" else "jpg"
            val mimeType = if (isPng) "image/png" else if (isWebp) "image/webp" else "image/jpeg"
            val filename = "Lumenhance_${cleanTitle}_${timestamp}.$extension"

            val (targetW, targetH) = resolution.getTargetDimensions(bitmap.width, bitmap.height)
            val exportBitmap = if (targetW != bitmap.width || targetH != bitmap.height) {
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else bitmap

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.DATE_ADDED, timestamp / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Lumenhance")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
            if (outputStream != null) {
                if (isPng) {
                    exportBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                } else if (isWebp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    exportBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality.coerceIn(1, 100), outputStream)
                } else {
                    exportBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), outputStream)
                }
                outputStream.flush()
                outputStream.close()
            } else {
                return@withContext Result.failure(Exception("Unable to open output stream for gallery save"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            // Trigger MediaScanner for instant gallery indexing
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageUri.toString()),
                arrayOf(mimeType),
                null
            )

            if (notifyUser) {
                postNotification(
                    context = context,
                    id = NOTIFICATION_ID_PHOTO,
                    title = "Photo Saved to Gallery",
                    message = "Enhanced ${bitmap.width}x${bitmap.height} image saved to Pictures/Lumenhance",
                    uri = imageUri,
                    mimeType = mimeType
                )
            }

            Result.success(imageUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving photo to gallery", e)
            Result.failure(e)
        }
    }

    suspend fun saveVideoFileToGallery(
        context: Context,
        videoFile: File,
        title: String = "Enhanced_Video",
        notifyUser: Boolean = true
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!videoFile.exists() || videoFile.length() == 0L) {
                return@withContext Result.failure(Exception("Video file is empty or missing"))
            }

            initNotificationChannel(context)
            val timestamp = System.currentTimeMillis()
            val cleanTitle = title.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val filename = "Lumenhance_${cleanTitle}_${timestamp}.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, timestamp / 1000)
                put(MediaStore.Video.Media.DATE_TAKEN, timestamp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Lumenhance")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore video entry"))

            val outputStream = resolver.openOutputStream(videoUri)
            val inputStream = FileInputStream(videoFile)
            if (outputStream != null) {
                inputStream.copyTo(outputStream)
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } else {
                inputStream.close()
                return@withContext Result.failure(Exception("Could not open gallery video output stream"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(videoUri, contentValues, null, null)
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(videoUri.toString()),
                arrayOf("video/mp4"),
                null
            )

            if (notifyUser) {
                postNotification(
                    context = context,
                    id = NOTIFICATION_ID_VIDEO,
                    title = "Video Saved to Gallery",
                    message = "Enhanced Master MP4 video saved to Movies/Lumenhance (Audio Preserved)",
                    uri = videoUri,
                    mimeType = "video/mp4"
                )
            }

            Result.success(videoUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving video file to gallery", e)
            Result.failure(e)
        }
    }

    suspend fun saveVideoFramesToGallery(
        context: Context,
        frames: List<Bitmap>,
        title: String = "Enhanced_Video",
        fps: Int = 60,
        sourceVideoUri: Uri? = null,
        resolution: ExportResolution = ExportResolution.RES_4K,
        notifyUser: Boolean = true
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (frames.isEmpty()) {
                return@withContext Result.failure(Exception("No video frames to encode"))
            }

            val tempFile = File(context.cacheDir, "temp_render_${System.currentTimeMillis()}.mp4")
            val success = VideoEncoder.encodeFramesToMp4(
                context = context,
                frames = frames,
                fps = fps,
                outputFile = tempFile,
                sourceVideoUri = sourceVideoUri,
                resolution = resolution
            )

            if (success && tempFile.exists() && tempFile.length() > 0L) {
                val result = saveVideoFileToGallery(context, tempFile, title, notifyUser)
                tempFile.delete()
                result
            } else {
                tempFile.delete()
                Result.failure(Exception("Video encoding pipeline returned an empty stream"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed encoding and saving video frames", e)
            Result.failure(e)
        }
    }

    private fun postNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        uri: Uri,
        mimeType: String
    ) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                id,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_circle_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(id, notification)
        } catch (_: Exception) {
        }
    }
}
