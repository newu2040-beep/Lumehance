package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LumenhanceTheme

/**
 * Apple-style Frosted Glass Surface
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val colors = LumenhanceTheme.colors
    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        colors.surfaceGlassBorder,
                        colors.surfaceGlassBorder.copy(alpha = 0.04f)
                    )
                ),
                shape = shape
            ),
        shape = shape,
        color = colors.surfaceGlass,
        tonalElevation = tonalElevation
    ) {
        content()
    }
}

/**
 * Apple-style Pill Action Button with spring scale feedback
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
    testTag: String = "pill_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pill_scale"
    )

    val colors = LumenhanceTheme.colors
    val bgColor = when {
        !enabled -> colors.textTertiary.copy(alpha = 0.15f)
        isPrimary -> colors.accent
        else -> colors.cardSurface
    }
    val contentColor = when {
        !enabled -> colors.textTertiary
        isPrimary -> if (colors.isDark) Color.Black else Color.White
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPrimary && enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(50),
                spotColor = colors.accent.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 9.dp else 12.dp
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(if (compact) 15.dp else 17.dp)
                    )
                    Spacer(modifier = Modifier.width(if (compact) 5.dp else 7.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Apple Capsule Segmented Control
 */
@Composable
fun <T> CapsuleSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    itemLabel: (T) -> String
) {
    val colors = LumenhanceTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.cardSurface)
            .border(1.dp, colors.subtleBorder.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(if (compact) 2.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val itemBg = if (isSelected) colors.elevatedSurface else Color.Transparent
                val itemTextColor = if (isSelected) colors.textPrimary else colors.textSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(itemBg)
                        .clickable { onItemSelected(item) }
                        .padding(vertical = if (compact) 5.dp else 7.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        color = itemTextColor,
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Signature Draggable Split Before/After Comparison View
 * Allows interactive dragging of the split divider across original & enhanced bitmaps
 */
@Composable
fun DraggableSplitComparison(
    originalBitmap: Bitmap,
    enhancedBitmap: Bitmap,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    compact: Boolean = false,
    onSplitFractionChanged: ((Float) -> Unit)? = null
) {
    var splitFraction by remember { mutableFloatStateOf(0.50f) }
    var isHoldingOriginal by remember { mutableStateOf(false) }
    val colors = LumenhanceTheme.colors

    val photoRatio = remember(originalBitmap) {
        if (originalBitmap.height > 0) {
            (originalBitmap.width.toFloat() / originalBitmap.height.toFloat()).coerceIn(0.75f, 1.8f)
        } else {
            1.25f
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHoldingOriginal = true
                        tryAwaitRelease()
                        isHoldingOriginal = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newFrac = (splitFraction + (dragAmount.x / size.width)).coerceIn(0.02f, 0.98f)
                    splitFraction = newFrac
                    onSplitFractionChanged?.invoke(newFrac)
                }
            }
            .testTag("split_comparison_view")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val splitX = if (isHoldingOriginal) widthPx else widthPx * splitFraction

        val origImageBitmap = remember(originalBitmap) { originalBitmap.asImageBitmap() }
        val enhImageBitmap = remember(enhancedBitmap) { enhancedBitmap.asImageBitmap() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            if (isHoldingOriginal) {
                // Showing full original
                drawImage(
                    image = origImageBitmap,
                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                )
            } else {
                // Right side: Enhanced
                drawImage(
                    image = enhImageBitmap,
                    dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                )

                // Left side: Original clipped up to splitX
                val clipPath = Path().apply {
                    addRect(Rect(0f, 0f, splitX, canvasHeight))
                }
                clipPath(clipPath) {
                    drawImage(
                        image = origImageBitmap,
                        dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                    )
                }

                // Vertical Divider Line
                drawLine(
                    color = Color.White,
                    start = Offset(splitX, 0f),
                    end = Offset(splitX, canvasHeight),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        // Draggable Handle Pill in the center of the divider
        if (!isHoldingOriginal) {
            val density = LocalDensity.current
            val handleX = with(density) {
                (splitX.toDp() - 19.dp).coerceIn(4.dp, (maxWidth - 42.dp).coerceAtLeast(4.dp))
            }

            Box(
                modifier = Modifier
                    .offset(x = handleX)
                    .align(Alignment.CenterStart)
                    .size(38.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, colors.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Slide to compare",
                    tint = Color.Black,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Badges for "Before" (Original) and "After" (Enhanced)
        if (showLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusBadge(
                    text = if (isHoldingOriginal) "ORIGINAL" else "BEFORE",
                    isSubtle = true
                )
                if (!isHoldingOriginal) {
                    StatusBadge(
                        text = "ENHANCED",
                        isAccent = true
                    )
                }
            }
        }
    }
}

/**
 * Apple Status Badge
 */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    isSubtle: Boolean = false
) {
    val colors = LumenhanceTheme.colors
    val bg = when {
        isAccent -> colors.accent
        isSubtle -> Color.Black.copy(alpha = 0.65f)
        else -> colors.cardSurface
    }
    val fg = when {
        isAccent -> if (colors.isDark) Color.Black else Color.White
        isSubtle -> Color.White
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Modern Slider with title and percentage badge
 */
@Composable
fun SpringSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueFormat: String = "${(value * 100).toInt()}%",
    enabled: Boolean = true
) {
    val colors = LumenhanceTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueFormat,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = colors.accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_$title"),
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.subtleBorder
            )
        )
    }
}

/**
 * Animated Theme Toggle Button
 */
@Composable
fun MorphThemeToggle(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LumenhanceTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.cardSurface)
            .border(1.dp, colors.subtleBorder, RoundedCornerShape(50))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = if (isDark) "Dark mode" else "Light mode",
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isDark) "Dark" else "Light",
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
