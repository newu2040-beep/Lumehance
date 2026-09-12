#include <jni.h>
#include <android/bitmap.h>
#include <cmath>
#include <algorithm>
#include <vector>
#include <cstring>

#define CLAMP(v, min_v, max_v) ((v) < (min_v) ? (min_v) : ((v) > (max_v) ? (max_v) : (v)))

extern "C" {

/**
 * High-performance C++ bilateral denoise filter operating directly on RGBA_8888 pixel buffers.
 * Uses 4-connected difference thresholding with zero dynamic allocation in hot loops.
 */
JNIEXPORT void JNICALL
Java_com_example_engine_NativeImageProcessor_nativeBilateralDenoise(
    JNIEnv* env,
    jclass clazz,
    jintArray pixelsArray,
    jintArray outputArray,
    jint width,
    jint height,
    jfloat strength
) {
    jint* src = env->GetIntArrayElements(pixelsArray, NULL);
    jint* dst = env->GetIntArrayElements(outputArray, NULL);

    if (!src || !dst) {
        if (src) env->ReleaseIntArrayElements(pixelsArray, src, JNI_ABORT);
        if (dst) env->ReleaseIntArrayElements(outputArray, dst, 0);
        return;
    }

    int threshold = static_cast<int>(30.0f + (1.0f - strength) * 45.0f);
    float blendFactor = CLAMP(strength, 0.2f, 0.85f);

    #pragma omp parallel for schedule(static)
    for (int y = 1; y < height - 1; ++y) {
        int yOffset = y * width;
        for (int x = 1; x < width - 1; ++x) {
            int center = src[yOffset + x];
            int cR = (center >> 16) & 0xFF;
            int cG = (center >> 8) & 0xFF;
            int cB = center & 0xFF;

            int sumR = cR * 4;
            int sumG = cG * 4;
            int sumB = cB * 4;
            int count = 4;

            int neighbors[4] = {
                src[(y - 1) * width + x],
                src[(y + 1) * width + x],
                src[yOffset + (x - 1)],
                src[yOffset + (x + 1)]
            };

            for (int i = 0; i < 4; ++i) {
                int n = neighbors[i];
                int nR = (n >> 16) & 0xFF;
                int nG = (n >> 8) & 0xFF;
                int nB = n & 0xFF;

                int diff = std::abs(cR - nR) + std::abs(cG - nG) + std::abs(cB - nB);
                if (diff < threshold * 3) {
                    int weight = (threshold * 3 - diff);
                    if (weight < 1) weight = 1;
                    sumR += nR * weight;
                    sumG += nG * weight;
                    sumB += nB * weight;
                    count += weight;
                }
            }

            int avgR = sumR / count;
            int avgG = sumG / count;
            int avgB = sumB / count;

            int finalR = static_cast<int>(cR * (1.0f - blendFactor) + avgR * blendFactor);
            int finalG = static_cast<int>(cG * (1.0f - blendFactor) + avgG * blendFactor);
            int finalB = static_cast<int>(cB * (1.0f - blendFactor) + avgB * blendFactor);

            dst[yOffset + x] = (0xFF << 24) |
                               (CLAMP(finalR, 0, 255) << 16) |
                               (CLAMP(finalG, 0, 255) << 8) |
                               CLAMP(finalB, 0, 255);
        }
    }

    // Copy top and bottom row borders
    std::memcpy(dst, src, width * sizeof(jint));
    std::memcpy(dst + (height - 1) * width, src + (height - 1) * width, width * sizeof(jint));

    // Copy side borders
    for (int y = 0; y < height; ++y) {
        dst[y * width] = src[y * width];
        dst[y * width + (width - 1)] = src[y * width + (width - 1)];
    }

    env->ReleaseIntArrayElements(pixelsArray, src, JNI_ABORT);
    env->ReleaseIntArrayElements(outputArray, dst, 0);
}

/**
 * High-performance C++ unsharp mask clarity filter.
 */
JNIEXPORT void JNICALL
Java_com_example_engine_NativeImageProcessor_nativeUnsharpMask(
    JNIEnv* env,
    jclass clazz,
    jintArray pixelsArray,
    jintArray outputArray,
    jint width,
    jint height,
    jfloat strength
) {
    jint* src = env->GetIntArrayElements(pixelsArray, NULL);
    jint* dst = env->GetIntArrayElements(outputArray, NULL);

    if (!src || !dst) {
        if (src) env->ReleaseIntArrayElements(pixelsArray, src, JNI_ABORT);
        if (dst) env->ReleaseIntArrayElements(outputArray, dst, 0);
        return;
    }

    float amount = strength * 1.35f;

    #pragma omp parallel for schedule(static)
    for (int y = 1; y < height - 1; ++y) {
        int yOffset = y * width;
        for (int x = 1; x < width - 1; ++x) {
            int center = src[yOffset + x];
            int cR = (center >> 16) & 0xFF;
            int cG = (center >> 8) & 0xFF;
            int cB = center & 0xFF;

            int top = src[(y - 1) * width + x];
            int bottom = src[(y + 1) * width + x];
            int left = src[yOffset + (x - 1)];
            int right = src[yOffset + (x + 1)];

            int avgR = (((top >> 16) & 0xFF) + ((bottom >> 16) & 0xFF) + ((left >> 16) & 0xFF) + ((right >> 16) & 0xFF)) >> 2;
            int avgG = (((top >> 8) & 0xFF) + ((bottom >> 8) & 0xFF) + ((left >> 8) & 0xFF) + ((right >> 8) & 0xFF)) >> 2;
            int avgB = ((top & 0xFF) + (bottom & 0xFF) + (left & 0xFF) + (right & 0xFF)) >> 2;

            int diffR = cR - avgR;
            int diffG = cG - avgG;
            int diffB = cB - avgB;

            int sharpR = static_cast<int>(cR + diffR * amount);
            int sharpG = static_cast<int>(cG + diffG * amount);
            int sharpB = static_cast<int>(cB + diffB * amount);

            dst[yOffset + x] = (0xFF << 24) |
                               (CLAMP(sharpR, 0, 255) << 16) |
                               (CLAMP(sharpG, 0, 255) << 8) |
                               CLAMP(sharpB, 0, 255);
        }
    }

    // Copy boundaries
    std::memcpy(dst, src, width * sizeof(jint));
    std::memcpy(dst + (height - 1) * width, src + (height - 1) * width, width * sizeof(jint));
    for (int y = 0; y < height; ++y) {
        dst[y * width] = src[y * width];
        dst[y * width + (width - 1)] = src[y * width + (width - 1)];
    }

    env->ReleaseIntArrayElements(pixelsArray, src, JNI_ABORT);
    env->ReleaseIntArrayElements(outputArray, dst, 0);
}

/**
 * C++ query for native engine status.
 */
JNIEXPORT jboolean JNICALL
Java_com_example_engine_NativeImageProcessor_isNativeEngineAvailable(
    JNIEnv* env,
    jclass clazz
) {
    return JNI_TRUE;
}

}
