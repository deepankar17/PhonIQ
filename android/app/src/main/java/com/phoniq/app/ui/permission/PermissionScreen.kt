package com.phoniq.app.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.phoniq.app.ui.theme.PhoniqAccent

// ---------------------------------------------------------------------------
// Permissions required for core functionality
// ---------------------------------------------------------------------------

val CORE_PERMISSIONS = arrayOf(
    Manifest.permission.READ_SMS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.CALL_PHONE,
)

val OPTIONAL_PERMISSIONS = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.POST_NOTIFICATIONS,
)

fun allCorePermissionsGranted(context: android.content.Context): Boolean =
    CORE_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

// ---------------------------------------------------------------------------
// Full-screen permission rationale + request composable
// ---------------------------------------------------------------------------

@Composable
fun PermissionScreen(
    onPermissionsResult: (allGranted: Boolean) -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val allPerms = CORE_PERMISSIONS + OPTIONAL_PERMISSIONS

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val coreGranted = CORE_PERMISSIONS.all { results[it] == true }
        onPermissionsResult(coreGranted)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = PhoniqAccent,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "PhonIQ needs access",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "To show your real calls, messages, and financial data — all processed offline, on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionCard(
                    icon = Icons.Outlined.Sms,
                    title = "SMS & Messages",
                    description = "Read messages to detect OTPs, transactions, and bills. Never uploaded.",
                )
                PermissionCard(
                    icon = Icons.Outlined.PhoneInTalk,
                    title = "Call Log & Contacts",
                    description = "Show your real call history and match sender names to contacts.",
                )
                PermissionCard(
                    icon = Icons.Outlined.Lock,
                    title = "Phone State",
                    description = "Required to act as a dialer — detect active calls and answer/reject.",
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { launcher.launch(allPerms) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PhoniqAccent),
                ) {
                    Text("Grant permissions", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                TextButton(onClick = onSkip) {
                    Text(
                        "Continue with demo data",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
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
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                "Showing demo data · Grant permissions to sync your real data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onGrant) {
                Text("Grant", color = PhoniqAccent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
