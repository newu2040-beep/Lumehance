package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.domain.model.EnhancementConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class EnhancementOutput(
    val enhancedBitmap: Bitmap,
    val processingTimeMs: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val enhancedWidth: Int,
    val enhancedHeight: Int,
    val passes: List<String>
)

object ImageProcessor {

    /**
     * Executes the enhancement pipeline with real pixel-level operations
     */
    suspend fun enhance(
        sourceBitmap: Bitmap,
        config: EnhancementConfig,
        onProgress: (Float, String) -> Unit
    ): EnhancementOutput = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val origW = sourceBitmap.width
        val origH = sourceBitmap.height
        val passes = mutableListOf<String>()

        var currentBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Pass 1: Denoise / Grain Reduction
        if (config.denoiseStrength > 0.05f) {
            onProgress(0.20f, "Applying edge-preserving bilateral denoise...")
            currentBitmap = applyBilateralDenoise(currentBitmap, config.denoiseStrength)
            passes.add("Adaptive Denoise (${(config.denoiseStrength * 100).toInt()}%)")
        }

        // Pass 2: Color & Dynamic Range Recovery
        if (config.colorRestore > 0.05f || config.dynamicRangeBoost > 0.05f) {
            onProgress(0.40f, "Restoring dynamic range & chromatic balance...")
            currentBitmap = applyColorAndLighting(
                currentBitmap,
                config.colorRestore,
                config.dynamicRangeBoost
            )
            passes.add("Dynamic Range & Color Balance")
        }

        // Pass 3: Archival / Old Photo Restoration
        if (config.oldPhotoRestore) {
            onProgress(0.55f, "Removing dust, scratches & archival aging...")
            currentBitmap = applyOldPhotoRestoration(currentBitmap)
            passes.add("Archival Descratch & Color Restoration")
        }

        // Pass 4: Face & Portrait Enhancement (Opt-in)
        if (config.faceEnhance) {
            onProgress(0.70f, "Refining facial micro-texture & contour clarity...")
            currentBitmap = applyFaceRefine(currentBitmap)
            passes.add("Targeted Face & Portrait Refine")
        }

        // Pass 5: Super-Resolution Upscaling (2x or 4x)
        if (config.upscaleFactor > 1) {
            onProgress(0.85f, "Synthesizing high-frequency edge super-resolution (${config.upscaleFactor}x)...")
            currentBitmap = applySuperResolution(currentBitmap, config.upscaleFactor)
            passes.add("Super-Resolution ${config.upscaleFactor}x Detail Synthesis")
        }

        // Pass 6: Sharpen & Deblur
        if (config.sharpenStrength > 0.05f) {
            onProgress(0.95f, "Enhancing structural clarity and edge sharpness...")
            currentBitmap = applyUnsharpMask(currentBitmap, config.sharpenStrength)
            passes.add("Unsharp Mask Clarity (${(config.sharpenStrength * 100).toInt()}%)")
        }

        onProgress(1.0f, "Enhancement complete")
        val elapsed = System.currentTimeMillis() - startTime

        EnhancementOutput(
            enhancedBitmap = currentBitmap,
            processingTimeMs = elapsed,
            originalWidth = origW,
            originalHeight = origH,
            enhancedWidth = currentBitmap.width,
            enhancedHeight = currentBitmap.height,
            passes = passes
        )
    }

    /**
     * Bilateral edge-preserving spatial denoise algorithm
     */
    private fun applyBilateralDenoise(src: Bitmap, strength: Float): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        val outputPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val radius = (1 + (strength * 2.0f).roundToInt()).coerceIn(1, 3)
        val sigmaSpatial = radius.toFloat()
        val sigmaRange = 25.0f + (1.0f - strength) * 40.0f
        val twoSigmaRangeSq = 2f * sigmaRange * sigmaRange

        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                val centerPixel = pixels[yOffset + x]
                val centerR = (centerPixel shr 16) and 0xFF
                val centerG = (centerPixel shr 8) and 0xFF
                val centerB = centerPixel and 0xFF

                var sumR = 0.0
                var sumG = 0.0
                var sumB = 0.0
                var sumWeight = 0.0

                for (ky in -radius..radius) {
                    val py = (y + ky).coerceIn(0, height - 1)
                    val pOffset = py * width
                    for (kx in -radius..radius) {
                        val px = (x + kx).coerceIn(0, width - 1)
                        val neighborPixel = pixels[pOffset + px]

                        val nR = (neighborPixel shr 16) and 0xFF
                        val nG = (neighborPixel shr 8) and 0xFF
                        val nB = neighborPixel and 0xFF

                        val spatialDistSq = (kx * kx + ky * ky).toFloat()
                        val colorDistSq = ((centerR - nR) * (centerR - nR) +
                                (centerG - nG) * (centerG - nG) +
                                (centerB - nB) * (centerB - nB)).toFloat()

                        val weight = exp(-(spatialDistSq / (2f * sigmaSpatial * sigmaSpatial) + colorDistSq / twoSigmaRangeSq)).toDouble()
                        sumR += nR * weight
                        sumG += nG * weight
                        sumB += nB * weight
                        sumWeight += weight
                    }
                }

                val finalR = (sumR / sumWeight).roundToInt().coerceIn(0, 255)
                val finalG = (sumG / sumWeight).roundToInt().coerceIn(0, 255)
                val finalB = (sumB / sumWeight).roundToInt().coerceIn(0, 255)

                outputPixels[yOffset + x] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Color balance and dynamic range shadow/highlight lift
     */
    private fun applyColorAndLighting(src: Bitmap, colorStrength: Float, drBoost: Float): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val saturation = 1.0f + (colorStrength * 0.45f)
        val contrast = 1.0f + (drBoost * 0.35f)
        val brightness = drBoost * 12.0f

        val cm = ColorMatrix().apply {
            setSaturation(saturation)
        }

        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /**
     * Archival old photo restoration: median filtering for scratches + tone revitalization
     */
    private fun applyOldPhotoRestoration(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val rWindow = IntArray(9)
        val gWindow = IntArray(9)
        val bWindow = IntArray(9)

        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                var idx = 0
                for (ky in -1..1) {
                    val pOffset = (y + ky) * width
                    for (kx in -1..1) {
                        val p = pixels[pOffset + (x + kx)]
                        rWindow[idx] = (p shr 16) and 0xFF
                        gWindow[idx] = (p shr 8) and 0xFF
                        bWindow[idx] = p and 0xFF
                        idx++
                    }
                }
                rWindow.sort()
                gWindow.sort()
                bWindow.sort()

                // Median pixel suppresses dust and fine scratches
                val medR = rWindow[4]
                val medG = gWindow[4]
                val medB = bWindow[4]

                val orig = pixels[yOffset + x]
                val origR = (orig shr 16) and 0xFF
                val origG = (orig shr 8) and 0xFF
                val origB = orig and 0xFF

                // Only replace if significantly deviant (scratch or speckle)
                val diff = abs(origR - medR) + abs(origG - medG) + abs(origB - medB)
                val finalR = if (diff > 45) medR else ((origR * 0.6f) + (medR * 0.4f)).toInt()
                val finalG = if (diff > 45) medG else ((origG * 0.6f) + (medG * 0.4f)).toInt()
                val finalB = if (diff > 45) medB else ((origB * 0.6f) + (medB * 0.4f)).toInt()

                // Archival subtle warm saturation enrichment
                val boostedR = (finalR * 1.05f).roundToInt().coerceIn(0, 255)
                val boostedG = (finalG * 1.02f).roundToInt().coerceIn(0, 255)
                val boostedB = finalB.coerceIn(0, 255)

                output[yOffset + x] = (0xFF shl 24) or (boostedR shl 16) or (boostedG shl 8) or boostedB
            }
        }

        val res = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        res.setPixels(output, 0, width, 0, 0, width, height)
        return res
    }

    /**
     * Opt-in Face & Portrait Refine: skin smoothing while preserving eye/lip contour sharpness
     */
    private fun applyFaceRefine(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                val p = pixels[yOffset + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                // Check skin tone criteria in RGB color space
                val isSkinTone = (r > 95) && (g > 40) && (b > 20) &&
                        ((max(r, max(g, b)) - min(r, min(g, b))) > 15) &&
                        (abs(r - g) > 15) && (r > g) && (r > b)

                if (isSkinTone) {
                    // Soften skin micro-texture
                    var avgR = 0
                    var avgG = 0
                    var avgB = 0
                    var count = 0
                    for (ky in -1..1) {
                        val py = (y + ky).coerceIn(0, height - 1)
                        val pOff = py * width
                        for (kx in -1..1) {
                            val px = (x + kx).coerceIn(0, width - 1)
                            val np = pixels[pOff + px]
                            avgR += (np shr 16) and 0xFF
                            avgG += (np shr 8) and 0xFF
                            avgB += np and 0xFF
                            count++
                        }
                    }
                    val smoothR = (r * 0.45f + (avgR / count) * 0.55f).roundToInt().coerceIn(0, 255)
                    val smoothG = (g * 0.45f + (avgG / count) * 0.55f).roundToInt().coerceIn(0, 255)
                    val smoothB = (b * 0.45f + (avgB / count) * 0.55f).roundToInt().coerceIn(0, 255)
                    output[yOffset + x] = (0xFF shl 24) or (smoothR shl 16) or (smoothG shl 8) or smoothB
                } else {
                    // Non-skin (eyes, hair, clothing, contours) kept crisp
                    output[yOffset + x] = p
                }
            }
        }

        val res = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        res.setPixels(output, 0, width, 0, 0, width, height)
        return res
    }

    /**
     * High-Frequency Edge-Directed Super-Resolution (2x or 4x)
     */
    private fun applySuperResolution(src: Bitmap, factor: Int): Bitmap {
        val targetWidth = src.width * factor
        val targetHeight = src.height * factor

        // Initial high-fidelity bicubic baseline
        val scaled = Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)

        val pixels = IntArray(targetWidth * targetHeight)
        val output = IntArray(targetWidth * targetHeight)
        scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        // Directional edge kernel injection for super-resolution detail synthesis
        val synthStrength = if (factor == 4) 0.35f else 0.25f

        for (y in 1 until targetHeight - 1) {
            val yOffset = y * targetWidth
            for (x in 1 until targetWidth - 1) {
                val center = pixels[yOffset + x]
                val cR = (center shr 16) and 0xFF
                val cG = (center shr 8) and 0xFF
                val cB = center and 0xFF

                // Horizontal & vertical gradients
                val left = pixels[yOffset + (x - 1)]
                val right = pixels[yOffset + (x + 1)]
                val top = pixels[(y - 1) * targetWidth + x]
                val bottom = pixels[(y + 1) * targetWidth + x]

                val gradX = ((right shr 16) and 0xFF) - ((left shr 16) and 0xFF)
                val gradY = ((bottom shr 16) and 0xFF) - ((top shr 16) and 0xFF)
                val edgeMagnitude = abs(gradX) + abs(gradY)

                if (edgeMagnitude > 18) {
                    // Reconstruct crisp edge transition
                    val boostR = (cR + (gradX + gradY) * synthStrength * 0.15f).roundToInt().coerceIn(0, 255)
                    val boostG = (cG + (gradX + gradY) * synthStrength * 0.15f).roundToInt().coerceIn(0, 255)
                    val boostB = (cB + (gradX + gradY) * synthStrength * 0.15f).roundToInt().coerceIn(0, 255)
                    output[yOffset + x] = (0xFF shl 24) or (boostR shl 16) or (boostG shl 8) or boostB
                } else {
                    output[yOffset + x] = center
                }
            }
        }

        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        return result
    }

    /**
     * Unsharp mask sharpening kernel with edge gradient threshold
     */
    private fun applyUnsharpMask(src: Bitmap, strength: Float): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val amount = strength * 1.5f

        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                val center = pixels[yOffset + x]
                val cR = (center shr 16) and 0xFF
                val cG = (center shr 8) and 0xFF
                val cB = center and 0xFF

                // 4-neighborhood Laplacian high-pass
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[yOffset + (x - 1)]
                val right = pixels[yOffset + (x + 1)]

                val avgR = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) + ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) / 4
                val avgG = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) + ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) / 4
                val avgB = ((top and 0xFF) + (bottom and 0xFF) + (left and 0xFF) + (right and 0xFF)) / 4

                val diffR = cR - avgR
                val diffG = cG - avgG
                val diffB = cB - avgB

                val sharpR = (cR + diffR * amount).roundToInt().coerceIn(0, 255)
                val sharpG = (cG + diffG * amount).roundToInt().coerceIn(0, 255)
                val sharpB = (cB + diffB * amount).roundToInt().coerceIn(0, 255)

                output[yOffset + x] = (0xFF shl 24) or (sharpR shl 16) or (sharpG shl 8) or sharpB
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }
}
