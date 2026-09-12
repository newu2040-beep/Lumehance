package com.example.engine

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.abs

object NativeImageProcessor {

    private const val TAG = "NativeImageProcessor"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("native_enhancer")
            isNativeLoaded = true
            Log.i(TAG, "Native C++ enhancer library loaded successfully")
        } catch (t: Throwable) {
            isNativeLoaded = false
            Log.i(TAG, "Native C++ library not present in current target ABI; utilizing hyper-optimized parallel Kotlin engine.")
        }
    }

    fun isNativeEngineAvailable(): Boolean = isNativeLoaded

    /**
     * Parallel bilateral denoise with native C++ acceleration or parallel coroutine worker chunks.
     */
    suspend fun bilateralDenoise(
        source: Bitmap,
        strength: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        if (isNativeLoaded) {
            try {
                nativeBilateralDenoise(pixels, output, width, height, strength)
                val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                result.setPixels(output, 0, width, 0, 0, width, height)
                return@withContext result
            } catch (e: Throwable) {
                Log.w(TAG, "Native execution error, falling back to parallel coroutines", e)
            }
        }

        // Hyper-optimized multithreaded coroutine chunking
        val threshold = (30 + (1.0f - strength) * 45).toInt()
        val blendFactor = strength.coerceIn(0.2f, 0.85f)
        val numCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
        val chunkHeight = (height - 2) / numCores

        coroutineScope {
            val jobs = (0 until numCores).map { coreIdx ->
                async {
                    val startY = 1 + coreIdx * chunkHeight
                    val endY = if (coreIdx == numCores - 1) height - 1 else startY + chunkHeight

                    for (y in startY until endY) {
                        val yOffset = y * width
                        for (x in 1 until width - 1) {
                            val center = pixels[yOffset + x]
                            val cR = (center shr 16) and 0xFF
                            val cG = (center shr 8) and 0xFF
                            val cB = center and 0xFF

                            var sumR = cR * 4
                            var sumG = cG * 4
                            var sumB = cB * 4
                            var count = 4

                            val top = pixels[(y - 1) * width + x]
                            val bottom = pixels[(y + 1) * width + x]
                            val left = pixels[yOffset + (x - 1)]
                            val right = pixels[yOffset + (x + 1)]

                            val neighbors = intArrayOf(top, bottom, left, right)
                            for (n in neighbors) {
                                val nR = (n shr 16) and 0xFF
                                val nG = (n shr 8) and 0xFF
                                val nB = n and 0xFF
                                val diff = abs(cR - nR) + abs(cG - nG) + abs(cB - nB)
                                if (diff < threshold * 3) {
                                    val weight = (threshold * 3 - diff).coerceAtLeast(1)
                                    sumR += nR * weight
                                    sumG += nG * weight
                                    sumB += nB * weight
                                    count += weight
                                }
                            }

                            val avgR = (sumR / count).coerceIn(0, 255)
                            val avgG = (sumG / count).coerceIn(0, 255)
                            val avgB = (sumB / count).coerceIn(0, 255)

                            val finalR = (cR * (1f - blendFactor) + avgR * blendFactor).toInt().coerceIn(0, 255)
                            val finalG = (cG * (1f - blendFactor) + avgG * blendFactor).toInt().coerceIn(0, 255)
                            val finalB = (cB * (1f - blendFactor) + avgB * blendFactor).toInt().coerceIn(0, 255)

                            output[yOffset + x] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                        }
                    }
                }
            }
            jobs.awaitAll()
        }

        // Copy borders
        for (x in 0 until width) {
            output[x] = pixels[x]
            output[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            output[y * width] = pixels[y * width]
            output[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        result
    }

    /**
     * Parallel unsharp mask clarity filter.
     */
    suspend fun unsharpMask(
        source: Bitmap,
        strength: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        if (isNativeLoaded) {
            try {
                nativeUnsharpMask(pixels, output, width, height, strength)
                val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                result.setPixels(output, 0, width, 0, 0, width, height)
                return@withContext result
            } catch (e: Throwable) {
                Log.w(TAG, "Native execution error, falling back to parallel coroutines", e)
            }
        }

        val amount = strength * 1.35f
        val numCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
        val chunkHeight = (height - 2) / numCores

        coroutineScope {
            val jobs = (0 until numCores).map { coreIdx ->
                async {
                    val startY = 1 + coreIdx * chunkHeight
                    val endY = if (coreIdx == numCores - 1) height - 1 else startY + chunkHeight

                    for (y in startY until endY) {
                        val yOffset = y * width
                        for (x in 1 until width - 1) {
                            val center = pixels[yOffset + x]
                            val cR = (center shr 16) and 0xFF
                            val cG = (center shr 8) and 0xFF
                            val cB = center and 0xFF

                            val top = pixels[(y - 1) * width + x]
                            val bottom = pixels[(y + 1) * width + x]
                            val left = pixels[yOffset + (x - 1)]
                            val right = pixels[yOffset + (x + 1)]

                            val avgR = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) + ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) shr 2
                            val avgG = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) + ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) shr 2
                            val avgB = ((top and 0xFF) + (bottom and 0xFF) + (left and 0xFF) + (right and 0xFF)) shr 2

                            val diffR = cR - avgR
                            val diffG = cG - avgG
                            val diffB = cB - avgB

                            val sharpR = (cR + diffR * amount).toInt().coerceIn(0, 255)
                            val sharpG = (cG + diffG * amount).toInt().coerceIn(0, 255)
                            val sharpB = (cB + diffB * amount).toInt().coerceIn(0, 255)

                            output[yOffset + x] = (0xFF shl 24) or (sharpR shl 16) or (sharpG shl 8) or sharpB
                        }
                    }
                }
            }
            jobs.awaitAll()
        }

        // Copy borders
        for (x in 0 until width) {
            output[x] = pixels[x]
            output[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            output[y * width] = pixels[y * width]
            output[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        result
    }

    private external fun nativeBilateralDenoise(
        pixels: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
        strength: Float
    )

    private external fun nativeUnsharpMask(
        pixels: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
        strength: Float
    )
}
