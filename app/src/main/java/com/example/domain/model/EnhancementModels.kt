package com.example.domain.model

enum class MediaType {
    PHOTO,
    VIDEO
}

enum class ProcessStatus {
    IDLE,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}

enum class EnhancePreset(val title: String, val subtitle: String) {
    BALANCED("Pro Auto", "Balanced denoise, micro-contrast & edge clarity"),
    LOW_LIGHT("Low-Light Clean", "Aggressive ISO noise reduction & dynamic range rescue"),
    SUPER_RES_4X("Super Res 4x", "Desktop-grade edge-directed super-resolution upscaler"),
    VINTAGE_RESTORE("Archival Restore", "Descratch, color revival & historical tone balance"),
    PORTRAIT("Face & Portrait", "Opt-in targeted skin texture refinement & iris recovery")
}

enum class ExportResolution(
    val label: String,
    val width: Int,
    val height: Int,
    val description: String
) {
    RES_1080P("1080p Full HD", 1920, 1080, "1920 × 1080 • Crisp & Compact"),
    RES_2K("2K Quad HD", 2560, 1440, "2560 × 1440 • High Fidelity"),
    RES_4K("4K Ultra HD", 3840, 2160, "3840 × 2160 • Studio Master UHD"),
    NATIVE("Source Native", 0, 0, "Preserve input dimensions");

    fun getTargetDimensions(srcWidth: Int, srcHeight: Int): Pair<Int, Int> {
        if (srcWidth <= 0 || srcHeight <= 0) return Pair(1920, 1080)
        if (this == NATIVE) return Pair(srcWidth, srcHeight)

        val targetMax = when (this) {
            RES_1080P -> 1920
            RES_2K -> 2560
            RES_4K -> 3840
            NATIVE -> maxOf(srcWidth, srcHeight)
        }

        val aspect = srcWidth.toFloat() / srcHeight.toFloat()
        val (w, h) = if (srcWidth >= srcHeight) {
            val w = targetMax
            val h = (targetMax / aspect).toInt().coerceAtLeast(2)
            Pair(w, if (h % 2 != 0) h + 1 else h)
        } else {
            val h = targetMax
            val w = (targetMax * aspect).toInt().coerceAtLeast(2)
            Pair(if (w % 2 != 0) w + 1 else w, h)
        }
        return Pair(w, h)
    }

    fun getDynamicDescription(srcWidth: Int, srcHeight: Int): String {
        if (srcWidth <= 0 || srcHeight <= 0) return description
        val (w, h) = getTargetDimensions(srcWidth, srcHeight)
        return when (this) {
            RES_1080P -> "$w × $h • Crisp & Compact"
            RES_2K -> "$w × $h • High Fidelity"
            RES_4K -> "$w × $h • Studio Master UHD"
            NATIVE -> "$w × $h • 100% Original Ratio"
        }
    }
}

data class EnhancementConfig(
    val denoiseStrength: Float = 0.45f,
    val sharpenStrength: Float = 0.50f,
    val upscaleFactor: Int = 2,
    val colorRestore: Float = 0.40f,
    val dynamicRangeBoost: Float = 0.35f,
    val oldPhotoRestore: Boolean = false,
    val faceEnhance: Boolean = false,
    val videoFpsTarget: Int = 60,
    val targetResolution: ExportResolution = ExportResolution.RES_4K,
    val videoBitrate: String = "4K Studio (50 Mbps)"
) {
    companion object {
        fun fromPreset(preset: EnhancePreset): EnhancementConfig = when (preset) {
            EnhancePreset.BALANCED -> EnhancementConfig(
                denoiseStrength = 0.40f,
                sharpenStrength = 0.45f,
                upscaleFactor = 2,
                colorRestore = 0.35f,
                dynamicRangeBoost = 0.30f,
                oldPhotoRestore = false,
                faceEnhance = false,
                videoFpsTarget = 60,
                targetResolution = ExportResolution.RES_4K
            )
            EnhancePreset.LOW_LIGHT -> EnhancementConfig(
                denoiseStrength = 0.75f,
                sharpenStrength = 0.40f,
                upscaleFactor = 2,
                colorRestore = 0.60f,
                dynamicRangeBoost = 0.65f,
                oldPhotoRestore = false,
                faceEnhance = false,
                videoFpsTarget = 60,
                targetResolution = ExportResolution.RES_4K
            )
            EnhancePreset.SUPER_RES_4X -> EnhancementConfig(
                denoiseStrength = 0.35f,
                sharpenStrength = 0.65f,
                upscaleFactor = 4,
                colorRestore = 0.30f,
                dynamicRangeBoost = 0.25f,
                oldPhotoRestore = false,
                faceEnhance = false,
                videoFpsTarget = 60,
                targetResolution = ExportResolution.RES_4K
            )
            EnhancePreset.VINTAGE_RESTORE -> EnhancementConfig(
                denoiseStrength = 0.55f,
                sharpenStrength = 0.45f,
                upscaleFactor = 2,
                colorRestore = 0.75f,
                dynamicRangeBoost = 0.50f,
                oldPhotoRestore = true,
                faceEnhance = false,
                videoFpsTarget = 30,
                targetResolution = ExportResolution.RES_4K
            )
            EnhancePreset.PORTRAIT -> EnhancementConfig(
                denoiseStrength = 0.35f,
                sharpenStrength = 0.40f,
                upscaleFactor = 2,
                colorRestore = 0.40f,
                dynamicRangeBoost = 0.30f,
                oldPhotoRestore = false,
                faceEnhance = true,
                videoFpsTarget = 60,
                targetResolution = ExportResolution.RES_4K
            )
        }
    }
}

data class ExportOptions(
    val format: String = "JPEG", // JPEG, PNG, WEBP, MP4
    val qualityPercent: Int = 95,
    val targetResolution: ExportResolution = ExportResolution.RES_4K,
    val targetFps: Int = 60,
    val bitrateMbps: Int = 50
)

data class AIModelInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val sizeMb: Int,
    val isInstalled: Boolean,
    val accelerationMode: String, // NNAPI / GPU / CPU Vector
    val privacyStatus: String = "100% On-Device Offline"
)
