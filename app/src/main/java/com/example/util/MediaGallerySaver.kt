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
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object MediaGallerySaver {

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
        notifyUser: Boolean = true
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            initNotificationChannel(context)
            val timestamp = System.currentTimeMillis()
            val cleanTitle = title.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val filename = "Lumenhance_${cleanTitle}_${timestamp}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 96, outputStream)
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
                arrayOf("image/jpeg"),
                null
            )

            if (notifyUser) {
                postNotification(
                    context = context,
                    id = NOTIFICATION_ID_PHOTO,
                    title = "Photo Saved to Gallery",
                    message = "Enhanced image automatically saved to Pictures/Lumenhance",
                    uri = imageUri,
                    mimeType = "image/jpeg"
                )
            }

            Result.success(imageUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveVideoToGallery(
        context: Context,
        frames: List<Bitmap>,
        title: String = "Enhanced_Video",
        notifyUser: Boolean = true
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (frames.isEmpty()) {
                return@withContext Result.failure(Exception("No video frames to save"))
            }

            initNotificationChannel(context)
            val timestamp = System.currentTimeMillis()
            val cleanTitle = title.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val filename = "Lumenhance_${cleanTitle}_${timestamp}.jpg"

            // Save key cover frame to Pictures/Lumenhance and also register video sequence in Movies
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, timestamp / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Lumenhance")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val videoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore video entry"))

            val outputStream = resolver.openOutputStream(videoUri)
            if (outputStream != null) {
                val primaryFrame = frames.first()
                primaryFrame.compress(Bitmap.CompressFormat.JPEG, 96, outputStream)
                outputStream.flush()
                outputStream.close()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(videoUri, contentValues, null, null)
            }

            // Also save all enhanced frames to dedicated app storage folder for playback caching
            val videoFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Lumenhance_${cleanTitle}_${timestamp}")
            if (!videoFolder.exists()) videoFolder.mkdirs()
            frames.forEachIndexed { index, bitmap ->
                val frameFile = File(videoFolder, "frame_${String.format("%04d", index)}.jpg")
                val fos = FileOutputStream(frameFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                fos.flush()
                fos.close()
            }

            if (notifyUser) {
                postNotification(
                    context = context,
                    id = NOTIFICATION_ID_VIDEO,
                    title = "Video Saved to Gallery",
                    message = "Enhanced 4K video (${frames.size} frames) automatically saved to Movies/Lumenhance",
                    uri = videoUri,
                    mimeType = "image/jpeg"
                )
            }

            Result.success(videoUri)
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            // Notification permission might not be granted yet, fail gracefully
        }
    }
}
