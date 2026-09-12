package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.ThemeMode
import com.example.domain.model.AIModelInfo
import com.example.ui.components.CapsuleSegmentedControl
import com.example.ui.components.GlassSurface
import com.example.ui.components.PillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentTheme
import com.example.ui.theme.LumenhanceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    themeMode: ThemeMode,
    currentAccent: AccentTheme,
    aiModels: List<AIModelInfo>,
    storageUsageText: String,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentTheme) -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LumenhanceTheme.colors

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
                    .padding(horizontal = if (isCompact) 16.dp else 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings & Models",
                        color = colors.textPrimary,
                        fontSize = if (isCompact) 18.sp else 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(if (isCompact) 36.dp else 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 16.dp))

                // ZERO PAYWALL BANNER
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isCompact) 10.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 32.dp else 36.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "All Pro Features Unlocked",
                                    color = colors.textPrimary,
                                    fontSize = if (isCompact) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusBadge(text = "100% FREE", isAccent = true)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Zero paywalls. 1080p to 4K Ultra HD exports, 120 FPS high framerates, and all neural AI models are permanently unlocked.",
                                color = colors.textSecondary,
                                fontSize = if (isCompact) 10.sp else 11.sp,
                                lineHeight = if (isCompact) 13.sp else 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 14.dp else 20.dp))

                // SECTION 1: APPEARANCE
                SectionHeader(icon = Icons.Default.Palette, title = "Appearance & Theme", compact = isCompact)
                Spacer(modifier = Modifier.height(8.dp))

                CapsuleSegmentedControl(
                    items = listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT),
                    selectedItem = themeMode,
                    onItemSelected = onThemeModeChange,
                    compact = isCompact,
                    itemLabel = {
                        when (it) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.DARK -> if (isCompact) "Dark" else "Dark Pro"
                            ThemeMode.LIGHT -> if (isCompact) "Light" else "Light Clean"
                        }
                    }
                )

                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))

                Text(
                    text = "Accent Color",
                    color = colors.textSecondary,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccentTheme.entries.forEach { accent ->
                        val isSelected = accent == currentAccent
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 32.dp else 38.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onAccentChange(accent) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isCompact) 14.dp else 18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 18.dp else 28.dp))

                // SECTION 2: ON-DEVICE AI MODELS
                SectionHeader(icon = Icons.Default.Memory, title = "On-Device AI Models", compact = isCompact)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All model execution occurs entirely on your device's NPU/GPU. Zero network dependency.",
                    color = colors.textSecondary,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    lineHeight = if (isCompact) 14.sp else 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                aiModels.forEach { model ->
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isCompact) 10.dp else 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = model.name,
                                        color = colors.textPrimary,
                                        fontSize = if (isCompact) 12.sp else 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    StatusBadge(text = model.version, isSubtle = true)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${model.description} • ${model.accelerationMode}",
                                    color = colors.textSecondary,
                                    fontSize = if (isCompact) 10.sp else 11.sp,
                                    lineHeight = if (isCompact) 13.sp else 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(
                                text = if (isCompact) "${model.sizeMb}MB" else "${model.sizeMb} MB • READY",
                                isAccent = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))

                // SECTION 3: PRIVACY PLEDGE
                SectionHeader(icon = Icons.Default.Security, title = "Privacy Guarantee", compact = isCompact)
                Spacer(modifier = Modifier.height(6.dp))
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(if (isCompact) 10.dp else 14.dp)) {
                        Text(
                            text = "100% Offline Processing",
                            color = colors.accent,
                            fontSize = if (isCompact) 12.sp else 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lumenhance never transmits your photos, videos, or facial biometrics to external servers. All enhancements run natively in memory.",
                            color = colors.textSecondary,
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            lineHeight = if (isCompact) 14.sp else 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))

                // SECTION 4: STORAGE & CACHE
                SectionHeader(icon = Icons.Default.Storage, title = "Storage Management", compact = isCompact)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Temporary Cache",
                            color = colors.textPrimary,
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = storageUsageText,
                            color = colors.textSecondary,
                            fontSize = if (isCompact) 11.sp else 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    PillButton(
                        text = if (isCompact) "Clear" else "Clear Cache",
                        onClick = onClearCache,
                        icon = Icons.Default.DeleteSweep,
                        isPrimary = false,
                        compact = isCompact,
                        testTag = "clear_cache_button"
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 32.dp))

                // Version info
                Text(
                    text = "Lumenhance v1.0 • Designed by Rahul Shah (Editingcells)",
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    compact: Boolean = false
) {
    val colors = LumenhanceTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(if (compact) 16.dp else 18.dp)
        )
        Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
