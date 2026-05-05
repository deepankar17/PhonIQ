package com.phoniq.app.ui.phone

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.ui.theme.PhoniqOnBackground
import com.phoniq.app.ui.theme.PhoniqTextSecondaryMock
import kotlinx.coroutines.delay

private val OutgoingCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF0D0B1E),
                Color(0xFF0A1A0F),
                Color(0xFF050A0F),
            ),
    )

private val DialActionKeyColor = Color(0xFF1C1C2E)

/**
 * Full-screen in-call UI (`phoniq-mockup-v1.html` `#screen-calling` outgoing chrome + action grid).
 */
@Composable
fun InCallScreen(
    callerName: String,
    callerNumber: String,
    onHangUp: () -> Unit,
    onUserMessage: (String) -> Unit = {},
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }
    var holdOn by remember { mutableStateOf(false) }
    var recordingOn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(OutgoingCallGradient),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp),
            ) {
                Box(Modifier.align(Alignment.Center)) {
                    CallPulseRings()
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CallerAvatar(name = callerName)
                }
            }

            Text(
                text = callerName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PhoniqOnBackground,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = callerNumber,
                fontSize = 13.sp,
                color = PhoniqTextSecondaryMock,
            )

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.incall_status_in_call),
                    fontSize = 14.sp,
                    color = Color(0xFFAAAAAA),
                )
                StatusDotPulse()
            }

            Text(
                text = formatDuration(elapsedSeconds),
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace,
                color = PhoniqOnBackground,
                modifier = Modifier.padding(top = 18.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            if (showDialpad) {
                DialpadContent(onDismiss = { showDialpad = false })
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        CallActionCell(
                            icon = if (muted) Icons.Filled.MicOff else Icons.Outlined.Mic,
                            label = stringResource(if (muted) R.string.incall_unmute else R.string.incall_mute),
                            active = muted,
                            onClick = { muted = !muted },
                        )
                        CallActionCell(
                            icon = Icons.Default.Dialpad,
                            label = stringResource(R.string.incall_keypad),
                            active = false,
                            onClick = { showDialpad = true },
                        )
                        CallActionCell(
                            icon = if (speakerOn) Icons.Filled.SpeakerPhone else Icons.Outlined.VolumeUp,
                            label = stringResource(R.string.incall_speaker),
                            active = speakerOn,
                            onClick = { speakerOn = !speakerOn },
                        )
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val holdToastMsg = stringResource(R.string.toast_incall_hold)
                        val recordToastMsg = stringResource(R.string.toast_incall_record)
                        val smsToastMsg = stringResource(R.string.toast_incall_sms)
                        CallActionCell(
                            icon = Icons.Filled.Pause,
                            label = stringResource(R.string.incall_hold),
                            active = holdOn,
                            onClick = {
                                holdOn = !holdOn
                                onUserMessage(holdToastMsg)
                            },
                        )
                        CallActionCell(
                            icon = Icons.Filled.FiberManualRecord,
                            label = stringResource(R.string.incall_record),
                            active = recordingOn,
                            onClick = {
                                recordingOn = !recordingOn
                                onUserMessage(recordToastMsg)
                            },
                        )
                        CallActionCell(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = stringResource(R.string.incall_sms),
                            active = false,
                            onClick = { onUserMessage(smsToastMsg) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF3B3B), Color(0xFFCC1A1A)),
                            ),
                        )
                        .clickable(
                            indication = ripple(),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onHangUp,
                        ),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_incall_end),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun CallPulseRings() {
    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        repeat(4) { index ->
            val transition = rememberInfiniteTransition(label = "ring$index")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(2200, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(index * 550),
                    ),
                label = "ringProgress",
            )
            Box(
                modifier =
                    Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            scaleX = 1f + progress * 1.6f
                            scaleY = 1f + progress * 1.6f
                            alpha = (1f - progress) * 0.6f
                        }
                        .border(1.5.dp, PhoniqAccent.copy(alpha = 0.28f), CircleShape),
            )
        }
    }
}

@Composable
private fun StatusDotPulse() {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val t = rememberInfiniteTransition(label = "dot$index")
            val a by t.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(700, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 200),
                    ),
                label = "dotA",
            )
            Box(
                modifier =
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .graphicsLayer { alpha = a }
                        .background(Color(0xFFAAAAAA)),
            )
        }
    }
}

@Composable
private fun CallerAvatar(name: String) {
    val initials =
        name.trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(100.dp)
                .border(3.dp, PhoniqAccent.copy(alpha = 0.45f), CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF8C5FE8), Color(0xFF6C63FF)),
                    ),
                ),
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CallActionCell(
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
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (active) PhoniqAccent else DialActionKeyColor),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = PhoniqTextSecondaryMock,
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}
