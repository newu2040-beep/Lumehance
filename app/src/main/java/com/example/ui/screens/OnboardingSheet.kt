package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.R
import com.example.ui.components.PillButton
import com.example.ui.theme.LumenhanceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSheet(
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Lumenhance brand icon
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 64.dp else 80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(colors.accent.copy(alpha = 0.35f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_circle_logo),
                        contentDescription = "Lumenhance Logo",
                        modifier = Modifier
                            .size(if (isCompact) 52.dp else 64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 16.dp))

                Text(
                    text = "Welcome to Lumenhance",
                    color = colors.textPrimary,
                    fontSize = if (isCompact) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Desktop-grade photo & video enhancement on Android with transparent on-device processing.",
                    color = colors.textSecondary,
                    fontSize = if (isCompact) 13.sp else 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isCompact) 18.sp else 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(if (isCompact) 18.dp else 28.dp))

                // Feature 1: Privacy pledge
                OnboardingFeatureRow(
                    icon = Icons.Default.Shield,
                    title = "100% On-Device & Private",
                    description = "No cloud upload or account required. All neural filters and pixel processing run offline on your device.",
                    compact = isCompact
                )

                Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 18.dp))

                // Feature 2: Super-resolution
                OnboardingFeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Super-Resolution & Restoration",
                    description = "Real bilateral denoising, edge-directed upscaling (2x/4x), and archival scratch & tone revival.",
                    compact = isCompact
                )

                Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 18.dp))

                // Feature 3: Photo & Video in one
                OnboardingFeatureRow(
                    icon = Icons.Default.Bolt,
                    title = "Photo & Video Unified",
                    description = "Clean low-light camera grain, sharpen soft footage, and compare results with interactive split-slider.",
                    compact = isCompact
                )

                Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 32.dp))

                PillButton(
                    text = "Continue to Lumenhance",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = true,
                    compact = isCompact,
                    testTag = "onboarding_continue_button"
                )
            }
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    compact: Boolean = false
) {
    val colors = LumenhanceTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 34.dp else 40.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp)
            )
        }

        Spacer(modifier = Modifier.width(if (compact) 12.dp else 16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = if (compact) 14.sp else 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = if (compact) 12.sp else 13.sp,
                lineHeight = if (compact) 16.sp else 18.sp
            )
        }
    }
}
