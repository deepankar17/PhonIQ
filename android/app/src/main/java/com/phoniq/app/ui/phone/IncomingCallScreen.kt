package com.phoniq.app.ui.phone

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.util.startSmsCompose

private val IncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF060D12), Color(0xFF071A10), Color(0xFF0A0F08)),
    )

private val QuickActionFill = Color(0xFF1C1C2E)

/**
 * Full-screen incoming call (`phoniq-mockup-v1.html` `#screen-incoming`).
 */
@Composable
fun IncomingCallScreen(
    callerName: String,
    callerNumber: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onUserMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var muted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(false) }

    fun openSmsToCaller() {
        if (!context.startSmsCompose(callerNumber)) {
            onUserMessage(context.getString(R.string.snackbar_no_sms_app))
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(IncomingGradient),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                IncomingBlinkDot()
                Text(
                    text = stringResource(R.string.incoming_call_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00C472),
                    letterSpacing = 2.sp,
                )
            }

            Spacer(Modifier.height(28.dp))
            CallerAvatar(name = callerName)
            Text(
                text = callerName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8E8EE),
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = callerNumber,
                fontSize = 13.sp,
                color = Color(0xFF888888),
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier =
                    Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x1A00C472))
                        .border(1.dp, Color(0x4D00C472), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF00C472),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.incoming_verified_badge),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00C472),
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IncomingQuickAction(
                    icon = if (muted) Icons.Filled.MicOff else Icons.Outlined.Mic,
                    label = stringResource(if (muted) R.string.incall_unmute else R.string.incall_mute),
                    active = muted,
                    onClick = { muted = !muted },
                )
                IncomingQuickAction(
                    icon = if (speakerOn) Icons.Filled.SpeakerPhone else Icons.AutoMirrored.Outlined.VolumeUp,
                    label = stringResource(R.string.incall_speaker),
                    active = speakerOn,
                    onClick = { speakerOn = !speakerOn },
                )
                IncomingQuickAction(
                    icon = Icons.AutoMirrored.Filled.Message,
                    label = stringResource(R.string.incoming_message_short),
                    active = false,
                    onClick = { openSmsToCaller() },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141420))
                        .border(1.dp, Color(0xFF2A2A3E), RoundedCornerShape(20.dp))
                        .clickable(
                            indication = ripple(),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { openSmsToCaller() },
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.incoming_sms_reply),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFAAAAAA),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF3B3B), Color(0xFFCC1A1A)),
                                    ),
                                )
                                .clickable(
                                    indication = ripple(),
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onDecline,
                                ),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.incoming_decline),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.incoming_decline),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFAAAAAA),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF00C472), Color(0xFF009950)),
                                    ),
                                )
                                .clickable(
                                    indication = ripple(),
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onAnswer,
                                ),
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = stringResource(R.string.incoming_answer),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.incoming_answer),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFAAAAAA),
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomingBlinkDot() {
    val t = rememberInfiniteTransition(label = "blink")
    val alpha by t.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "blinkA",
    )
    Box(
        modifier =
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .graphicsLayer { this.alpha = alpha }
                .background(Color(0xFF00C472)),
    )
}

@Composable
private fun IncomingQuickAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .size(width = 72.dp, height = 76.dp)
                .clickable(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (active) PhoniqAccent else QuickActionFill),
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF888888))
    }
}
