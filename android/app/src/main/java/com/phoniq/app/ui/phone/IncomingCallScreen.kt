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
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import com.phoniq.app.ui.theme.LocalHapticsEnabled
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringArrayResource
import com.phoniq.app.PhonIQApp
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.vector.ImageVector
import com.phoniq.app.R
import com.phoniq.app.telecom.CallAudioRoute
import com.phoniq.app.telecom.CallStateRepository
import com.phoniq.app.ui.theme.LocalThemePreset
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.util.PersonalizationStore
import com.phoniq.app.util.ThemeUiBindings
import com.phoniq.app.util.startSmsCompose

private val IncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF060D12), Color(0xFF071A10), Color(0xFF0A0F08)),
    )

private val QuickActionFill = Color(0xFF1C1C2E)

private val IosIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF0A1628), Color(0xFF0F3550), Color(0xFF060D14)),
    )

private fun parseIncomingChrome(key: String): IncomingPhoneChrome =
    when (key) {
        "Samsung" -> IncomingPhoneChrome.Samsung
        "DailyDial" -> IncomingPhoneChrome.DailyDial
        "NeoMirror" -> IncomingPhoneChrome.NeoMirror
        "Dialer360" -> IncomingPhoneChrome.Dialer360
        "NothingDial" -> IncomingPhoneChrome.NothingDial
        "GlassDial" -> IncomingPhoneChrome.GlassDial
        "AiTranslator" -> IncomingPhoneChrome.AiTranslator
        "SaasWidget" -> IncomingPhoneChrome.SaasWidget
        "MessageApp" -> IncomingPhoneChrome.MessageApp
        "ModernMessaging" -> IncomingPhoneChrome.ModernMessaging
        "ConversationFlow" -> IncomingPhoneChrome.ConversationFlow
        "MicroMotion" -> IncomingPhoneChrome.MicroMotion
        "TealTide" -> IncomingPhoneChrome.TealTide
        "IndigoLine" -> IncomingPhoneChrome.IndigoLine
        "SkyPanel" -> IncomingPhoneChrome.SkyPanel
        "VioletStudio" -> IncomingPhoneChrome.VioletStudio
        "Ios" -> IncomingPhoneChrome.Ios
        "Material3" -> IncomingPhoneChrome.Material3
        else -> IncomingPhoneChrome.Classic
    }

private enum class IncomingPhoneChrome {
    Classic,
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
private fun incomingPhoneChrome(dialpadStyle: String, answerCallStyle: String): IncomingPhoneChrome {
    val theme = LocalThemePreset.current
    ThemeUiBindings.forcedCallChromeKeyOrNull(theme)?.let { return parseIncomingChrome(it) }
    return when (answerCallStyle) {
        PersonalizationStore.ANSWER_STYLE_GLASS -> IncomingPhoneChrome.Ios
        PersonalizationStore.ANSWER_STYLE_SAMSUNG_LIQUID -> IncomingPhoneChrome.Samsung
        else -> if (dialpadStyle == "iOS-like") IncomingPhoneChrome.Ios else IncomingPhoneChrome.Classic
    }
}

private val SamsungIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF12151C), Color(0xFF1A2433), Color(0xFF090B0F)),
    )

private val DailyDialIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF0A0812), Color(0xFF14102A), Color(0xFF06040C)),
    )

private val NeoMirrorIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF020306), Color(0xFF081820), Color(0xFF000204)),
    )

private val Dialer360IncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF0C1629), Color(0xFF1A3A5C), Color(0xFF080F1A)),
    )

private val NothingDialIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF141414), Color(0xFF000000)),
    )

private val GlassDialIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF3A3F5C), Color(0xFF4D5680), Color(0xFF2A2E42)),
    )

private val AiTranslatorIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF1E1038), Color(0xFF2D2250), Color(0xFF120A24)),
    )

private val SaasWidgetIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF0F172A)),
    )

private val MessageAppIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF2D1F35), Color(0xFF40294A), Color(0xFF1E1524)),
    )

private val ModernMessagingIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF010409)),
    )

private val ConversationFlowIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF1B1F2A), Color(0xFF282E3C), Color(0xFF141720)),
    )

private val MicroMotionIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF1C1424), Color(0xFF2A2038), Color(0xFF100C16)),
    )

private val TealTideIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF071218), Color(0xFF142E38), Color(0xFF04080C)),
    )

private val IndigoLineIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF0C0B14), Color(0xFF1E1A32), Color(0xFF06050C)),
    )

private val SkyPanelIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF101012), Color(0xFF152028), Color(0xFF08080A)),
    )

private val VioletStudioIncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF120818), Color(0xFF281838), Color(0xFF080410)),
    )

private val Material3IncomingGradient =
    Brush.linearGradient(
        colors = listOf(Color(0xFF10131A), Color(0xFF1A2438), Color(0xFF0C1018)),
    )

/**
 * Full-screen incoming call (`phoniq-mockup-v1.html` `#screen-incoming`).
 */
@Composable
fun IncomingCallScreen(
    callerName: String,
    callerNumber: String,
    deviceContactId: Long = 0L,
    dialpadStyle: String = "Classic",
    answerCallStyle: String = PersonalizationStore.ANSWER_STYLE_CLASSIC,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onUserMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val muted by CallStateRepository.isMuted.collectAsState()
    val audioRoute by CallStateRepository.audioRoute.collectAsState()
    val speakerOn = audioRoute == CallAudioRoute.SPEAKER
    val scope = rememberCoroutineScope()
    var templateStripOpen by remember { mutableStateOf(false) }

    fun openSmsToCaller() {
        if (!context.startSmsCompose(callerNumber)) {
            onUserMessage(context.getString(R.string.snackbar_no_sms_app))
        }
    }

    fun sendTemplateAndDecline(template: String) {
        val app = context.applicationContext as? PhonIQApp
        if (app == null) {
            onUserMessage(context.getString(R.string.snackbar_no_sms_app))
            return
        }
        onDecline()
        scope.launch {
            val result = app.smsRepository.sendSms(callerNumber, template)
            if (result.success) {
                onUserMessage(context.getString(R.string.incoming_template_sent))
            } else {
                onUserMessage(
                    context.getString(
                        R.string.thread_send_failed,
                        result.errorMessage ?: "unknown",
                    ),
                )
            }
        }
    }

    val chrome = incomingPhoneChrome(dialpadStyle, answerCallStyle)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    when (chrome) {
                        IncomingPhoneChrome.Ios -> IosIncomingGradient
                        IncomingPhoneChrome.Samsung -> SamsungIncomingGradient
                        IncomingPhoneChrome.DailyDial -> DailyDialIncomingGradient
                        IncomingPhoneChrome.NeoMirror -> NeoMirrorIncomingGradient
                        IncomingPhoneChrome.Dialer360 -> Dialer360IncomingGradient
                        IncomingPhoneChrome.NothingDial -> NothingDialIncomingGradient
                        IncomingPhoneChrome.GlassDial -> GlassDialIncomingGradient
                        IncomingPhoneChrome.AiTranslator -> AiTranslatorIncomingGradient
                        IncomingPhoneChrome.SaasWidget -> SaasWidgetIncomingGradient
                        IncomingPhoneChrome.MessageApp -> MessageAppIncomingGradient
                        IncomingPhoneChrome.ModernMessaging -> ModernMessagingIncomingGradient
                        IncomingPhoneChrome.ConversationFlow -> ConversationFlowIncomingGradient
                        IncomingPhoneChrome.MicroMotion -> MicroMotionIncomingGradient
                        IncomingPhoneChrome.TealTide -> TealTideIncomingGradient
                        IncomingPhoneChrome.IndigoLine -> IndigoLineIncomingGradient
                        IncomingPhoneChrome.SkyPanel -> SkyPanelIncomingGradient
                        IncomingPhoneChrome.VioletStudio -> VioletStudioIncomingGradient
                        IncomingPhoneChrome.Material3 -> Material3IncomingGradient
                        IncomingPhoneChrome.Classic -> IncomingGradient
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
            CallerAvatar(name = callerName, deviceContactId = deviceContactId)
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

            Spacer(Modifier.height(20.dp))
            when (chrome) {
                IncomingPhoneChrome.Ios -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IosIncomingGlassAction(
                        icon = Icons.AutoMirrored.Filled.Message,
                        label = stringResource(R.string.incoming_message_short),
                        onClick = { openSmsToCaller() },
                    )
                    IosIncomingGlassAction(
                        icon = Icons.Filled.Voicemail,
                        label = stringResource(R.string.incoming_voicemail_short),
                        onClick = { onUserMessage(context.getString(R.string.toast_voicemail_hint)) },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IosSlideToAnswer(
                    onAnswer = onAnswer,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.incoming_decline),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier =
                        Modifier
                            .clickable(
                                indication = ripple(color = Color.White.copy(alpha = 0.25f)),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onDecline,
                            )
                            .padding(8.dp),
                )
                }
                IncomingPhoneChrome.Samsung -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        SamsungLiquidIncomingAction(
                            icon = if (muted) Icons.Filled.MicOff else Icons.Outlined.Mic,
                            label = stringResource(if (muted) R.string.incall_unmute else R.string.incall_mute),
                            active = muted,
                            onClick = { CallStateRepository.requestToggleMute() },
                        )
                        SamsungLiquidIncomingAction(
                            icon = if (speakerOn) Icons.Filled.SpeakerPhone else Icons.AutoMirrored.Outlined.VolumeUp,
                            label = stringResource(R.string.incall_speaker),
                            active = speakerOn,
                            onClick = { CallStateRepository.requestToggleSpeaker() },
                        )
                        SamsungLiquidIncomingAction(
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
                                    onClick = { templateStripOpen = !templateStripOpen },
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
                    if (templateStripOpen) {
                        Spacer(Modifier.height(10.dp))
                        val templates = stringArrayResource(R.array.after_call_sms_templates)
                        val scrollState = rememberScrollState()
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                                    .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            templates.forEach { template ->
                                ReplyTemplateChip(
                                    text = template,
                                    onClick = { sendTemplateAndDecline(template) },
                                )
                            }
                            ReplyTemplateChip(
                                text = stringResource(R.string.incoming_reply_custom),
                                onClick = { openSmsToCaller() },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(27.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF5A6270), Color(0xFF383E4A), Color(0xFF2A2F3A)),
                                        ),
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(27.dp))
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
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(27.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF3EE09A), Color(0xFF00A854)),
                                        ),
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(27.dp))
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
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
                IncomingPhoneChrome.Classic,
                IncomingPhoneChrome.DailyDial,
                IncomingPhoneChrome.NeoMirror,
                IncomingPhoneChrome.Dialer360,
                IncomingPhoneChrome.NothingDial,
                IncomingPhoneChrome.GlassDial,
                IncomingPhoneChrome.AiTranslator,
                IncomingPhoneChrome.SaasWidget,
                IncomingPhoneChrome.MessageApp,
                IncomingPhoneChrome.ModernMessaging,
                IncomingPhoneChrome.ConversationFlow,
                IncomingPhoneChrome.MicroMotion,
                IncomingPhoneChrome.TealTide,
                IncomingPhoneChrome.IndigoLine,
                IncomingPhoneChrome.SkyPanel,
                IncomingPhoneChrome.VioletStudio,
                IncomingPhoneChrome.Material3,
                -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IncomingQuickAction(
                    icon = if (muted) Icons.Filled.MicOff else Icons.Outlined.Mic,
                    label = stringResource(if (muted) R.string.incall_unmute else R.string.incall_mute),
                    active = muted,
                    onClick = { CallStateRepository.requestToggleMute() },
                )
                IncomingQuickAction(
                    icon = if (speakerOn) Icons.Filled.SpeakerPhone else Icons.AutoMirrored.Outlined.VolumeUp,
                    label = stringResource(R.string.incall_speaker),
                    active = speakerOn,
                    onClick = { CallStateRepository.requestToggleSpeaker() },
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
                            onClick = { templateStripOpen = !templateStripOpen },
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
            if (templateStripOpen) {
                Spacer(Modifier.height(10.dp))
                val templates = stringArrayResource(R.array.after_call_sms_templates)
                val scrollState = rememberScrollState()
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    templates.forEach { template ->
                        ReplyTemplateChip(
                            text = template,
                            onClick = { sendTemplateAndDecline(template) },
                        )
                    }
                    ReplyTemplateChip(
                        text = stringResource(R.string.incoming_reply_custom),
                        onClick = { openSmsToCaller() },
                    )
                }
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
    }
}

@Composable
private fun SamsungLiquidIncomingAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
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
                .size(width = 76.dp, height = 88.dp)
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(brush)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(26.dp)),
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
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
private fun IosIncomingGlassAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .size(width = 80.dp, height = 88.dp)
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
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.34f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color(0xFF88AAFF).copy(alpha = 0.1f),
                                ),
                        ),
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.42f), CircleShape),
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun IosSlideToAnswer(
    onAnswer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    var maxPx by remember { mutableFloatStateOf(1f) }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val knobSize = 52.dp
    val trackH = 56.dp
    val horizontalPad = 6.dp
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val innerW = maxWidth - horizontalPad * 2
        LaunchedEffect(innerW) {
            maxPx = with(density) { (innerW - knobSize).toPx() }.coerceAtLeast(1f)
        }
        Box(
            modifier =
                Modifier
                    .padding(horizontal = horizontalPad)
                    .fillMaxWidth()
                    .height(trackH)
                    .clip(RoundedCornerShape(trackH / 2))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.26f),
                                Color.White.copy(alpha = 0.12f),
                                Color(0xFF88AAFF).copy(alpha = 0.08f),
                            ),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(trackH / 2))
                    .pointerInput(maxPx) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dx ->
                                offsetPx = (offsetPx + dx).coerceIn(0f, maxPx)
                            },
                            onDragEnd = {
                                if (offsetPx >= maxPx * 0.52f) {
                                    if (hapticsOn) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onAnswer()
                                }
                                offsetPx = 0f
                            },
                            onDragCancel = {
                                offsetPx = 0f
                            },
                        )
                    },
        ) {
            Text(
                text = stringResource(R.string.incoming_slide_to_answer),
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 3.dp)
                        .offset { IntOffset(offsetPx.roundToInt(), 0) }
                        .size(knobSize)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = stringResource(R.string.incoming_answer),
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
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
private fun ReplyTemplateChip(text: String, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C2E))
                .border(1.dp, Color(0xFF3A3A4F), RoundedCornerShape(20.dp))
                .clickable(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE8E8EE),
        )
    }
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
