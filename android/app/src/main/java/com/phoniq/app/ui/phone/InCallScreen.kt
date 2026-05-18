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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.phoniq.app.R
import com.phoniq.app.telecom.CallAudioRoute
import com.phoniq.app.telecom.CallStateRepository
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.components.contactAvatarClip
import com.phoniq.app.ui.components.contactAvatarShapeForSize
import com.phoniq.app.ui.theme.LocalShowInCallTimer
import com.phoniq.app.ui.theme.LocalThemePreset
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.util.PersonalizationStore
import com.phoniq.app.util.ThemeUiBindings
import com.phoniq.app.util.startSmsCompose
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

private val IosInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF0A1628),
                Color(0xFF0F3550),
                Color(0xFF050A0F),
            ),
    )

private val SamsungInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF12151C),
                Color(0xFF1A2433),
                Color(0xFF090B0F),
            ),
    )

private val DailyDialInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF0A0812),
                Color(0xFF161028),
                Color(0xFF05040A),
            ),
    )

private val NeoMirrorInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF020306),
                Color(0xFF0A1620),
                Color(0xFF000102),
            ),
    )

private val Dialer360InCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF0C1629),
                Color(0xFF152A45),
                Color(0xFF060A12),
            ),
    )

private val NothingDialInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF000000),
                Color(0xFF121212),
                Color(0xFF000000),
            ),
    )

private val GlassDialInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF353951),
                Color(0xFF454C70),
                Color(0xFF252836),
            ),
    )

private val AiTranslatorInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF221434),
                Color(0xFF352560),
                Color(0xFF160E28),
            ),
    )

private val SaasWidgetInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF1F2937),
                Color(0xFF374151),
                Color(0xFF111827),
            ),
    )

private val MessageAppInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF281E32),
                Color(0xFF382848),
                Color(0xFF1A1420),
            ),
    )

private val ModernMessagingInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF0F1419),
                Color(0xFF1C2333),
                Color(0xFF080B0E),
            ),
    )

private val ConversationFlowInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF1C202C),
                Color(0xFF2A3140),
                Color(0xFF12151E),
            ),
    )

private val MicroMotionInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF181020),
                Color(0xFF2A2240),
                Color(0xFF0E0A12),
            ),
    )

private val TealTideInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF0A141A),
                Color(0xFF183844),
                Color(0xFF060A0E),
            ),
    )

private val IndigoLineInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF100E18),
                Color(0xFF242038),
                Color(0xFF080710),
            ),
    )

private val SkyPanelInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF0E0E10),
                Color(0xFF18202A),
                Color(0xFF060608),
            ),
    )

private val VioletStudioInCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF140A1A),
                Color(0xFF2A1C38),
                Color(0xFF0A0610),
            ),
    )

private val Material3InCallGradient =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFF10131A),
                Color(0xFF1C2838),
                Color(0xFF0A0E14),
            ),
    )

private fun parseInCallChrome(key: String): InCallPhoneChrome =
    when (key) {
        "Samsung" -> InCallPhoneChrome.Samsung
        "DailyDial" -> InCallPhoneChrome.DailyDial
        "NeoMirror" -> InCallPhoneChrome.NeoMirror
        "Dialer360" -> InCallPhoneChrome.Dialer360
        "NothingDial" -> InCallPhoneChrome.NothingDial
        "GlassDial" -> InCallPhoneChrome.GlassDial
        "AiTranslator" -> InCallPhoneChrome.AiTranslator
        "SaasWidget" -> InCallPhoneChrome.SaasWidget
        "MessageApp" -> InCallPhoneChrome.MessageApp
        "ModernMessaging" -> InCallPhoneChrome.ModernMessaging
        "ConversationFlow" -> InCallPhoneChrome.ConversationFlow
        "MicroMotion" -> InCallPhoneChrome.MicroMotion
        "TealTide" -> InCallPhoneChrome.TealTide
        "IndigoLine" -> InCallPhoneChrome.IndigoLine
        "SkyPanel" -> InCallPhoneChrome.SkyPanel
        "VioletStudio" -> InCallPhoneChrome.VioletStudio
        "Ios" -> InCallPhoneChrome.Ios
        "Material3" -> InCallPhoneChrome.Material3
        else -> InCallPhoneChrome.Default
    }

private enum class InCallPhoneChrome {
    Default,
    Ios,
    Samsung,
    DailyDial,
    NeoMirror,
    Dialer360,
    NothingDial,
    GlassDial,
    AiTranslator,
    SaasWidget,
    MessageApp,
    ModernMessaging,
    ConversationFlow,
    MicroMotion,
    TealTide,
    IndigoLine,
    SkyPanel,
    VioletStudio,
    Material3,
}

@Composable
private fun inCallPhoneChrome(dialpadStyle: String, answerCallStyle: String): InCallPhoneChrome {
    val theme = LocalThemePreset.current
    ThemeUiBindings.forcedCallChromeKeyOrNull(theme)?.let { return parseInCallChrome(it) }
    return when {
        answerCallStyle == PersonalizationStore.ANSWER_STYLE_GLASS -> InCallPhoneChrome.Ios
        answerCallStyle == PersonalizationStore.ANSWER_STYLE_SAMSUNG_LIQUID -> InCallPhoneChrome.Samsung
        dialpadStyle == "iOS-like" -> InCallPhoneChrome.Ios
        else -> InCallPhoneChrome.Default
    }
}

enum class InCallUiPhase {
    /** Outgoing — show "Calling…", no elapsed timer. */
    Dialing,
    /** Connected — "In call" + timer. */
    Active,
}

/**
 * Full-screen in-call UI (`phoniq-mockup-v1.html` `#screen-calling` outgoing chrome + action grid).
 */
@Composable
fun InCallScreen(
    callerName: String,
    callerNumber: String,
    deviceContactId: Long = 0L,
    phase: InCallUiPhase = InCallUiPhase.Active,
    dialpadStyle: String = "Classic",
    answerCallStyle: String = PersonalizationStore.ANSWER_STYLE_CLASSIC,
    spamRiskLabel: String? = null,
    isCallRecordingActive: Boolean = false,
    canControlCallRecording: Boolean = true,
    onToggleCallRecording: () -> Unit = {},
    onHangUp: () -> Unit,
    onUserMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val showTimer = LocalShowInCallTimer.current
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val muted by CallStateRepository.isMuted.collectAsState()
    val audioRoute by CallStateRepository.audioRoute.collectAsState()
    val speakerOn = audioRoute == CallAudioRoute.SPEAKER
    var showDialpad by remember { mutableStateOf(false) }
    val callInfo by CallStateRepository.callInfo.collectAsState()
    val holdOn = callInfo?.state == com.phoniq.app.telecom.CallState.HOLDING

    val chrome = inCallPhoneChrome(dialpadStyle, answerCallStyle)

    LaunchedEffect(phase) {
        elapsedSeconds = 0
        while (phase == InCallUiPhase.Active) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    when (chrome) {
                        InCallPhoneChrome.Ios -> IosInCallGradient
                        InCallPhoneChrome.Samsung -> SamsungInCallGradient
                        InCallPhoneChrome.DailyDial -> DailyDialInCallGradient
                        InCallPhoneChrome.NeoMirror -> NeoMirrorInCallGradient
                        InCallPhoneChrome.Dialer360 -> Dialer360InCallGradient
                        InCallPhoneChrome.NothingDial -> NothingDialInCallGradient
                        InCallPhoneChrome.GlassDial -> GlassDialInCallGradient
                        InCallPhoneChrome.AiTranslator -> AiTranslatorInCallGradient
                        InCallPhoneChrome.SaasWidget -> SaasWidgetInCallGradient
                        InCallPhoneChrome.MessageApp -> MessageAppInCallGradient
                        InCallPhoneChrome.ModernMessaging -> ModernMessagingInCallGradient
                        InCallPhoneChrome.ConversationFlow -> ConversationFlowInCallGradient
                        InCallPhoneChrome.MicroMotion -> MicroMotionInCallGradient
                        InCallPhoneChrome.TealTide -> TealTideInCallGradient
                        InCallPhoneChrome.IndigoLine -> IndigoLineInCallGradient
                        InCallPhoneChrome.SkyPanel -> SkyPanelInCallGradient
                        InCallPhoneChrome.VioletStudio -> VioletStudioInCallGradient
                        InCallPhoneChrome.Material3 -> Material3InCallGradient
                        InCallPhoneChrome.Default -> OutgoingCallGradient
                    },
                ),
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
                    CallerAvatar(name = callerName, deviceContactId = deviceContactId)
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

            if (spamRiskLabel != null) {
                Text(
                    text = spamRiskLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF8F8F),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(
                        if (phase == InCallUiPhase.Dialing) R.string.incall_status_calling
                        else R.string.incall_status_in_call,
                    ),
                    fontSize = 14.sp,
                    color = Color(0xFFAAAAAA),
                )
                StatusDotPulse()
            }

            if (phase == InCallUiPhase.Active && showTimer) {
                Text(
                    text = formatDuration(elapsedSeconds),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace,
                    color = PhoniqOnBackground,
                    modifier = Modifier.padding(top = 18.dp),
                )
            } else {
                Spacer(Modifier.height(40.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showDialpad) {
                DialpadContent(onDismiss = { showDialpad = false }, dialpadStyle = dialpadStyle)
            } else {
                when (chrome) {
                    InCallPhoneChrome.Ios -> {
                val recordWhenActiveMsg = stringResource(R.string.toast_incall_record_when_active)
                IosInCallActionGrid(
                    muted = muted,
                    speakerOn = speakerOn,
                    onToggleMute = { CallStateRepository.requestToggleMute() },
                    onToggleSpeaker = { CallStateRepository.requestToggleSpeaker() },
                    onOpenKeypad = { showDialpad = true },
                    onHold = { CallStateRepository.requestHold(!holdOn) },
                    onToggleRecord = {
                        if (!canControlCallRecording) {
                            onUserMessage(recordWhenActiveMsg)
                        } else {
                            onToggleCallRecording()
                        }
                    },
                    onSms = {
                        if (!context.startSmsCompose(callerNumber)) {
                            onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                        }
                    },
                    onVideoStub = {
                        onUserMessage(context.getString(R.string.toast_video_calls_not_supported))
                    },
                    onHangUp = onHangUp,
                )
                    }
                    InCallPhoneChrome.Samsung -> {
                        val recordWhenActiveMsg = stringResource(R.string.toast_incall_record_when_active)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                SamsungLiquidCallActionCell(
                                    icon = if (muted) Icons.Filled.MicOff else Icons.Outlined.Mic,
                                    label = stringResource(if (muted) R.string.incall_unmute else R.string.incall_mute),
                                    active = muted,
                                    onClick = { CallStateRepository.requestToggleMute() },
                                )
                                SamsungLiquidCallActionCell(
                                    icon = Icons.Default.Dialpad,
                                    label = stringResource(R.string.incall_keypad),
                                    active = false,
                                    onClick = { showDialpad = true },
                                )
                                SamsungLiquidCallActionCell(
                                    icon = if (speakerOn) Icons.Filled.SpeakerPhone else Icons.AutoMirrored.Outlined.VolumeUp,
                                    label = stringResource(R.string.incall_speaker),
                                    active = speakerOn,
                                    onClick = { CallStateRepository.requestToggleSpeaker() },
                                )
                            }
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                SamsungLiquidCallActionCell(
                                    icon = Icons.Filled.Pause,
                                    label = stringResource(R.string.incall_hold),
                                    active = holdOn,
                                    onClick = { CallStateRepository.requestHold(!holdOn) },
                                )
                                SamsungLiquidCallActionCell(
                                    icon = Icons.Filled.FiberManualRecord,
                                    label = stringResource(R.string.incall_record),
                                    active = isCallRecordingActive,
                                    contentDimmed = !canControlCallRecording,
                                    onClick = {
                                        if (!canControlCallRecording) {
                                            onUserMessage(recordWhenActiveMsg)
                                        } else {
                                            onToggleCallRecording()
                                        }
                                    },
                                )
                                SamsungLiquidCallActionCell(
                                    icon = Icons.AutoMirrored.Filled.Message,
                                    label = stringResource(R.string.incall_sms),
                                    active = false,
                                    onClick = {
                                        if (!context.startSmsCompose(callerNumber)) {
                                            onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                                        }
                                    },
                                )
                            }
                        }
                    }
                    InCallPhoneChrome.Default,
                    InCallPhoneChrome.DailyDial,
                    InCallPhoneChrome.NeoMirror,
                    InCallPhoneChrome.Dialer360,
                    InCallPhoneChrome.NothingDial,
                    InCallPhoneChrome.GlassDial,
                    InCallPhoneChrome.AiTranslator,
                    InCallPhoneChrome.SaasWidget,
                    InCallPhoneChrome.MessageApp,
                    InCallPhoneChrome.ModernMessaging,
                    InCallPhoneChrome.ConversationFlow,
                    InCallPhoneChrome.MicroMotion,
                    InCallPhoneChrome.TealTide,
                    InCallPhoneChrome.IndigoLine,
                    InCallPhoneChrome.SkyPanel,
                    InCallPhoneChrome.VioletStudio,
                    InCallPhoneChrome.Material3,
                    -> {
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
                            onClick = { CallStateRepository.requestToggleMute() },
                        )
                        CallActionCell(
                            icon = Icons.Default.Dialpad,
                            label = stringResource(R.string.incall_keypad),
                            active = false,
                            onClick = { showDialpad = true },
                        )
                        CallActionCell(
                            icon = if (speakerOn) Icons.Filled.SpeakerPhone else Icons.AutoMirrored.Outlined.VolumeUp,
                            label = stringResource(R.string.incall_speaker),
                            active = speakerOn,
                            onClick = { CallStateRepository.requestToggleSpeaker() },
                        )
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val recordWhenActiveMsg = stringResource(R.string.toast_incall_record_when_active)
                        CallActionCell(
                            icon = Icons.Filled.Pause,
                            label = stringResource(R.string.incall_hold),
                            active = holdOn,
                            onClick = { CallStateRepository.requestHold(!holdOn) },
                        )
                        CallActionCell(
                            icon = Icons.Filled.FiberManualRecord,
                            label = stringResource(R.string.incall_record),
                            active = isCallRecordingActive,
                            contentDimmed = !canControlCallRecording,
                            onClick = {
                                if (!canControlCallRecording) {
                                    onUserMessage(recordWhenActiveMsg)
                                } else {
                                    onToggleCallRecording()
                                }
                            },
                        )
                        CallActionCell(
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = stringResource(R.string.incall_sms),
                            active = false,
                            onClick = {
                                if (!context.startSmsCompose(callerNumber)) {
                                    onUserMessage(context.getString(R.string.snackbar_no_sms_app))
                                }
                            },
                        )
                    }
                }
                }
                }
            }

            if (chrome != InCallPhoneChrome.Ios || showDialpad) {
                Spacer(Modifier.height(20.dp))

                val endShape =
                    if (
                        chrome == InCallPhoneChrome.Samsung ||
                        chrome == InCallPhoneChrome.DailyDial ||
                        chrome == InCallPhoneChrome.NeoMirror ||
                        chrome == InCallPhoneChrome.Dialer360 ||
                        chrome == InCallPhoneChrome.NothingDial ||
                        chrome == InCallPhoneChrome.GlassDial ||
                        chrome == InCallPhoneChrome.AiTranslator ||
                        chrome == InCallPhoneChrome.SaasWidget ||
                        chrome == InCallPhoneChrome.MessageApp ||
                        chrome == InCallPhoneChrome.ModernMessaging ||
                        chrome == InCallPhoneChrome.ConversationFlow ||
                        chrome == InCallPhoneChrome.MicroMotion ||
                        chrome == InCallPhoneChrome.TealTide ||
                        chrome == InCallPhoneChrome.IndigoLine ||
                        chrome == InCallPhoneChrome.SkyPanel ||
                        chrome == InCallPhoneChrome.VioletStudio ||
                        chrome == InCallPhoneChrome.Material3
                    ) {
                        RoundedCornerShape(22.dp)
                    } else {
                        CircleShape
                    }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(66.dp)
                            .clip(endShape)
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
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun IosInCallActionGrid(
    muted: Boolean,
    speakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenKeypad: () -> Unit,
    onHold: () -> Unit,
    onToggleRecord: () -> Unit,
    onSms: () -> Unit,
    onVideoStub: () -> Unit,
    onHangUp: () -> Unit,
) {
    var moreMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IosCallGlassButton(
                icon = if (speakerOn) Icons.Filled.SpeakerPhone else Icons.AutoMirrored.Outlined.VolumeUp,
                label = stringResource(R.string.incall_speaker),
                emphasized = speakerOn,
                onClick = onToggleSpeaker,
            )
            IosCallGlassButton(
                icon = Icons.Filled.Videocam,
                label = stringResource(R.string.incall_video),
                emphasized = false,
                onClick = onVideoStub,
            )
            IosCallGlassButton(
                icon = if (muted) Icons.Filled.MicOff else Icons.Outlined.Mic,
                label = stringResource(if (muted) R.string.incall_unmute else R.string.incall_mute),
                emphasized = muted,
                onClick = onToggleMute,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IosCallGlassButton(
                    icon = Icons.Filled.MoreHoriz,
                    label = stringResource(R.string.incall_more),
                    emphasized = false,
                    onClick = { moreMenu = true },
                )
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.incall_hold)) },
                        onClick = {
                            moreMenu = false
                            onHold()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.incall_record)) },
                        onClick = {
                            moreMenu = false
                            onToggleRecord()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.incall_sms)) },
                        onClick = {
                            moreMenu = false
                            onSms()
                        },
                    )
                }
            }
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .clickable(
                                indication = ripple(),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onHangUp,
                            ),
                ) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = stringResource(R.string.cd_incall_end),
                        tint = Color.White,
                        modifier = Modifier.size(30.dp).rotate(135f),
                    )
                }
            }
            IosCallGlassButton(
                icon = Icons.Filled.Dialpad,
                label = stringResource(R.string.incall_keypad),
                emphasized = false,
                onClick = onOpenKeypad,
            )
        }
    }
}

@Composable
private fun IosCallGlassButton(
    icon: ImageVector,
    label: String,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .size(width = 78.dp, height = 92.dp)
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
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (emphasized) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.18f),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.82f),
        )
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
                        .border(1.5.dp, PhoniqAccent.copy(alpha = 0.28f), contactAvatarShapeForSize(150.dp)),
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
internal fun CallerAvatar(name: String, deviceContactId: Long = 0L) {
    val ringShape = contactAvatarShapeForSize(100.dp)
    val initials =
        name.trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
    val letter = initials.ifEmpty { "?" }
    if (deviceContactId > 0L) {
        val hue = name.trim().ifEmpty { "?" }.hashCode().toLong() and 0x00FFFFFFL
        val g0 = Color(0xFF000000L or hue)
        val g1 = Color(0xFF000000L or (hue xor 0x00333333L))
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(100.dp)
                    .border(3.dp, PhoniqAccent.copy(alpha = 0.45f), ringShape),
        ) {
            ContactPhotoAvatar(
                deviceContactId = deviceContactId,
                initials = letter,
                gradientStart = g0,
                gradientEnd = g1,
                size = 94.dp,
                fontSize = 36.sp,
            )
        }
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(100.dp)
                    .border(3.dp, PhoniqAccent.copy(alpha = 0.45f), ringShape)
                    .contactAvatarClip(100.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8C5FE8), Color(0xFF6C63FF)),
                        ),
                    ),
        ) {
            AvatarInitialsText(
                text = letter,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SamsungLiquidCallActionCell(
    icon: ImageVector,
    label: String,
    active: Boolean,
    contentDimmed: Boolean = false,
    onClick: () -> Unit,
) {
    val brush =
        if (active) {
            Brush.verticalGradient(
                listOf(Color(0xFF7A8FB0), Color(0xFF4A5A78), Color(0xFF343F56)),
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFF5F6E88), Color(0xFF3D4A5E), Color(0xFF2A3444)),
            )
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .size(width = 72.dp, height = 76.dp)
                .alpha(if (contentDimmed) 0.45f else 1f)
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
                    .background(brush)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
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
            color = Color(0xFFCCD4E0),
        )
    }
}

@Composable
private fun CallActionCell(
    icon: ImageVector,
    label: String,
    active: Boolean,
    contentDimmed: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .size(width = 72.dp, height = 76.dp)
                .alpha(if (contentDimmed) 0.45f else 1f)
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
