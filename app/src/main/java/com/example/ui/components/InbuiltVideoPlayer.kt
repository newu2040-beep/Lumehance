package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.BoxWithConstraints
import com.example.ui.theme.LumenhanceTheme
import kotlinx.coroutines.delay

@Composable
fun InbuiltVideoPlayer(
    originalFrames: List<Bitmap>,
    enhancedFrames: List<Bitmap>,
    modifier: Modifier = Modifier,
    fps: Int = 30,
    isEnhanced: Boolean = enhancedFrames.isNotEmpty(),
    onFrameChanged: (Int) -> Unit = {}
) {
    val colors = LumenhanceTheme.colors
    val hasEnhanced = enhancedFrames.isNotEmpty()
    var showEnhanced by remember(hasEnhanced) { mutableStateOf(hasEnhanced) }
    val activeFrames = if (showEnhanced && hasEnhanced) enhancedFrames else originalFrames

    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    // Coroutine loop for continuous playback
    LaunchedEffect(isPlaying, playbackSpeed, activeFrames.size, isLooping) {
        if (isPlaying && activeFrames.isNotEmpty()) {
            val frameDelay = ((1000L / fps.coerceAtLeast(10)) / playbackSpeed).toLong().coerceAtLeast(16L)
            while (isPlaying) {
                delay(frameDelay)
                if (currentFrameIndex < activeFrames.size - 1) {
                    currentFrameIndex++
                    onFrameChanged(currentFrameIndex)
                } else {
                    if (isLooping) {
                        currentFrameIndex = 0
                        onFrameChanged(0)
                    } else {
                        isPlaying = false
                    }
                }
            }
        }
    }

    // Ensure frame index is valid
    val safeIndex = currentFrameIndex.coerceIn(0, (activeFrames.size - 1).coerceAtLeast(0))
    val currentBitmap = if (activeFrames.isNotEmpty()) activeFrames[safeIndex] else null

    // Video Player Content
    val playerContent = @Composable { inFullscreen: Boolean ->
        BoxWithConstraints(
            modifier = if (inFullscreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else {
                modifier
                    .fillMaxWidth()
                    .aspectRatio(1.33f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF030712))
                    .border(1.dp, colors.subtleBorder, RoundedCornerShape(22.dp))
            }.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
        ) {
            val isCompact = maxWidth < 380.dp
            val isVeryCompact = maxWidth < 330.dp

            // Render active frame
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Video Frame Player",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (inFullscreen) ContentScale.Fit else ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No Video Frames Available",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            // Top Status Overlay
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 8.dp else 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            text = if (showEnhanced && hasEnhanced) {
                                if (isCompact) "4K MASTER" else "4K HDR MASTER"
                            } else {
                                if (isCompact) "ORIG" else "ORIGINAL LOG"
                            },
                            isAccent = showEnhanced && hasEnhanced
                        )
                        if (!isVeryCompact) {
                            StatusBadge(
                                text = "${String.format("%.1fx", playbackSpeed)} SPEED",
                                isSubtle = true
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasEnhanced) {
                            // Toggle between original and enhanced
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .clickable { showEnhanced = !showEnhanced }
                                    .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Compare,
                                        contentDescription = "Compare",
                                        tint = if (showEnhanced) colors.accent else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isCompact) {
                                            if (showEnhanced) "Orig" else "4K"
                                        } else {
                                            if (showEnhanced) "Show Orig" else "Show 4K"
                                        },
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Fullscreen toggle
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (inFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Player Controls
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                            )
                        )
                        .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 6.dp else 10.dp)
                ) {
                    // Timeline Scrubber Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentSeconds = (safeIndex.toFloat() / fps.coerceAtLeast(1))
                        val totalSeconds = (activeFrames.size.toFloat() / fps.coerceAtLeast(1))
                        Text(
                            text = String.format("%02d:%04.1f", (currentSeconds / 60).toInt(), currentSeconds % 60),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Slider(
                            value = safeIndex.toFloat(),
                            onValueChange = { newIdx ->
                                currentFrameIndex = newIdx.toInt().coerceIn(0, (activeFrames.size - 1).coerceAtLeast(0))
                                onFrameChanged(currentFrameIndex)
                            },
                            valueRange = 0f..(activeFrames.size - 1).coerceAtLeast(1).toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                                .testTag("video_player_scrubber"),
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            )
                        )

                        Text(
                            text = String.format("%02d:%04.1f", (totalSeconds / 60).toInt(), totalSeconds % 60),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Transport Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Loop toggle
                        IconButton(
                            onClick = { isLooping = !isLooping },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Loop",
                                tint = if (isLooping) colors.accent else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Center Controls (Prev Frame, Play/Pause, Next Frame)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Step Back 1 Frame
                            IconButton(
                                onClick = {
                                    if (currentFrameIndex > 0) {
                                        currentFrameIndex--
                                        onFrameChanged(currentFrameIndex)
                                    }
                                },
                                modifier = Modifier.size(if (isCompact) 30.dp else 34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Step Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                                )
                            }

                            // Main Play/Pause Button
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 38.dp else 44.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                                    .clickable { isPlaying = !isPlaying }
                                    .testTag("video_player_play_pause"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
                                )
                            }

                            // Step Forward 1 Frame
                            IconButton(
                                onClick = {
                                    if (currentFrameIndex < activeFrames.size - 1) {
                                        currentFrameIndex++
                                        onFrameChanged(currentFrameIndex)
                                    }
                                },
                                modifier = Modifier.size(if (isCompact) 30.dp else 34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Step Forward",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                                )
                            }
                        }

                        // Playback Speed Cycle (0.5x, 1x, 1.5x, 2x)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    playbackSpeed = when (playbackSpeed) {
                                        0.5f -> 1.0f
                                        1.0f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 0.5f
                                    }
                                }
                                .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            )
        ) {
            playerContent(true)
        }
    } else {
        playerContent(false)
    }
}
