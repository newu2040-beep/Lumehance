package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.EnhancementEntity
import com.example.ui.components.DraggableSplitComparison
import com.example.ui.components.GlassSurface
import com.example.ui.components.PillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LumenhanceTheme
import java.io.File
import java.io.FileOutputStream

@Composable
fun PhotoPreviewGalleryScreen(
    currentOriginalBitmap: Bitmap?,
    currentEnhancedBitmap: Bitmap?,
    title: String,
    libraryItems: List<EnhancementEntity>,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSaveToGalleryClick: () -> Unit,
    onDeleteItem: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LumenhanceTheme.colors

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showCompareSlider by remember { mutableStateOf(false) }
    var showInfoPanel by remember { mutableStateOf(false) }

    val displayBitmap = currentEnhancedBitmap ?: currentOriginalBitmap

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isCompact = maxWidth < 380.dp

        // MAIN INTERACTIVE VIEW AREA
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            val maxOffsetX = (size.width * (scale - 1f)) / 2f
                            val maxOffsetY = (size.height * (scale - 1f)) / 2f
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (showCompareSlider && currentOriginalBitmap != null && currentEnhancedBitmap != null) {
                // Split slider comparison mode
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isCompact) 10.dp else 16.dp, vertical = 72.dp)
                ) {
                    DraggableSplitComparison(
                        originalBitmap = currentOriginalBitmap,
                        enhancedBitmap = currentEnhancedBitmap,
                        compact = isCompact,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (displayBitmap != null) {
                // High-res photo view with pinch-to-zoom and pan, preserving 100% native aspect ratio
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = "Photo Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "No Photo Available",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
        }

        // TOP NAVIGATION BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .padding(horizontal = if (isCompact) 10.dp else 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(if (isCompact) 32.dp else 36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    displayBitmap?.let { bmp ->
                        Text(
                            text = "${bmp.width} × ${bmp.height} • On-Device HDR",
                            color = colors.accent,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentOriginalBitmap != null && currentEnhancedBitmap != null) {
                    // Split compare toggle
                    IconButton(
                        onClick = { showCompareSlider = !showCompareSlider },
                        modifier = Modifier
                            .size(if (isCompact) 32.dp else 36.dp)
                            .clip(CircleShape)
                            .background(if (showCompareSlider) colors.accent else Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Compare",
                            tint = if (showCompareSlider) Color.Black else Color.White,
                            modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                        )
                    }
                }

                // Info toggle
                IconButton(
                    onClick = { showInfoPanel = !showInfoPanel },
                    modifier = Modifier
                        .size(if (isCompact) 32.dp else 36.dp)
                        .clip(CircleShape)
                        .background(if (showInfoPanel) colors.accent else Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = if (showInfoPanel) Color.Black else Color.White,
                        modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                    )
                }

                // Edit button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(if (isCompact) 32.dp else 36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                    )
                }
            }
        }

        // ZOOM CONTROLS FLOATING BADGE (1x, 2x, 4x)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = if (isCompact) 10.dp else 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                .padding(vertical = 3.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(1f to "1×", 2f to "2×", 4f to "4×").forEach { (targetScale, label) ->
                val isCurrent = (scale == targetScale)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) colors.accent else Color.Transparent)
                        .clickable {
                            scale = targetScale
                            if (targetScale == 1f) offset = Offset.Zero
                        }
                        .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isCurrent) Color.Black else Color.White,
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // METADATA INFO CARD OVERLAY
        AnimatedVisibility(
            visible = showInfoPanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isCompact) 76.dp else 90.dp, start = if (isCompact) 12.dp else 16.dp, end = if (isCompact) 12.dp else 16.dp)
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enhancement Diagnostics",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        StatusBadge(text = "ON-DEVICE HDR", isAccent = true)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentOriginalBitmap != null && currentEnhancedBitmap != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Original Input", color = colors.textSecondary, fontSize = 10.sp)
                                Text(
                                    text = "${currentOriginalBitmap.width} × ${currentOriginalBitmap.height}",
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Master Output", color = colors.textSecondary, fontSize = 10.sp)
                                Text(
                                    text = "${currentEnhancedBitmap.width} × ${currentEnhancedBitmap.height}",
                                    color = colors.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ 100% Native Aspect Ratio Maintained",
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // BOTTOM ACTION BAR (ADAPTIVE)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(horizontal = if (isCompact) 14.dp else 20.dp, vertical = if (isCompact) 10.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save to Gallery button
            PillButton(
                text = if (isCompact) "Save Gallery" else "Save to Gallery",
                onClick = onSaveToGalleryClick,
                icon = Icons.Default.Download,
                isPrimary = true,
                compact = isCompact,
                testTag = "gallery_save_button"
            )

            // Direct Native Share button
            IconButton(
                onClick = {
                    displayBitmap?.let { bmp ->
                        try {
                            val sharedDir = File(context.cacheDir, "shared_images").apply { if (!exists()) mkdirs() }
                            val shareFile = File(sharedDir, "Lumenhance_Photo_${System.currentTimeMillis()}.jpg")
                            val fos = FileOutputStream(shareFile)
                            bmp.compress(Bitmap.CompressFormat.JPEG, 96, fos)
                            fos.flush()
                            fos.close()

                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                shareFile
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Enhanced Photo")
                                putExtra(Intent.EXTRA_TEXT, "Enhanced with Lumenhance On-Device HDR")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            val chooser = Intent.createChooser(intent, "Share Master Photo with...").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooser)
                        } catch (e: Exception) {
                            // Handled safely
                        }
                    }
                },
                modifier = Modifier
                    .size(if (isCompact) 38.dp else 44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                )
            }
        }
    }
}
