package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.ThemeMode
import com.example.domain.model.MediaType
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExportSheet
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingSheet
import com.example.ui.screens.PermissionManagementSheet
import com.example.ui.screens.PhotoPreviewGalleryScreen
import com.example.ui.screens.SettingsSheet
import com.example.ui.screens.VideoEditorScreen
import com.example.ui.theme.LumenhanceAppTheme
import com.example.ui.viewmodel.EnhanceViewModel

enum class CurrentView {
    LIBRARY,
    PHOTO_EDITOR,
    VIDEO_EDITOR,
    PHOTO_PREVIEW_GALLERY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EnhanceViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val accentTheme by viewModel.accentTheme.collectAsState()
            val uiState by viewModel.uiState.collectAsState()
            val libraryItems by viewModel.libraryItems.collectAsState()
            val queueItems by viewModel.queueItems.collectAsState()

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            var currentView by remember { mutableStateOf(CurrentView.LIBRARY) }
            val snackbarHostState = remember { SnackbarHostState() }

            // Handle snackbar events
            LaunchedEffect(uiState.snackbarMessage) {
                uiState.snackbarMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.dismissSnackbar()
                }
            }

            // Handle back navigation
            BackHandler(enabled = currentView != CurrentView.LIBRARY) {
                currentView = CurrentView.LIBRARY
            }

            LumenhanceAppTheme(
                darkTheme = isDarkTheme,
                accentTheme = accentTheme
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(16.dp)
                        ) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentView,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { view ->
                            when (view) {
                                CurrentView.LIBRARY -> {
                                    LibraryScreen(
                                        libraryItems = libraryItems,
                                        queueItems = queueItems,
                                        selectedTab = uiState.selectedTab,
                                        isDarkTheme = isDarkTheme,
                                        isProcessing = uiState.isProcessing,
                                        progressFraction = uiState.progressFraction,
                                        progressStatusText = uiState.progressStatusText,
                                        activeStageName = uiState.activeStageName,
                                        onTabSelect = { viewModel.selectTab(it) },
                                        onSampleSelect = { sample ->
                                            viewModel.loadSampleMedia(sample)
                                            currentView = if (sample.type == MediaType.PHOTO) {
                                                CurrentView.PHOTO_EDITOR
                                            } else {
                                                CurrentView.VIDEO_EDITOR
                                            }
                                        },
                                        onMediaImport = { uri ->
                                            val isVideo = com.example.util.MediaHelper.isVideoUri(this@MainActivity, uri)
                                            viewModel.loadFromUri(uri, isVideo = isVideo)
                                            currentView = if (isVideo) {
                                                CurrentView.VIDEO_EDITOR
                                            } else {
                                                CurrentView.PHOTO_EDITOR
                                            }
                                        },
                                        onItemOpen = { entity ->
                                            viewModel.loadEntity(entity)
                                            currentView = if (entity.mediaType == MediaType.PHOTO) {
                                                CurrentView.PHOTO_EDITOR
                                            } else {
                                                CurrentView.VIDEO_EDITOR
                                            }
                                        },
                                        onItemDelete = { viewModel.deleteItem(it) },
                                        onProcessQueueItem = { entity ->
                                            viewModel.processQueueItem(entity)
                                            currentView = if (entity.mediaType == MediaType.PHOTO) {
                                                CurrentView.PHOTO_EDITOR
                                            } else {
                                                CurrentView.VIDEO_EDITOR
                                            }
                                        },
                                        onToggleTheme = {
                                            val nextMode = if (isDarkTheme) ThemeMode.LIGHT else ThemeMode.DARK
                                            viewModel.setThemeMode(nextMode)
                                        },
                                        onOpenSettings = { viewModel.openSettingsDialog(true) },
                                        onOpenPermissions = { viewModel.openPermissionSheet(true) },
                                        onOpenPreviewGallery = { currentView = CurrentView.PHOTO_PREVIEW_GALLERY }
                                    )
                                }

                                CurrentView.PHOTO_EDITOR -> {
                                    EditorScreen(
                                        title = uiState.activeTitle,
                                        originalBitmap = uiState.activeOriginalBitmap,
                                        enhancedBitmap = uiState.activeEnhancedBitmap,
                                        config = uiState.config,
                                        selectedPreset = uiState.selectedPreset,
                                        isProcessing = uiState.isProcessing,
                                        progressFraction = uiState.progressFraction,
                                        progressStatusText = uiState.progressStatusText,
                                        lastOutput = uiState.lastOutput,
                                        onBackClick = { currentView = CurrentView.LIBRARY },
                                        onPresetSelect = { viewModel.applyPreset(it) },
                                        onConfigChange = { viewModel.updateConfig(it) },
                                        onEnhanceClick = { viewModel.runEnhancePhoto() },
                                        onQueueClick = { viewModel.queueCurrentItem() },
                                        onExportClick = { viewModel.openExportDialog(true) },
                                        onPreviewGalleryClick = { currentView = CurrentView.PHOTO_PREVIEW_GALLERY },
                                        onSaveToGalleryClick = { viewModel.saveCurrentToGallery() }
                                    )
                                }

                                CurrentView.VIDEO_EDITOR -> {
                                    VideoEditorScreen(
                                        title = uiState.activeTitle,
                                        videoFrames = uiState.videoFrames,
                                        enhancedFrames = uiState.enhancedVideoFrames,
                                        currentFrameIndex = uiState.currentVideoFrameIndex,
                                        config = uiState.config,
                                        isProcessing = uiState.isProcessing,
                                        progress = uiState.videoProgress,
                                        onFrameSelect = { viewModel.setVideoFrameIndex(it) },
                                        onBackClick = { currentView = CurrentView.LIBRARY },
                                        onConfigChange = { viewModel.updateConfig(it) },
                                        onStartEnhance = { viewModel.runEnhanceVideo() },
                                        onPauseEnhance = { viewModel.pauseVideo() },
                                        onResumeEnhance = { viewModel.resumeVideo() },
                                        onCancelEnhance = { viewModel.cancelVideo() },
                                        onExportClick = { viewModel.openExportDialog(true) }
                                    )
                                }

                                CurrentView.PHOTO_PREVIEW_GALLERY -> {
                                    PhotoPreviewGalleryScreen(
                                        currentOriginalBitmap = uiState.activeOriginalBitmap,
                                        currentEnhancedBitmap = uiState.activeEnhancedBitmap,
                                        title = uiState.activeTitle,
                                        libraryItems = libraryItems,
                                        onBackClick = { currentView = CurrentView.LIBRARY },
                                        onEditClick = { currentView = CurrentView.PHOTO_EDITOR },
                                        onSaveToGalleryClick = { viewModel.saveCurrentToGallery() },
                                        onDeleteItem = { viewModel.deleteItem(it) }
                                    )
                                }
                            }
                        }

                        // MODALS & BOTTOM SHEETS
                        if (uiState.showPermissionSheet) {
                            PermissionManagementSheet(
                                onDismiss = { viewModel.openPermissionSheet(false) }
                            )
                        }

                        if (uiState.showOnboarding) {
                            OnboardingSheet(
                                onDismiss = { viewModel.setOnboardingCompleted() }
                            )
                        }

                        if (uiState.showSettings) {
                            SettingsSheet(
                                themeMode = themeMode,
                                currentAccent = accentTheme,
                                aiModels = uiState.aiModels,
                                storageUsageText = uiState.storageUsageText,
                                onThemeModeChange = { viewModel.setThemeMode(it) },
                                onAccentChange = { viewModel.setAccentTheme(it) },
                                onClearCache = { viewModel.clearCache() },
                                onDismiss = { viewModel.openSettingsDialog(false) }
                            )
                        }

                        if (uiState.showExportDialog) {
                            ExportSheet(
                                isVideo = uiState.isVideoMode,
                                initialFps = uiState.config.videoFpsTarget,
                                sourceWidth = uiState.sourceWidth,
                                sourceHeight = uiState.sourceHeight,
                                onDismiss = { viewModel.openExportDialog(false) },
                                onSaveToGallery = { format, quality, resolution, fps ->
                                    viewModel.saveExport(format, quality, resolution, fps)
                                },
                                onShare = { format, quality, resolution, fps ->
                                    viewModel.shareExport(format, quality, resolution, fps)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
