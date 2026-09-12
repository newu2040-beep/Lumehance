package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EnhancePreset
import com.example.domain.model.EnhancementConfig
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import com.example.domain.model.ExportResolution
import com.example.engine.EnhancementOutput
import com.example.ui.components.CapsuleSegmentedControl
import com.example.ui.components.DraggableSplitComparison
import com.example.ui.components.GlassSurface
import com.example.ui.components.PillButton
import com.example.ui.components.SpringSlider
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LumenhanceTheme

@Composable
fun EditorScreen(
    title: String,
    originalBitmap: Bitmap?,
    enhancedBitmap: Bitmap?,
    config: EnhancementConfig,
    selectedPreset: EnhancePreset,
    isProcessing: Boolean,
    progressFraction: Float,
    progressStatusText: String,
    lastOutput: EnhancementOutput?,
    onBackClick: () -> Unit,
    onPresetSelect: (EnhancePreset) -> Unit,
    onConfigChange: (EnhancementConfig) -> Unit,
    onEnhanceClick: () -> Unit,
    onQueueClick: () -> Unit,
    onExportClick: () -> Unit,
    onPreviewGalleryClick: () -> Unit = {},
    onSaveToGalleryClick: () -> Unit = {}
) {
    val colors = LumenhanceTheme.colors
    var activeToolTab by remember { mutableStateOf(0) } // 0: Presets, 1: Sliders, 2: Advanced

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isCompact = maxWidth < 380.dp
        val isCompactHeight = maxHeight < 700.dp

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
                            contentDescription = "Back to library",
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
                            text = if (isProcessing) progressStatusText else "Slide line or hold to compare",
                            color = if (isProcessing) colors.accent else colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (enhancedBitmap != null) {
                        // Preview Gallery Fullscreen button
                        IconButton(
                            onClick = onPreviewGalleryClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Preview Gallery",
                                tint = colors.accent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    PillButton(
                        text = "Export",
                        onClick = onExportClick,
                        icon = Icons.Default.IosShare,
                        isPrimary = true,
                        compact = isCompact,
                        testTag = "editor_export_button"
                    )
                }
            }

            // MAIN CONTENT (SCROLLABLE)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isCompact) 12.dp else 16.dp)
            ) {
                // Interactive Before/After Split Viewer (Adaptive height)
                if (originalBitmap != null && enhancedBitmap != null) {
                    val viewerHeight = if (isCompactHeight) 220.dp else if (isCompact) 260.dp else 320.dp
                    DraggableSplitComparison(
                        originalBitmap = originalBitmap,
                        enhancedBitmap = enhancedBitmap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(viewerHeight)
                            .padding(vertical = 6.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactHeight) 200.dp else 240.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.cardSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                }

                // Live Output Inspector Card
                if (lastOutput != null) {
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "RESOLUTION",
                                        color = colors.textTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${lastOutput.originalWidth}x${lastOutput.originalHeight} → ${lastOutput.enhancedWidth}x${lastOutput.enhancedHeight}",
                                        color = colors.textPrimary,
                                        fontSize = if (isCompact) 12.sp else 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "TIME & PASSES",
                                        color = colors.textTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${lastOutput.processingTimeMs}ms • ${lastOutput.passes.size} passes",
                                        color = colors.accent,
                                        fontSize = if (isCompact) 12.sp else 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ ${lastOutput.engineName}",
                                    color = colors.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "100% On-Device • Zero Cloud",
                                    color = colors.textTertiary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sub-navigation: [Presets, Precision Sliders, Advanced]
                CapsuleSegmentedControl(
                    items = listOf("Presets", "Precision Sliders", "Advanced AI"),
                    selectedItem = when (activeToolTab) {
                        0 -> "Presets"
                        1 -> "Precision Sliders"
                        else -> "Advanced AI"
                    },
                    onItemSelected = {
                        activeToolTab = when (it) {
                            "Presets" -> 0
                            "Precision Sliders" -> 1
                            else -> 2
                        }
                    },
                    itemLabel = { it }
                )

                Spacer(modifier = Modifier.height(12.dp))

            when (activeToolTab) {
                0 -> {
                    // PRESET CARDS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EnhancePreset.entries.forEach { preset ->
                            val isSelected = preset == selectedPreset
                            GlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPresetSelect(preset) },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.title,
                                            color = if (isSelected) colors.accent else colors.textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = preset.subtitle,
                                            color = colors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(colors.accent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (colors.isDark) Color.Black else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // PRECISION SLIDERS
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
                            // Denoise
                            SpringSlider(
                                title = "Denoise & Grain Removal",
                                value = config.denoiseStrength,
                                onValueChange = { onConfigChange(config.copy(denoiseStrength = it)) }
                            )

                            // Sharpen
                            SpringSlider(
                                title = "Edge Sharpen & Deblur",
                                value = config.sharpenStrength,
                                onValueChange = { onConfigChange(config.copy(sharpenStrength = it)) }
                            )

                            // Color & Lighting
                            SpringSlider(
                                title = "Color & Dynamic Range Restore",
                                value = config.colorRestore,
                                onValueChange = { onConfigChange(config.copy(colorRestore = it, dynamicRangeBoost = it * 0.8f)) }
                            )

                            // Super-Resolution Scale
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Super-Resolution Scale",
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    StatusBadge(text = "4X UNLOCKED", isAccent = true)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                CapsuleSegmentedControl(
                                    items = listOf(1, 2, 4),
                                    selectedItem = config.upscaleFactor,
                                    onItemSelected = { onConfigChange(config.copy(upscaleFactor = it)) },
                                    itemLabel = { "${it}x" }
                                )
                            }

                            // Output Resolution
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Target Resolution",
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = config.targetResolution.label,
                                        color = colors.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                CapsuleSegmentedControl(
                                    items = listOf(ExportResolution.RES_1080P, ExportResolution.RES_2K, ExportResolution.RES_4K),
                                    selectedItem = config.targetResolution,
                                    onItemSelected = { onConfigChange(config.copy(targetResolution = it)) },
                                    itemLabel = { res ->
                                        when (res) {
                                            ExportResolution.RES_1080P -> "1080p"
                                            ExportResolution.RES_2K -> "2K"
                                            ExportResolution.RES_4K -> "4K UHD"
                                            else -> "Native"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // ADVANCED AI TOGGLES
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
                            // Archival Restore
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Archival Dust & Scratch Removal",
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Inpaints dust specks, creases, and restores faded photographic tones.",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = config.oldPhotoRestore,
                                    onCheckedChange = { onConfigChange(config.copy(oldPhotoRestore = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.4f))
                                )
                            }

                            // Face Refine (Opt-in)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Face & Portrait Detail Synthesis",
                                            color = colors.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusBadge(text = "OPT-IN", isSubtle = true)
                                    }
                                    Text(
                                        text = "Locally smooths skin texture while boosting iris and eyelash clarity. 100% on-device.",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = config.faceEnhance,
                                    onCheckedChange = { onConfigChange(config.copy(faceEnhance = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // BOTTOM ACTION ROW
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 10.dp else 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompact) 8.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillButton(
                    text = if (isProcessing) "Enhancing..." else if (enhancedBitmap != null) "Re-Enhance" else "Enhance Photo",
                    onClick = onEnhanceClick,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AutoAwesome,
                    isPrimary = true,
                    isLoading = isProcessing,
                    compact = isCompact,
                    testTag = "re_enhance_button"
                )

                if (enhancedBitmap != null) {
                    PillButton(
                        text = if (isCompact) "Save Gallery" else "Save to Gallery",
                        onClick = onSaveToGalleryClick,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Download,
                        isPrimary = false,
                        compact = isCompact,
                        testTag = "editor_save_gallery_button"
                    )
                } else {
                    PillButton(
                        text = if (isCompact) "Queue" else "Add to Queue",
                        onClick = onQueueClick,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Queue,
                        isPrimary = false,
                        compact = isCompact,
                        testTag = "add_to_queue_button"
                    )
                }
            }
        }
    }
}
}
