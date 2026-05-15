package com.phoniq.app.ui.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.content.ClipData
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Inline OTP with live expiry countdown and one-tap copy (list row + thread bubble).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpCountdownCopyStrip(
    code: String,
    expiresAtEpochMillis: Long,
    modifier: Modifier = Modifier,
    onCopied: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(code) { mutableStateOf(false) }
    var nowMs by remember(code, expiresAtEpochMillis) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(code, expiresAtEpochMillis) {
        while (true) {
            val leftSec = ((expiresAtEpochMillis - System.currentTimeMillis()) / 1000L).toInt()
            if (leftSec <= 0) {
                nowMs = System.currentTimeMillis()
                break
            }
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }

    @Suppress("UNUSED_VARIABLE")
    val pulse = nowMs
    val secondsLeft = max(0, ((expiresAtEpochMillis - System.currentTimeMillis()) / 1000L).toInt())
    val expired = secondsLeft <= 0
    val timerText =
        if (expired) {
            stringResource(R.string.otp_timer_expired)
        } else {
            val m = secondsLeft / 60
            val s = secondsLeft % 60
            if (m > 0) {
                stringResource(R.string.otp_timer_mm_ss, m, s)
            } else {
                stringResource(R.string.otp_timer_ss, s)
            }
        }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (expired) Color(0x20888888) else PhoniqAccent.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, if (expired) Color(0x30888888) else PhoniqAccent.copy(alpha = 0.28f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = if (expired) Color(0xFF888888) else PhoniqAccent,
                modifier = Modifier.size(12.dp),
            )
            Text(
                code,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (expired) Color(0xFF888888) else PhoniqAccent,
            )
            Text(
                text = "·  $timerText",
                fontSize = 10.sp,
                color = if (expired) Color(0xFF666666) else PhoniqAccent.copy(alpha = 0.7f),
            )
            Surface(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("OTP", code)),
                        )
                        copied = true
                        onCopied?.invoke()
                    }
                },
                shape = RoundedCornerShape(6.dp),
                color =
                    when {
                        copied -> PhoniqSecondary.copy(alpha = 0.15f)
                        expired -> Color(0x20888888)
                        else -> PhoniqAccent.copy(alpha = 0.18f)
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        if (copied) Icons.Default.Done else Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.otp_cd_copy),
                        tint =
                            when {
                                copied -> PhoniqSecondary
                                expired -> Color(0xFF888888)
                                else -> PhoniqAccent
                            },
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        if (copied) stringResource(R.string.otp_copied_short) else stringResource(R.string.otp_copy_short),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            when {
                                copied -> PhoniqSecondary
                                expired -> Color(0xFF666666)
                                else -> PhoniqAccent
                            },
                    )
                }
            }
        }
    }
}
