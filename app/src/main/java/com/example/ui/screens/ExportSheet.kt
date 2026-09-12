package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ExportResolution
import com.example.ui.components.CapsuleSegmentedControl
import com.example.ui.components.GlassSurface
import com.example.ui.components.PillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LumenhanceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    isVideo: Boolean = false,
    initialFps: Int = 60,
    sourceWidth: Int = 0,
    sourceHeight: Int = 0,
    onDismiss: () -> Unit,
    onSaveToGallery: (format: String, quality: Int, resolution: ExportResolution, fps: Int) -> Unit,
    onShare: (format: String, quality: Int, resolution: ExportResolution, fps: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LumenhanceTheme.colors

    val formatOptions = if (isVideo) {
        listOf("MP4 (HEVC)", "MP4 (H.264)", "WEBM")
    } else {
        listOf("JPEG", "PNG (Lossless)", "WEBP")
    }

    var selectedFormat by remember { mutableStateOf(formatOptions.first()) }
    var qualityFloat by remember { mutableFloatStateOf(0.96f) }
    var selectedResolution by remember { mutableStateOf(ExportResolution.RES_4K) }
    var targetFps by remember { mutableIntStateOf(initialFps.coerceIn(24, 120)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.elevatedSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 380.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompact) 14.dp else 22.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isVideo) "Export Ultra Video" else "Export Ultra Media",
                                color = colors.textPrimary,
                                fontSize = if (isCompact) 17.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(text = "UNLOCKED", isAccent = true)
                        }
                        Text(
                            text = if (isVideo) "Audio Preserved • Native Ratio • Up to 120 FPS" else "1080p to 4K Ultra HD • Native Ratio",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // STUDIO FEATURES CARD
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "All Studio Features Unlocked",
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isVideo) "Master 4K resolution, original audio stream preservation & zero watermarks." else "Full 4K resolution, maximum fidelity export with zero watermarks.",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION: RESOLUTION (1080p to 4K Ultra HD)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Master Resolution",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = selectedResolution.label,
                        color = colors.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Resolution Options List with dynamic aspect-ratio-accurate dimensions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExportResolution.entries.forEach { res ->
                        val isSelected = res == selectedResolution
                        val descriptionText = res.getDynamicDescription(sourceWidth, sourceHeight)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) colors.accent.copy(alpha = 0.12f)
                                    else colors.cardSurface.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.accent else colors.subtleBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedResolution = res }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = res.label,
                                            color = if (isSelected) colors.accent else colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (res == ExportResolution.RES_4K) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            StatusBadge(text = "ULTRA", isAccent = true)
                                        }
                                    }
                                    Text(
                                        text = descriptionText,
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(colors.accent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION: FORMAT
                Text(
                    text = "File Format",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                CapsuleSegmentedControl(
                    items = formatOptions,
                    selectedItem = selectedFormat,
                    onItemSelected = { selectedFormat = it },
                    itemLabel = { if (isCompact) it.substringBefore(" (") else it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CONDITIONAL CONTROLS: QUALITY (PHOTOS) OR FPS (VIDEOS)
                if (!isVideo && !selectedFormat.contains("PNG")) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Compression Quality",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(qualityFloat * 100).toInt()}%",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = qualityFloat,
                            onValueChange = { qualityFloat = it },
                            valueRange = 0.50f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.subtleBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isVideo) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Target Frame Rate",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "$targetFps FPS",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = targetFps.toFloat(),
                            onValueChange = { targetFps = it.toInt().coerceIn(24, 120) },
                            valueRange = 24f..120f,
                            steps = 95,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.subtleBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // EXPORT ACTION BUTTONS: SAVE TO GALLERY AND SHARE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
                ) {
                    PillButton(
                        text = if (isCompact) "Save Gallery" else "Save to Gallery",
                        onClick = {
                            onSaveToGallery(
                                selectedFormat,
                                (qualityFloat * 100).toInt(),
                                selectedResolution,
                                targetFps
                            )
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Download,
                        isPrimary = true,
                        compact = isCompact,
                        testTag = "save_export_button"
                    )

                    PillButton(
                        text = if (isCompact) "Share" else "Share Master",
                        onClick = {
                            onShare(
                                selectedFormat,
                                (qualityFloat * 100).toInt(),
                                selectedResolution,
                                targetFps
                            )
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Share,
                        isPrimary = false,
                        compact = isCompact,
                        testTag = "share_export_button"
                    )
                }
            }
        }
    }
}
