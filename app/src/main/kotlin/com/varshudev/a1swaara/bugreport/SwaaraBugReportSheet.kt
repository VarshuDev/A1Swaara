package com.varshudev.a1swaara.bugreport

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varshudev.a1swaara.BuildConfig
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.preferences.DataPreferences
import com.varshudev.a1swaara.service.PlayerService
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraDeepSurface
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraPrimaryText
import com.varshudev.a1swaara.ui.components.SwaaraSecondaryText
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.utils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Instagram-style "Rage Shake" Bug & Problem Report Bottom Sheet.
 * Allows users to write a quick description, view auto-gathered device specs,
 * attach system logs, and upload the report directly to Cloud Firestore / Supabase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaaraBugReportSheet(
    onDismiss: () -> Unit,
    binder: PlayerService.Binder?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var descriptionInput by remember { mutableStateOf("") }
    var attachLogs by remember { mutableStateOf(true) }
    var shakeEnabled by remember { mutableStateOf(DataPreferences.isShakeToReportEnabled) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionSuccessReportId by remember { mutableStateOf<String?>(null) }

    val currentMediaItem = binder?.player?.currentMediaItem
    val activeTrackTitle = currentMediaItem?.mediaMetadata?.title?.toString()
    val activeArtist = currentMediaItem?.mediaMetadata?.artist?.toString()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SwaaraDeepSurface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 28.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Drag Accent Bar
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Header Row: Bug Icon, Title, and Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        SwaaraMagenta.copy(alpha = 0.35f),
                                        SwaaraViolet.copy(alpha = 0.20f)
                                    )
                                )
                            )
                            .border(1.dp, SwaaraMagenta.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bug_outline),
                            contentDescription = null,
                            tint = SwaaraMagenta,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Did something go wrong?",
                            color = SwaaraPrimaryText,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Shake detected • Report a problem",
                            color = SwaaraSecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SwaaraElevatedSurface)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "Close",
                        tint = SwaaraSecondaryText,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Success Banner State
            if (submissionSuccessReportId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SwaaraCyan.copy(alpha = 0.12f))
                        .border(1.dp, SwaaraCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.checkmark),
                            contentDescription = null,
                            tint = SwaaraCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Report Submitted Successfully!",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reference ID: #${submissionSuccessReportId}",
                            color = SwaaraCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Thank you for helping us polish and refine A1 Swaara.",
                            color = SwaaraSecondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            } else {
                // Multi-line Issue Description Input
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    placeholder = {
                        Text(
                            text = "Briefly explain what happened or what's not working properly...",
                            color = SwaaraSecondaryText.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SwaaraElevatedSurface,
                        unfocusedContainerColor = SwaaraElevatedSurface.copy(alpha = 0.70f),
                        focusedBorderColor = SwaaraMagenta.copy(alpha = 0.85f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                        focusedTextColor = SwaaraPrimaryText,
                        unfocusedTextColor = SwaaraPrimaryText,
                        cursorColor = SwaaraMagenta
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-Captured Device & Session Context Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SwaaraElevatedSurface.copy(alpha = 0.75f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.information_circle_outline),
                                contentDescription = null,
                                tint = SwaaraCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Auto-Captured Diagnostics",
                                color = SwaaraCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Device: ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                            color = SwaaraSecondaryText,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "App Build: A1 Swaara v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            color = SwaaraSecondaryText,
                            fontSize = 11.5.sp
                        )

                        if (!activeTrackTitle.isNullOrBlank()) {
                            Text(
                                text = "Active Track: $activeTrackTitle - ${activeArtist ?: "Unknown"}",
                                color = SwaaraSecondaryText,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 1: Include System Logs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { attachLogs = !attachLogs }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attach System Logs & Diagnostics",
                            color = SwaaraPrimaryText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Includes recent logcat lines to help trace errors",
                            color = SwaaraSecondaryText,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = attachLogs,
                        onCheckedChange = { attachLogs = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SwaaraMagenta,
                            uncheckedThumbColor = SwaaraSecondaryText,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.10f)
                        )
                    )
                }

                // Toggle 2: Shake to Report Gesture
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            shakeEnabled = !shakeEnabled
                            DataPreferences.isShakeToReportEnabled = shakeEnabled
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shake Phone to Report",
                            color = SwaaraPrimaryText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Double-shake phone to open this screen anytime",
                            color = SwaaraSecondaryText,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = shakeEnabled,
                        onCheckedChange = {
                            shakeEnabled = it
                            DataPreferences.isShakeToReportEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SwaaraCyan,
                            uncheckedThumbColor = SwaaraSecondaryText,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.10f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Submit Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    SwaaraMagenta,
                                    SwaaraViolet
                                )
                            )
                        )
                        .clickable(enabled = !isSubmitting && descriptionInput.isNotBlank()) {
                            isSubmitting = true
                            coroutineScope.launch {
                                val report = SwaaraBugReportEngine.captureDiagnostics(
                                    context = context,
                                    binder = binder,
                                    description = descriptionInput,
                                    includeLogs = attachLogs
                                )
                                val result = SwaaraBugReportEngine.submitReport(report, context)
                                isSubmitting = false
                                result.onSuccess { reportId ->
                                    submissionSuccessReportId = reportId
                                    context.toast("Bug report #${reportId} registered!")
                                    delay(1600)
                                    onDismiss()
                                }.onFailure { error ->
                                    context.toast("Error submitting: ${error.message}")
                                }
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSubmitting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Uploading to Cloud...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = if (descriptionInput.isBlank()) "Type a Description to Send" else "Send Bug Report",
                            color = if (descriptionInput.isBlank()) Color.White.copy(alpha = 0.65f) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Alternative Secondary Option: GitHub Issue Direct Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            uriHandler.openUri("https://github.com/VarshuDev/A1Swaara_apk/issues/new")
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Or create a public issue on GitHub",
                        color = SwaaraCyan.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_forward),
                        contentDescription = null,
                        tint = SwaaraCyan.copy(alpha = 0.85f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
