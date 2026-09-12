package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.EnhancementEntity
import com.example.domain.model.MediaType
import com.example.domain.model.ProcessStatus
import com.example.engine.SampleMediaItem
import com.example.engine.SampleMediaProvider
import com.example.ui.components.CapsuleSegmentedControl
import com.example.ui.components.GlassSurface
import com.example.ui.components.MorphThemeToggle
import com.example.ui.components.PillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LumenhanceTheme
import com.example.ui.viewmodel.EnhancementProgressEvent
import com.example.ui.viewmodel.LibraryTab
import com.example.util.MediaHelper

@Composable
fun LibraryScreen(
    libraryItems: List<EnhancementEntity>,
    queueItems: List<EnhancementEntity>,
    selectedTab: LibraryTab,
    isDarkTheme: Boolean,
    isProcessing: Boolean = false,
    progressFraction: Float = 0f,
    progressStatusText: String = "",
    activeStageName: String = "",
    onTabSelect: (LibraryTab) -> Unit,
    onSampleSelect: (SampleMediaItem) -> Unit,
    onMediaImport: (Uri) -> Unit,
    onItemOpen: (EnhancementEntity) -> Unit,
    onItemDelete: (Long) -> Unit,
    onProcessQueueItem: (EnhancementEntity) -> Unit = {},
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
            .background(MaterialTheme.colorScheme.background)
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
                            contentDescription = "Permissions & Full Access",
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

            // LIVE REAL-TIME STREAMING PROGRESS BANNER
            AnimatedVisibility(
                visible = isProcessing,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isCompact) 12.dp else 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    strokeColor = colors.accent.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeStageName.isNotEmpty()) activeStageName else "Neural Engine Active",
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                color = colors.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = colors.accent,
                            trackColor = colors.accent.copy(alpha = 0.2f)
                        )
                        if (progressStatusText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = progressStatusText,
                                color = colors.textSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

                // AUTO-SAVE & PERMISSIONS BADGE
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
                                        text = "Full Files & Auto-Save Active",
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Tap to review full storage permissions & settings",
                                        color = colors.textSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(text = "PERMISSIONS", isAccent = true)
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
                    val enhancedVideosCount = libraryItems.count { it.mediaType == MediaType.VIDEO && it.status == ProcessStatus.COMPLETED }
                    val pendingCount = queueItems.size

                    CapsuleSegmentedControl(
                        items = listOf(LibraryTab.ALL, LibraryTab.PHOTOS, LibraryTab.VIDEOS, LibraryTab.QUEUE),
                        selectedItem = selectedTab,
                        onItemSelected = onTabSelect,
                        itemLabel = {
                            when (it) {
                                LibraryTab.ALL -> "All (${libraryItems.size})"
                                LibraryTab.PHOTOS -> "Photos"
                                LibraryTab.VIDEOS -> if (enhancedVideosCount > 0) "Videos ($enhancedVideosCount)" else "Videos"
                                LibraryTab.QUEUE -> "Pending ($pendingCount)"
                            }
                        }
                    )
                }

                // DEDICATED RECENTLY ENHANCED VIDEOS SECTION (Shown on VIDEOS or ALL tab)
                val recentlyEnhancedVideos = libraryItems.filter { it.mediaType == MediaType.VIDEO && it.status == ProcessStatus.COMPLETED }
                val pendingVideos = queueItems.filter { it.mediaType == MediaType.VIDEO }

                if ((selectedTab == LibraryTab.VIDEOS || selectedTab == LibraryTab.ALL) && recentlyEnhancedVideos.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Recently Enhanced Videos",
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                StatusBadge(text = "${recentlyEnhancedVideos.size} Ready", isAccent = true)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    items(recentlyEnhancedVideos) { videoEntity ->
                        EnhancedVideoCard(
                            item = videoEntity,
                            isCompact = isCompact,
                            onPreview = { onItemOpen(videoEntity) },
                            onDelete = { onItemDelete(videoEntity.id) }
                        )
                    }
                }

                // DEDICATED PENDING & QUEUED VIDEOS SECTION
                if ((selectedTab == LibraryTab.VIDEOS || selectedTab == LibraryTab.QUEUE || selectedTab == LibraryTab.ALL) && pendingVideos.isNotEmpty()) {
                    item {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pending Videos (Queue)",
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                StatusBadge(text = "${pendingVideos.size} Pending", isSubtle = true)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    items(pendingVideos) { pendingEntity ->
                        PendingVideoCard(
                            item = pendingEntity,
                            isCompact = isCompact,
                            onProcess = { onProcessQueueItem(pendingEntity) },
                            onPreview = { onItemOpen(pendingEntity) },
                            onDelete = { onItemDelete(pendingEntity.id) }
                        )
                    }
                }

                // ALL OTHER FILTERED CONTENT
                val regularFilteredItems = when (selectedTab) {
                    LibraryTab.ALL -> libraryItems.filter { it.mediaType != MediaType.VIDEO || it.status != ProcessStatus.COMPLETED }
                    LibraryTab.PHOTOS -> libraryItems.filter { it.mediaType == MediaType.PHOTO }
                    LibraryTab.VIDEOS -> emptyList() // Rendered in dedicated video section above
                    LibraryTab.QUEUE -> queueItems.filter { it.mediaType != MediaType.VIDEO }
                }

                if (regularFilteredItems.isNotEmpty()) {
                    if (selectedTab == LibraryTab.ALL && recentlyEnhancedVideos.isNotEmpty()) {
                        item {
                            Text(
                                text = "Enhanced Photos & Library",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(regularFilteredItems) { entity ->
                        EnhancedMediaCard(
                            item = entity,
                            onOpen = { onItemOpen(entity) },
                            onDelete = { onItemDelete(entity.id) }
                        )
                    }
                }

                if (selectedTab == LibraryTab.VIDEOS && recentlyEnhancedVideos.isEmpty() && pendingVideos.isEmpty()) {
                    item {
                        EmptyLibraryState(selectedTab = LibraryTab.VIDEOS)
                    }
                } else if (selectedTab == LibraryTab.QUEUE && pendingVideos.isEmpty() && regularFilteredItems.isEmpty()) {
                    item {
                        EmptyLibraryState(selectedTab = LibraryTab.QUEUE)
                    }
                } else if (selectedTab == LibraryTab.PHOTOS && regularFilteredItems.isEmpty()) {
                    item {
                        EmptyLibraryState(selectedTab = LibraryTab.PHOTOS)
                    }
                } else if (selectedTab == LibraryTab.ALL && libraryItems.isEmpty() && queueItems.isEmpty()) {
                    item {
                        EmptyLibraryState(selectedTab = LibraryTab.ALL)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
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
                if (item.type == MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video Sample",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
fun EnhancedVideoCard(
    item: EnhancementEntity,
    isCompact: Boolean = false,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LumenhanceTheme.colors
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.originalUri, item.enhancedUri) {
        val uriStr = item.enhancedUri ?: item.originalUri
        try {
            val uri = Uri.parse(uriStr)
            if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                thumbnailBitmap = MediaHelper.extractVideoThumbnail(context, uri)
            }
        } catch (_: Throwable) {
        }
    }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview)
            .testTag("video_item_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        strokeColor = colors.accent.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail Preview Container with Play Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 130.dp else 160.dp)
                    .background(Color.Black)
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.sample_night_city),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Gradient Vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Top Tags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(text = "4K UHD 60FPS", isAccent = true)
                        StatusBadge(text = "AUDIO PRESERVED", isSubtle = false)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Center Large Play Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.85f))
                        .clickable(onClick = onPreview),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video Preview",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Bottom Metadata Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.enhancedWidth > 0) "${item.originalWidth}x${item.originalHeight} → ${item.enhancedWidth}x${item.enhancedHeight}" else "4K Master",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = item.fileSizeFormatted,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Action & Details Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.processingTimeMs > 0) "Enhanced in ${item.processingTimeMs}ms • On-Device Neural Pipeline" else "Enhanced Master",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                PillButton(
                    text = "Preview",
                    onClick = onPreview,
                    icon = Icons.Default.PlayCircleFilled,
                    isPrimary = true,
                    compact = true
                )
            }
        }
    }
}

@Composable
fun PendingVideoCard(
    item: EnhancementEntity,
    isCompact: Boolean = false,
    onProcess: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LumenhanceTheme.colors

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEAB308).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = Color(0xFFEAB308),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                    StatusBadge(text = "PENDING", isSubtle = true)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Preset: ${item.presetUsed} • Video Stream",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillButton(
                        text = "Enhance Now",
                        onClick = onProcess,
                        icon = Icons.Default.AutoAwesome,
                        isPrimary = true,
                        compact = true
                    )
                    PillButton(
                        text = "Inspect",
                        onClick = onPreview,
                        isPrimary = false,
                        compact = true
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.cardSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (selectedTab) {
                    LibraryTab.ALL -> Icons.Default.PhotoLibrary
                    LibraryTab.PHOTOS -> Icons.Default.Image
                    LibraryTab.VIDEOS -> Icons.Default.Movie
                    LibraryTab.QUEUE -> Icons.Default.Queue
                },
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = when (selectedTab) {
                LibraryTab.ALL -> "No Media Enhanced Yet"
                LibraryTab.PHOTOS -> "No Enhanced Photos"
                LibraryTab.VIDEOS -> "No Enhanced Videos Yet"
                LibraryTab.QUEUE -> "Pending Queue is Empty"
            },
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (selectedTab) {
                LibraryTab.ALL -> "Import a photo/video or select a test sample above."
                LibraryTab.PHOTOS -> "Import a high-ISO or vintage photo to restore."
                LibraryTab.VIDEOS -> "Import a video or select the Night City sample to enhance."
                LibraryTab.QUEUE -> "Queue video or batch processing items for sequential enhancement."
            },
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}
