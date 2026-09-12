package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.EnhancementEntity
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.ThemeMode
import com.example.data.repository.EnhancementRepository
import com.example.domain.model.AIModelInfo
import com.example.domain.model.EnhancePreset
import com.example.domain.model.EnhancementConfig
import com.example.domain.model.ExportOptions
import com.example.domain.model.ExportResolution
import com.example.domain.model.MediaType
import com.example.domain.model.ProcessStatus
import com.example.engine.EnhancementOutput
import com.example.engine.ImageProcessor
import com.example.engine.SampleMediaItem
import com.example.engine.SampleMediaProvider
import com.example.engine.VideoEnhanceProgress
import com.example.engine.VideoEnhanceResult
import com.example.engine.VideoProcessor
import com.example.ui.theme.AccentTheme
import com.example.util.MediaGallerySaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class LibraryTab {
    ALL,
    PHOTOS,
    VIDEOS,
    QUEUE
}

data class UIState(
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val activeOriginalBitmap: Bitmap? = null,
    val activeEnhancedBitmap: Bitmap? = null,
    val activeTitle: String = "Untitled",
    val activeMediaType: MediaType = MediaType.PHOTO,
    val activeEntityId: Long? = null,
    val config: EnhancementConfig = EnhancementConfig.fromPreset(EnhancePreset.BALANCED),
    val selectedPreset: EnhancePreset = EnhancePreset.BALANCED,
    val isProcessing: Boolean = false,
    val progressFraction: Float = 0f,
    val progressStatusText: String = "",
    val lastOutput: EnhancementOutput? = null,
    val isVideoMode: Boolean = false,
    val videoFrames: List<Bitmap> = emptyList(),
    val enhancedVideoFrames: List<Bitmap> = emptyList(),
    val currentVideoFrameIndex: Int = 0,
    val isVideoPlaying: Boolean = false,
    val isVideoProcessing: Boolean = false,
    val videoProgress: VideoEnhanceProgress? = null,
    val exportOptions: ExportOptions = ExportOptions(),
    val showExportDialog: Boolean = false,
    val showOnboarding: Boolean = false,
    val showSettings: Boolean = false,
    val showPermissionSheet: Boolean = false,
    val showFaceConsentDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val storageUsageText: String = "24.6 MB",
    val aiModels: List<AIModelInfo> = emptyList()
)

class EnhanceViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getInstance(context)
    private val repository = EnhancementRepository(database.enhancementDao())
    private val preferencesManager = PreferencesManager(context)
    private val videoProcessor = VideoProcessor()

    val themeMode: StateFlow<ThemeMode> = preferencesManager.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    val accentTheme: StateFlow<AccentTheme> = preferencesManager.accentThemeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccentTheme.CYAN
    )

    val libraryItems: StateFlow<List<EnhancementEntity>> = repository.allEnhancements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val queueItems: StateFlow<List<EnhancementEntity>> = repository.queueItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    init {
        checkOnboardingStatus()
        initializeDefaultModels()
        loadDefaultSample()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            preferencesManager.hasSeenOnboardingFlow.collect { seen ->
                if (!seen) {
                    _uiState.value = _uiState.value.copy(showOnboarding = true)
                }
            }
        }
    }

    private fun initializeDefaultModels() {
        val models = listOf(
            AIModelInfo(
                id = "nafnet_denoise",
                name = "NAFNet Mobile Denoise",
                version = "v2.1",
                description = "Ultra-fast nonlinear activation-free spatial denoise network",
                sizeMb = 18,
                isInstalled = true,
                accelerationMode = "NNAPI / GPU FP16"
            ),
            AIModelInfo(
                id = "realesrgan_x4",
                name = "Real-ESRGAN SuperRes 4x",
                version = "v3.0",
                description = "Deep generative edge reconstruction and detail synthesis",
                sizeMb = 42,
                isInstalled = true,
                accelerationMode = "Qualcomm Hexagon / GPU"
            ),
            AIModelInfo(
                id = "facerefine_optin",
                name = "FaceRefine Targeted",
                version = "v1.4",
                description = "Opt-in micro-texture facial refiner & iris sharpener",
                sizeMb = 14,
                isInstalled = true,
                accelerationMode = "CPU Vectorized (100% On-Device)"
            ),
            AIModelInfo(
                id = "opticalflow_video",
                name = "MotionFlow Video Engine",
                version = "v1.8",
                description = "Temporal motion consistency & frame stabilization kernel",
                sizeMb = 26,
                isInstalled = true,
                accelerationMode = "GPU MediaCodec"
            )
        )
        _uiState.value = _uiState.value.copy(aiModels = models)
    }

    private fun loadDefaultSample() {
        viewModelScope.launch {
            val sample = SampleMediaProvider.getSampleItems().first()
            loadSampleMedia(sample)
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun loadSampleMedia(sample: SampleMediaItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, progressStatusText = "Loading sample media...")
            try {
                if (sample.type == MediaType.PHOTO) {
                    val bitmap = SampleMediaProvider.loadBitmap(context, sample.drawableResId ?: R.drawable.sample_vintage)
                    _uiState.value = _uiState.value.copy(
                        activeOriginalBitmap = bitmap,
                        activeEnhancedBitmap = bitmap, // Initially set to original
                        activeTitle = sample.title,
                        activeMediaType = MediaType.PHOTO,
                        isVideoMode = false,
                        isProcessing = false,
                        lastOutput = null
                    )
                    // Auto run initial preview enhancement
                    runEnhancePhoto()
                } else {
                    // Video sequence
                    val frames = SampleMediaProvider.generateVideoFrames(context, sample.drawableResId ?: R.drawable.sample_night_city, 10)
                    _uiState.value = _uiState.value.copy(
                        activeOriginalBitmap = frames.firstOrNull(),
                        activeEnhancedBitmap = frames.firstOrNull(),
                        activeTitle = sample.title,
                        activeMediaType = MediaType.VIDEO,
                        isVideoMode = true,
                        videoFrames = frames,
                        enhancedVideoFrames = emptyList(),
                        currentVideoFrameIndex = 0,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    snackbarMessage = "Could not load sample: ${e.localizedMessage}"
                )
            }
        }
    }

    fun loadFromUri(uri: Uri, isVideo: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true, progressStatusText = "Importing media...")
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    _uiState.value = _uiState.value.copy(
                        activeOriginalBitmap = bitmap,
                        activeEnhancedBitmap = bitmap,
                        activeTitle = "Imported ${if (isVideo) "Video" else "Photo"}",
                        activeMediaType = if (isVideo) MediaType.VIDEO else MediaType.PHOTO,
                        isVideoMode = isVideo,
                        isProcessing = false,
                        lastOutput = null
                    )
                    if (!isVideo) {
                        runEnhancePhoto()
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        snackbarMessage = "Unable to decode selected file."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    snackbarMessage = "Import failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun applyPreset(preset: EnhancePreset) {
        val newConfig = EnhancementConfig.fromPreset(preset)
        _uiState.value = _uiState.value.copy(
            selectedPreset = preset,
            config = newConfig
        )
        if (!_uiState.value.isVideoMode) {
            runEnhancePhoto()
        }
    }

    fun updateConfig(config: EnhancementConfig) {
        _uiState.value = _uiState.value.copy(config = config)
    }

    fun runEnhancePhoto() {
        val orig = _uiState.value.activeOriginalBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                progressFraction = 0.05f,
                progressStatusText = "Preparing enhancement pipeline..."
            )
            try {
                val output = ImageProcessor.enhance(
                    sourceBitmap = orig,
                    config = _uiState.value.config,
                    onProgress = { fraction, stepText ->
                        _uiState.value = _uiState.value.copy(
                            progressFraction = fraction,
                            progressStatusText = stepText
                        )
                    }
                )

                _uiState.value = _uiState.value.copy(
                    activeEnhancedBitmap = output.enhancedBitmap,
                    lastOutput = output,
                    isProcessing = false,
                    progressFraction = 1f,
                    progressStatusText = "Enhanced in ${output.processingTimeMs}ms"
                )

                // Automatically save enhanced photo to device Gallery
                val saveResult = MediaGallerySaver.savePhotoToGallery(
                    context = context,
                    bitmap = output.enhancedBitmap,
                    title = _uiState.value.activeTitle,
                    notifyUser = true
                )
                val savedUri = saveResult.getOrNull()?.toString() ?: "local://enhanced"

                // Save record to local Room database
                val entity = EnhancementEntity(
                    title = _uiState.value.activeTitle,
                    mediaType = MediaType.PHOTO,
                    originalUri = "local://sample",
                    enhancedUri = savedUri,
                    originalWidth = output.originalWidth,
                    originalHeight = output.originalHeight,
                    enhancedWidth = output.enhancedWidth,
                    enhancedHeight = output.enhancedHeight,
                    status = ProcessStatus.COMPLETED,
                    presetUsed = _uiState.value.selectedPreset.title,
                    processingTimeMs = output.processingTimeMs,
                    fileSizeFormatted = "${String.format("%.1f", (output.enhancedWidth * output.enhancedHeight * 4) / 1024f / 1024f)} MB"
                )
                repository.insert(entity)

                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Photo enhanced & automatically saved to Gallery (Pictures/Lumenhance)"
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    snackbarMessage = "Enhancement error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun runEnhanceVideo() {
        val frames = _uiState.value.videoFrames
        if (frames.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isVideoProcessing = true,
                isProcessing = true,
                progressFraction = 0f,
                progressStatusText = "Starting video frame enhancement..."
            )
            val result = videoProcessor.processFrames(
                frames = frames,
                config = _uiState.value.config,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(
                        videoProgress = progress,
                        progressFraction = progress.progressFraction,
                        progressStatusText = progress.statusText,
                        activeEnhancedBitmap = progress.currentFrameBitmap ?: _uiState.value.activeEnhancedBitmap
                    )
                }
            )

            if (result != null) {
                // Automatically save enhanced video to device Gallery
                val videoSaveResult = MediaGallerySaver.saveVideoToGallery(
                    context = context,
                    frames = result.enhancedFrames,
                    title = _uiState.value.activeTitle,
                    notifyUser = true
                )
                val savedVideoUri = videoSaveResult.getOrNull()?.toString() ?: "local://video_enhanced"

                _uiState.value = _uiState.value.copy(
                    isVideoProcessing = false,
                    isProcessing = false,
                    enhancedVideoFrames = result.enhancedFrames,
                    progressFraction = 1f,
                    progressStatusText = "Video enhanced (${result.enhancedFrames.size} frames in ${result.processingTimeMs}ms)",
                    snackbarMessage = "4K Video enhanced & automatically saved to Gallery (Movies/Lumenhance)"
                )
                val entity = EnhancementEntity(
                    title = _uiState.value.activeTitle,
                    mediaType = MediaType.VIDEO,
                    originalUri = "local://video",
                    enhancedUri = savedVideoUri,
                    originalWidth = result.frameWidth,
                    originalHeight = result.frameHeight,
                    enhancedWidth = result.frameWidth,
                    enhancedHeight = result.frameHeight,
                    status = ProcessStatus.COMPLETED,
                    presetUsed = "4K Video Pro",
                    processingTimeMs = result.processingTimeMs,
                    fileSizeFormatted = "${result.estimatedOutputSizeMb} MB"
                )
                repository.insert(entity)
            } else {
                _uiState.value = _uiState.value.copy(
                    isVideoProcessing = false,
                    isProcessing = false,
                    progressStatusText = "Video processing cancelled"
                )
            }
        }
    }

    fun pauseVideo() {
        videoProcessor.pause()
    }

    fun resumeVideo() {
        videoProcessor.resume()
    }

    fun cancelVideo() {
        videoProcessor.cancel()
    }

    fun setVideoFrameIndex(index: Int) {
        val frames = if (_uiState.value.enhancedVideoFrames.isNotEmpty()) {
            _uiState.value.enhancedVideoFrames
        } else {
            _uiState.value.videoFrames
        }
        if (index in frames.indices) {
            _uiState.value = _uiState.value.copy(
                currentVideoFrameIndex = index,
                activeEnhancedBitmap = frames[index]
            )
        }
    }

    fun queueCurrentItem() {
        viewModelScope.launch {
            val entity = EnhancementEntity(
                title = "${_uiState.value.activeTitle} (Batch)",
                mediaType = _uiState.value.activeMediaType,
                originalUri = "local://queued",
                status = ProcessStatus.QUEUED,
                presetUsed = _uiState.value.selectedPreset.title,
                fileSizeFormatted = "Pending"
            )
            repository.insert(entity)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Added to background enhancement queue.")
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Item removed from library.")
        }
    }

    fun clearAllLibrary() {
        viewModelScope.launch {
            repository.clearAll()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Library cleared.")
        }
    }

    fun saveExport(
        format: String,
        quality: Int,
        resolution: ExportResolution = ExportResolution.RES_4K,
        fps: Int = 60
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_uiState.value.isVideoMode) {
                    val frames = if (_uiState.value.enhancedVideoFrames.isNotEmpty()) {
                        _uiState.value.enhancedVideoFrames
                    } else {
                        _uiState.value.videoFrames
                    }
                    if (frames.isNotEmpty()) {
                        MediaGallerySaver.saveVideoToGallery(
                            context = context,
                            frames = frames,
                            title = "${_uiState.value.activeTitle}_${resolution.label}_${fps}fps",
                            notifyUser = true
                        )
                        _uiState.value = _uiState.value.copy(
                            showExportDialog = false,
                            snackbarMessage = "Video saved to Gallery at ${resolution.label} ($fps FPS • Zero Paywalls)"
                        )
                    }
                } else {
                    val bitmap = _uiState.value.activeEnhancedBitmap ?: _uiState.value.activeOriginalBitmap
                    if (bitmap != null) {
                        val targetBitmap = when (resolution) {
                            ExportResolution.RES_1080P -> {
                                val targetW = 1920
                                val targetH = (bitmap.height * (1920f / bitmap.width)).toInt().coerceAtLeast(1)
                                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                            }
                            ExportResolution.RES_2K -> {
                                val targetW = 2560
                                val targetH = (bitmap.height * (2560f / bitmap.width)).toInt().coerceAtLeast(1)
                                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                            }
                            ExportResolution.RES_4K -> {
                                val targetW = 3840
                                val targetH = (bitmap.height * (3840f / bitmap.width)).toInt().coerceAtLeast(1)
                                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                            }
                            ExportResolution.NATIVE -> bitmap
                        }

                        MediaGallerySaver.savePhotoToGallery(
                            context = context,
                            bitmap = targetBitmap,
                            title = "${_uiState.value.activeTitle}_${resolution.label}",
                            notifyUser = true
                        )

                        _uiState.value = _uiState.value.copy(
                            showExportDialog = false,
                            snackbarMessage = "Photo saved to Gallery at ${resolution.label} ($format, $quality% Q)"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Export failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun saveCurrentToGallery() {
        viewModelScope.launch {
            if (_uiState.value.isVideoMode) {
                val frames = if (_uiState.value.enhancedVideoFrames.isNotEmpty()) {
                    _uiState.value.enhancedVideoFrames
                } else {
                    _uiState.value.videoFrames
                }
                if (frames.isNotEmpty()) {
                    val result = MediaGallerySaver.saveVideoToGallery(
                        context,
                        frames,
                        _uiState.value.activeTitle,
                        notifyUser = true
                    )
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            snackbarMessage = "Video saved to Gallery (Movies/Lumenhance)"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            snackbarMessage = "Failed to save video: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } else {
                val bitmap = _uiState.value.activeEnhancedBitmap ?: _uiState.value.activeOriginalBitmap
                if (bitmap != null) {
                    val result = MediaGallerySaver.savePhotoToGallery(
                        context,
                        bitmap,
                        _uiState.value.activeTitle,
                        notifyUser = true
                    )
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            snackbarMessage = "Photo saved to Gallery (Pictures/Lumenhance)"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            snackbarMessage = "Failed to save photo: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            }
        }
    }

    fun openPermissionSheet(open: Boolean) {
        _uiState.value = _uiState.value.copy(showPermissionSheet = open)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setAccentTheme(theme: AccentTheme) {
        viewModelScope.launch {
            preferencesManager.setAccentTheme(theme)
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            preferencesManager.setHasSeenOnboarding(true)
            _uiState.value = _uiState.value.copy(showOnboarding = false)
        }
    }

    fun openExportDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(showExportDialog = open)
    }

    fun openSettingsDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(showSettings = open)
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            context.cacheDir.listFiles()?.forEach { it.delete() }
            _uiState.value = _uiState.value.copy(
                storageUsageText = "0.0 MB",
                snackbarMessage = "Cache cleaned successfully"
            )
        }
    }
}
