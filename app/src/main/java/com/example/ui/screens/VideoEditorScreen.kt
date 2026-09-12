package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.style.TextOverflow
import com.example.domain.model.EnhancementConfig
import com.example.domain.model.ExportResolution
import com.example.engine.VideoEnhanceProgress
import com.example.ui.components.CapsuleSegmentedControl
import com.example.ui.components.GlassSurface
import com.example.ui.components.InbuiltVideoPlayer
import com.example.ui.components.PillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LumenhanceTheme

@Composable
fun VideoEditorScreen(
    title: String,
    videoFrames: List<Bitmap>,
    enhancedFrames: List<Bitmap>,
    currentFrameIndex: Int,
    config: EnhancementConfig,
    isProcessing: Boolean,
    progress: VideoEnhanceProgress?,
    onFrameSelect: (Int) -> Unit,
    onBackClick: () -> Unit,
    onConfigChange: (EnhancementConfig) -> Unit,
    onStartEnhance: () -> Unit,
    onPauseEnhance: () -> Unit,
    onResumeEnhance: () -> Unit,
    onCancelEnhance: () -> Unit,
    onExportClick: () -> Unit
) {
    val colors = LumenhanceTheme.colors
    val activeFrames = if (enhancedFrames.isNotEmpty()) enhancedFrames else videoFrames
    val currentBitmap = if (activeFrames.isNotEmpty() && currentFrameIndex in activeFrames.indices) {
        activeFrames[currentFrameIndex]
    } else null

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isCompact = maxWidth < 380.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // TOP NAV BAR (ADAPTIVE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompact) 10.dp else 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = title,
                            color = colors.textPrimary,
                            fontSize = if (isCompact) 15.sp else 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (enhancedFrames.isNotEmpty()) "4K HDR Master • ${enhancedFrames.size} frames" else "Video Timeline",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                PillButton(
                    text = "Export",
                    onClick = onExportClick,
                    icon = Icons.Default.IosShare,
                    isPrimary = true,
                    compact = isCompact,
                    testTag = "video_export_button"
                )
            }

            // SCROLLABLE BODY
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isCompact) 12.dp else 16.dp)
            ) {
                // INBUILT VIDEO PLAYER
                InbuiltVideoPlayer(
                    originalFrames = videoFrames,
                    enhancedFrames = enhancedFrames,
                    fps = config.videoFpsTarget,
                    isEnhanced = enhancedFrames.isNotEmpty(),
                    onFrameChanged = onFrameSelect,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

            // FRAME SCRUBBER ROW
            Text(
                text = "Frame Timeline Scrub",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(videoFrames) { idx, frame ->
                    val isSelected = idx == currentFrameIndex
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) colors.accent else colors.subtleBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onFrameSelect(idx) }
                    ) {
                        Image(
                            bitmap = frame.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // PROCESSING STATUS CARD
            if (isProcessing && progress != null) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = progress.statusText,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(progress.progressFraction * 100).toInt()}%",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = colors.accent,
                            trackColor = colors.subtleBorder
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (progress.isPaused) {
                                PillButton(
                                    text = "Resume",
                                    onClick = onResumeEnhance,
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.PlayArrow,
                                    isPrimary = true
                                )
                            } else {
                                PillButton(
                                    text = "Pause",
                                    onClick = onPauseEnhance,
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Pause,
                                    isPrimary = false
                                )
                            }
                            PillButton(
                                text = "Cancel",
                                onClick = onCancelEnhance,
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Close,
                                isPrimary = false
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // VIDEO CONFIGURATION CONTROLS
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Resolution Output (1080p to 4K Ultra HD)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Export Target Resolution",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusBadge(text = "UNLOCKED", isAccent = true)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        CapsuleSegmentedControl(
                            items = listOf(ExportResolution.RES_1080P, ExportResolution.RES_2K, ExportResolution.RES_4K),
                            selectedItem = config.targetResolution,
                            onItemSelected = { onConfigChange(config.copy(targetResolution = it)) },
                            itemLabel = { res ->
                                when (res) {
                                    ExportResolution.RES_1080P -> "1080p Full HD"
                                    ExportResolution.RES_2K -> "2K Quad HD"
                                    ExportResolution.RES_4K -> "4K Ultra HD"
                                    else -> "Native"
                                }
                            }
                        )
                    }

                    // FPS Frame Rate (Custom 24 to 120 FPS)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Frame Rate / Motion Smoothing",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${config.videoFpsTarget} FPS",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick FPS preset buttons
                        val fpsChips = listOf(24, 30, 48, 60, 90, 120)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fpsChips.forEach { fps ->
                                val isSelected = config.videoFpsTarget == fps
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) colors.accent else colors.cardSurface.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colors.accent else colors.subtleBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onConfigChange(config.copy(videoFpsTarget = fps)) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${fps}p",
                                        color = if (isSelected) Color.Black else colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom 24-120 slider
                        Slider(
                            value = config.videoFpsTarget.toFloat(),
                            onValueChange = { onConfigChange(config.copy(videoFpsTarget = it.toInt().coerceIn(24, 120))) },
                            valueRange = 24f..120f,
                            steps = 95,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.subtleBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "24 FPS (Cinematic)",
                                color = colors.textSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (config.videoFpsTarget >= 120) "120 FPS Flagship ProMotion" else if (config.videoFpsTarget >= 60) "60+ FPS Ultra Smooth" else "${config.videoFpsTarget} FPS Custom",
                                color = colors.accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "120 FPS (Pro)",
                                color = colors.textSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Target Bitrate
                    Column {
                        Text(
                            text = "Encoding Bitrate & Container",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CapsuleSegmentedControl(
                            items = listOf("High (16 Mbps)", "4K Studio (50 Mbps)", "Lossless (80 Mbps)"),
                            selectedItem = config.videoBitrate,
                            onItemSelected = { onConfigChange(config.copy(videoBitrate = it)) },
                            itemLabel = { if (isCompact) it.substringBefore(" (") else it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // BOTTOM ACTION (ADAPTIVE)
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 10.dp else 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            PillButton(
                text = if (isProcessing) "Enhancing Video..." else "Start Video Enhancement",
                onClick = onStartEnhance,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompact) 8.dp else 10.dp),
                icon = Icons.Default.AutoAwesome,
                isPrimary = true,
                isLoading = isProcessing,
                compact = isCompact,
                testTag = "start_video_enhance_button"
            )
        }
    }
}
}
