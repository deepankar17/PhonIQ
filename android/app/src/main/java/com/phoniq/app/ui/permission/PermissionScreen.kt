package com.phoniq.app.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqBackground
import com.phoniq.app.ui.theme.PhoniqBorder
import com.phoniq.app.ui.theme.PhoniqOnBackground
import com.phoniq.app.ui.theme.PhoniqSecondary
import com.phoniq.app.ui.theme.PhoniqSurface
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock

// ---------------------------------------------------------------------------
// Permissions required for core functionality
// ---------------------------------------------------------------------------

val CORE_PERMISSIONS =
    arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
    )

val OPTIONAL_PERMISSIONS =
    arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
    )

fun allCorePermissionsGranted(context: android.content.Context): Boolean =
    CORE_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

// ---------------------------------------------------------------------------
// Full-screen permission rationale + request composable
// (`phoniq-mockup-v1.html` onboarding card density + dark surfaces)
// ---------------------------------------------------------------------------

@Composable
fun PermissionScreen(
    onPermissionsResult: (allGranted: Boolean) -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val allPerms = CORE_PERMISSIONS + OPTIONAL_PERMISSIONS

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            val coreGranted = CORE_PERMISSIONS.all { results[it] == true }
            onPermissionsResult(coreGranted)
        }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PhoniqBackground,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PhoniqAccent, PhoniqSecondary),
                                ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.permission_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PhoniqOnBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 25.sp,
                    modifier = Modifier.fillMaxWidth(0.92f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.permission_subtitle),
                    fontSize = 13.sp,
                    color = PhoniqTextSecondaryMock,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier =
                        Modifier
                            .fillMaxWidth(0.85f)
                            .padding(horizontal = 8.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PermissionCard(
                    icon = Icons.Outlined.Sms,
                    title = stringResource(R.string.permission_card_sms_title),
                    description = stringResource(R.string.permission_card_sms_body),
                )
                PermissionCard(
                    icon = Icons.Outlined.PhoneInTalk,
                    title = stringResource(R.string.permission_card_call_title),
                    description = stringResource(R.string.permission_card_call_body),
                )
                PermissionCard(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.permission_card_phone_title),
                    description = stringResource(R.string.permission_card_phone_body),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { launcher.launch(allPerms) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PhoniqAccent),
                ) {
                    Text(
                        stringResource(R.string.permission_grant),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
                TextButton(onClick = onSkip) {
                    Text(
                        stringResource(R.string.permission_demo_skip),
                        color = PhoniqTextSecondaryMock,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PhoniqSurface,
        border = BorderStroke(1.dp, PhoniqBorder),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PhoniqAccent,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PhoniqOnBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    fontSize = 13.sp,
                    color = PhoniqTextSecondaryMock,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Compact inline banner (used inside shell when permissions were skipped)
// ---------------------------------------------------------------------------

@Composable
fun PermissionBanner(onGrant: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PhoniqAccent.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.permission_banner_text),
                fontSize = 12.sp,
                color = PhoniqOnBackground,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onGrant) {
                Text(
                    stringResource(R.string.permission_banner_grant),
                    color = PhoniqAccent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
