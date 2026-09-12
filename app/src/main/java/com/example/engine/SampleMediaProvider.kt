package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.R
import com.example.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SampleMediaItem(
    val id: String,
    val title: String,
    val description: String,
    val type: MediaType,
    val drawableResId: Int?,
    val badge: String
)

object SampleMediaProvider {

    fun getSampleItems(): List<SampleMediaItem> = listOf(
        SampleMediaItem(
            id = "sample_vintage_portrait",
            title = "Archival Family Portrait",
            description = "1970s scanned print with soft focus, archival aging & warm grain",
            type = MediaType.PHOTO,
            drawableResId = R.drawable.sample_vintage,
            badge = "Vintage Restore"
        ),
        SampleMediaItem(
            id = "sample_night_neon",
            title = "Night City Low-Light",
            description = "Atmospheric twilight street photo with sensor noise & motion blur",
            type = MediaType.PHOTO,
            drawableResId = R.drawable.sample_night_city,
            badge = "Low-Light Clean"
        ),
        SampleMediaItem(
            id = "sample_video_clip",
            title = "Cinematic Street Sequence",
            description = "Multi-frame video sequence with camera motion & ambient lighting",
            type = MediaType.VIDEO,
            drawableResId = R.drawable.sample_night_city,
            badge = "Video 4K HDR"
        )
    )

    suspend fun loadBitmap(context: Context, resId: Int): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(context.resources, resId, options)
    }

    suspend fun generateVideoFrames(context: Context, baseResId: Int, frameCount: Int = 12): List<Bitmap> = withContext(Dispatchers.Default) {
        val base = loadBitmap(context, baseResId)
        val w = (base.width / 2).coerceAtLeast(360)
        val h = (base.height / 2).coerceAtLeast(270)
        val scaledBase = Bitmap.createScaledBitmap(base, w, h, true)

        val frames = mutableListOf<Bitmap>()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until frameCount) {
            val frame = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(frame)
            // Subtle cinematic pan & light sweep
            val panX = (i * 3.5f) - (frameCount * 1.5f)
            canvas.drawBitmap(scaledBase, panX, 0f, paint)

            // Dynamic camera timestamp & frame indicator
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 140
                textSize = 22f
            }
            canvas.drawText("REC 00:00:${String.format("%02d", i * 3)} [24fps]", 24f, 40f, textPaint)

            frames.add(frame)
        }
        frames
    }
}
