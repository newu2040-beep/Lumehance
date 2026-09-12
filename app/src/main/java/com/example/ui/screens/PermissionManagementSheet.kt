package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.GlassSurface
import com.example.ui.components.PillButton
import com.example.ui.theme.LumenhanceTheme
import com.example.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagementSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LumenhanceTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var status by remember { mutableStateOf(PermissionManager.checkStatus(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = PermissionManager.checkStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        status = PermissionManager.checkStatus(context)
    }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 36.dp else 44.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permissions",
                            tint = colors.accent,
                            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Permissions & Full Access",
                            color = colors.textPrimary,
                            fontSize = if (isCompact) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Full gallery, batch queue, and background processing access",
                            color = colors.textSecondary,
                            fontSize = if (isCompact) 11.sp else 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 14.dp else 20.dp))

                // Master "Allow All Permissions" Button
                PillButton(
                    text = if (status.areAllGranted) "Full Access Active ✓" else "Allow Full Access",
                    onClick = {
                        val permissions = PermissionManager.getAllInitialPermissions()
                        multiPermissionLauncher.launch(permissions)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !status.hasFilesAccess) {
                            PermissionManager.openAllFilesAccessSettings(context)
                        }
                    },
                    isPrimary = !status.areAllGranted,
                    compact = isCompact,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "allow_all_permissions_button"
                )

                Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 18.dp))

                // Item 1: Notifications
                PermissionRowItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications & Live Progress",
                    subtitle = "Real-time alerts when video/photo processing completes",
                    isGranted = status.hasNotifications,
                    compact = isCompact,
                    onGrantClick = {
                        PermissionManager.getNotificationPermission()?.let {
                            multiPermissionLauncher.launch(arrayOf(it))
                        } ?: run {
                            PermissionManager.openAppSettings(context)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Item 2: Gallery & Media
                PermissionRowItem(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Gallery & Photos / Videos",
                    subtitle = "Access high-res footage and auto-save master exports",
                    isGranted = status.hasGallery,
                    compact = isCompact,
                    onGrantClick = {
                        multiPermissionLauncher.launch(PermissionManager.getRequiredMediaPermissions())
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Item 3: Full Files Access
                PermissionRowItem(
                    icon = Icons.Default.Folder,
                    title = "Full Files & Storage Access",
                    subtitle = "Enables batch queue processing and direct disk master saving",
                    isGranted = status.hasFilesAccess,
                    compact = isCompact,
                    onGrantClick = {
                        PermissionManager.openAllFilesAccessSettings(context)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Item 4: App System Settings
                PermissionRowItem(
                    icon = Icons.Default.Settings,
                    title = "System App Settings",
                    subtitle = "Manage all permissions directly in Android System Settings",
                    isGranted = false,
                    buttonLabel = "Open",
                    compact = isCompact,
                    onGrantClick = {
                        PermissionManager.openAppSettings(context)
                    }
                )

                Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))

                PillButton(
                    text = "Done",
                    onClick = onDismiss,
                    isPrimary = false,
                    compact = isCompact,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    buttonLabel: String = "Allow",
    compact: Boolean = false,
    onGrantClick: () -> Unit
) {
    val colors = LumenhanceTheme.colors

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 32.dp else 38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f)
                            else colors.accent.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) Color(0xFF10B981) else colors.accent,
                        modifier = Modifier.size(if (compact) 16.dp else 20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
                Column {
                    Text(
                        text = title,
                        color = colors.textPrimary,
                        fontSize = if (compact) 12.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = colors.textSecondary,
                        fontSize = if (compact) 10.sp else 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isGranted) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Granted",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Allowed",
                        color = Color(0xFF10B981),
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent)
                        .clickable { onGrantClick() }
                        .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 5.dp else 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonLabel,
                        color = Color.Black,
                        fontSize = if (compact) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
