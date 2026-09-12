package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.EnhancementEntity
import com.example.domain.model.MediaType
import com.example.engine.SampleMediaItem
import com.example.engine.SampleMediaProvider
import com.example.ui.components.CapsuleSegmentedControl
import com.example.ui.components.GlassSurface
import com.example.ui.components.MorphThemeToggle
import com.example.ui.components.PillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LumenhanceTheme
import com.example.ui.viewmodel.LibraryTab

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun LibraryScreen(
    libraryItems: List<EnhancementEntity>,
    queueItems: List<EnhancementEntity>,
    selectedTab: LibraryTab,
    isDarkTheme: Boolean,
    onTabSelect: (LibraryTab) -> Unit,
    onSampleSelect: (SampleMediaItem) -> Unit,
    onMediaImport: (android.net.Uri) -> Unit,
    onItemOpen: (EnhancementEntity) -> Unit,
    onItemDelete: (Long) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit = {},
    onOpenPreviewGallery: () -> Unit = {}
) {
    val colors = LumenhanceTheme.colors

    // Modern Android Photo & Video Picker (zero permissions required)
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { onMediaImport(it) }
        }
    )

    val sampleItems = SampleMediaProvider.getSampleItems()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isCompact = maxWidth < 380.dp
        val isVeryCompact = maxWidth < 340.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // TOP APP BAR (ADAPTIVE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompact) 12.dp else 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 32.dp else 36.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_circle_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(if (isCompact) 28.dp else 32.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lumenhance",
                                color = colors.textPrimary,
                                fontSize = if (isCompact) 17.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1
                            )
                            if (!isVeryCompact) {
                                Spacer(modifier = Modifier.width(5.dp))
                                StatusBadge(text = "FREE", isAccent = true)
                            }
                        }
                        if (!isCompact) {
                            Text(
                                text = "On-Device Neural Enhancement",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 4.dp)
                ) {
                    IconButton(
                        onClick = onOpenPermissions,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("permissions_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permissions & Access",
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenPreviewGallery,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("preview_gallery_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Photo Preview Gallery",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    MorphThemeToggle(
                        isDark = isDarkTheme,
                        onToggle = onToggleTheme
                    )
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = if (isCompact) 12.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // HERO IMPORT BAR (ADAPTIVE ROW/COLUMN)
                item {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        if (isCompact) {
                            // Stacked layout for compact displays
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Enhance Photos & Footage",
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Recover fine details, eliminate noise & upscale to 4K on-device.",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                PillButton(
                                    text = "Import Media",
                                    onClick = {
                                        pickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    icon = Icons.Default.AddPhotoAlternate,
                                    isPrimary = true,
                                    compact = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "import_media_button"
                                )
                            }
                        } else {
                            // Side-by-side row for standard/large screens
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enhance Photos & Footage",
                                        color = colors.textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Recover fine details, eliminate high-ISO noise & upscale to 4K.",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                PillButton(
                                    text = "Import Media",
                                    onClick = {
                                        pickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    icon = Icons.Default.AddPhotoAlternate,
                                    isPrimary = true,
                                    testTag = "import_media_button"
                                )
                            }
                        }
                    }
                }

                // AUTO-SAVE & PERMISSIONS BADGE (ADAPTIVE)
                item {
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPermissions() },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
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
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Auto-Save to Gallery Active",
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Tap to review storage access & alerts",
                                        color = colors.textSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(text = "ACCESS", isAccent = true)
                        }
                    }
                }

                // SAMPLE WORKBENCH CAROUSEL
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Test Samples (No File Required)",
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap to load",
                                color = colors.accent,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sampleItems) { sample ->
                                SampleCard(
                                    item = sample,
                                    isCompact = isCompact,
                                    onClick = { onSampleSelect(sample) }
                                )
                            }
                        }
                    }
                }

                // TAB FILTER SEGMENT
                item {
                    CapsuleSegmentedControl(
                        items = listOf(LibraryTab.ALL, LibraryTab.PHOTOS, LibraryTab.VIDEOS, LibraryTab.QUEUE),
                        selectedItem = selectedTab,
                        onItemSelected = onTabSelect,
                        itemLabel = {
                            when (it) {
                                LibraryTab.ALL -> "All (${libraryItems.size})"
                                LibraryTab.PHOTOS -> "Photos"
                                LibraryTab.VIDEOS -> "Videos"
                                LibraryTab.QUEUE -> "Queue (${queueItems.size})"
                            }
                        }
                    )
                }

                // FILTERED CONTENT LIST
                val filteredItems = when (selectedTab) {
                    LibraryTab.ALL -> libraryItems
                    LibraryTab.PHOTOS -> libraryItems.filter { it.mediaType == MediaType.PHOTO }
                    LibraryTab.VIDEOS -> libraryItems.filter { it.mediaType == MediaType.VIDEO }
                    LibraryTab.QUEUE -> queueItems
                }

                if (filteredItems.isEmpty()) {
                    item {
                        EmptyLibraryState(selectedTab = selectedTab)
                    }
                } else {
                    items(filteredItems) { entity ->
                        EnhancedMediaCard(
                            item = entity,
                            onOpen = { onItemOpen(entity) },
                            onDelete = { onItemDelete(entity.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SampleCard(
    item: SampleMediaItem,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LumenhanceTheme.colors

    Card(
        modifier = Modifier
            .width(if (isCompact) 154.dp else 180.dp)
            .clickable(onClick = onClick)
            .testTag("sample_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 94.dp else 110.dp)
                    .background(Color.Black)
            ) {
                if (item.drawableResId != null) {
                    Image(
                        painter = painterResource(id = item.drawableResId),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(modifier = Modifier.padding(6.dp).align(Alignment.TopStart)) {
                    StatusBadge(text = item.badge, isAccent = true)
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    color = colors.textPrimary,
                    fontSize = if (isCompact) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EnhancedMediaCard(
    item: EnhancementEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LumenhanceTheme.colors

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("library_item_${item.id}"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.mediaType == MediaType.PHOTO) Icons.Default.Image else Icons.Default.Movie,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(text = item.presetUsed, isSubtle = true)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.enhancedWidth > 0) "${item.originalWidth}x${item.originalHeight} → ${item.enhancedWidth}x${item.enhancedHeight} (${item.fileSizeFormatted})" else "Status: ${item.status}",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.processingTimeMs > 0) {
                    Text(
                        text = "Processed in ${item.processingTimeMs}ms • On-Device",
                        color = colors.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(selectedTab: LibraryTab) {
    val colors = LumenhanceTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.cardSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = when (selectedTab) {
                    LibraryTab.QUEUE -> "Queue is empty"
                    LibraryTab.VIDEOS -> "No enhanced videos yet"
                    LibraryTab.PHOTOS -> "No enhanced photos yet"
                    else -> "No enhancements yet"
                },
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick a sample above or import your own media to start.",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}
