package com.phoniq.app.ui.phone

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoniq.app.R
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.model.RecentCall
import com.phoniq.app.ui.components.AvatarInitialsText
import com.phoniq.app.ui.components.ContactPhotoAvatar
import com.phoniq.app.ui.components.contactAvatarClip
import com.phoniq.app.ui.theme.LocalHapticsEnabled
import com.phoniq.app.ui.theme.LocalThemePreset
import com.phoniq.app.ui.theme.PhoniqAccent
import com.phoniq.app.util.computeDialpadMatches
import com.phoniq.app.util.dialDigitsOnly
import com.phoniq.app.util.dialpadPhoneHighlightRangeInSubtitle
import com.phoniq.app.util.DialpadMatchRow
import com.phoniq.app.util.PersonalizationStore
import com.phoniq.app.util.ThemeUiBindings
import com.phoniq.app.util.placeOrDial
import com.phoniq.app.util.sanitizeForTelDial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val DialpadCallMaterialGreen = Color(0xFF00C853)

private fun dtmfToneForDigit(digit: String): Int =
    when (digit) {
        "0" -> ToneGenerator.TONE_DTMF_0
        "1" -> ToneGenerator.TONE_DTMF_1
        "2" -> ToneGenerator.TONE_DTMF_2
        "3" -> ToneGenerator.TONE_DTMF_3
        "4" -> ToneGenerator.TONE_DTMF_4
        "5" -> ToneGenerator.TONE_DTMF_5
        "6" -> ToneGenerator.TONE_DTMF_6
        "7" -> ToneGenerator.TONE_DTMF_7
        "8" -> ToneGenerator.TONE_DTMF_8
        "9" -> ToneGenerator.TONE_DTMF_9
        "*" -> ToneGenerator.TONE_DTMF_S
        "#" -> ToneGenerator.TONE_DTMF_P
        else -> ToneGenerator.TONE_DTMF_0
    }

private data class DialKey(val digit: String, val letters: String)

private val KNOWN_DIALPAD_STYLES = setOf("Classic", "Rounded", "Minimal", "iOS-like", "Material 3")

private fun resolvedDialpadStyle(raw: String): String = if (raw in KNOWN_DIALPAD_STYLES) raw else "Classic"

@Composable
private fun effectiveDialpadVisualStyle(dialpadStyle: String): String =
    ThemeUiBindings.effectiveDialpadVisual(LocalThemePreset.current, dialpadStyle)

private fun dialpadRowHorizontalSpacing(style: String) =
    when (style) {
        "Minimal" -> 8.dp
        "iOS-like" -> 12.dp
        "Samsung",
        "Daily Dial",
        "Neo Mirror",
        "Dialer 360",
        "Nothing Dial",
        "Glass Dial",
        "AI Translator",
        "SaaS Widget",
        PersonalizationStore.THEME_MESSAGE_APP,
        PersonalizationStore.THEME_MODERN_MESSAGING,
        PersonalizationStore.THEME_CONVERSATION_FLOW,
        PersonalizationStore.THEME_MICRO_MOTION,
        PersonalizationStore.THEME_TEAL_TIDE,
        PersonalizationStore.THEME_INDIGO_LINE,
        PersonalizationStore.THEME_SKY_PANEL,
        PersonalizationStore.THEME_VIOLET_STUDIO,
        -> 12.dp
        else -> 10.dp
    }

private fun dialpadRowVerticalSpacing(style: String) =
    when (style) {
        "Minimal" -> 6.dp
        "iOS-like",
        "Rounded",
        "Samsung",
        "Daily Dial",
        "Neo Mirror",
        "Dialer 360",
        "Nothing Dial",
        "Glass Dial",
        "AI Translator",
        "SaaS Widget",
        PersonalizationStore.THEME_MESSAGE_APP,
        PersonalizationStore.THEME_MODERN_MESSAGING,
        PersonalizationStore.THEME_CONVERSATION_FLOW,
        PersonalizationStore.THEME_MICRO_MOTION,
        PersonalizationStore.THEME_TEAL_TIDE,
        PersonalizationStore.THEME_INDIGO_LINE,
        PersonalizationStore.THEME_SKY_PANEL,
        PersonalizationStore.THEME_VIOLET_STUDIO,
        -> 10.dp
        else -> 8.dp
    }

private fun dialpadKeyHeight(style: String) =
    when (style) {
        "iOS-like" -> 56.dp
        "Samsung" -> 58.dp
        "Daily Dial" -> 56.dp
        "Neo Mirror" -> 56.dp
        "Dialer 360" -> 56.dp
        "Nothing Dial" -> 56.dp
        "Glass Dial" -> 56.dp
        "AI Translator" -> 56.dp
        "SaaS Widget" -> 54.dp
        PersonalizationStore.THEME_MESSAGE_APP -> 56.dp
        PersonalizationStore.THEME_MODERN_MESSAGING -> 56.dp
        PersonalizationStore.THEME_CONVERSATION_FLOW -> 56.dp
        PersonalizationStore.THEME_MICRO_MOTION -> 56.dp
        PersonalizationStore.THEME_TEAL_TIDE -> 56.dp
        PersonalizationStore.THEME_INDIGO_LINE -> 56.dp
        PersonalizationStore.THEME_SKY_PANEL -> 56.dp
        PersonalizationStore.THEME_VIOLET_STUDIO -> 56.dp
        "Minimal" -> 50.dp
        else -> 54.dp
    }

private fun dialKeyShape(style: String): Shape =
    when (style) {
        "Samsung" -> RoundedCornerShape(28.dp)
        "Daily Dial" -> RoundedCornerShape(26.dp)
        "Dialer 360" -> RoundedCornerShape(18.dp)
        "Nothing Dial" -> RoundedCornerShape(15.dp)
        "Glass Dial" -> RoundedCornerShape(24.dp)
        "AI Translator" -> RoundedCornerShape(20.dp)
        "SaaS Widget" -> RoundedCornerShape(14.dp)
        PersonalizationStore.THEME_MESSAGE_APP -> RoundedCornerShape(22.dp)
        PersonalizationStore.THEME_MODERN_MESSAGING -> RoundedCornerShape(16.dp)
        PersonalizationStore.THEME_CONVERSATION_FLOW -> RoundedCornerShape(22.dp)
        PersonalizationStore.THEME_MICRO_MOTION -> RoundedCornerShape(24.dp)
        PersonalizationStore.THEME_TEAL_TIDE -> RoundedCornerShape(20.dp)
        PersonalizationStore.THEME_INDIGO_LINE -> RoundedCornerShape(18.dp)
        PersonalizationStore.THEME_SKY_PANEL -> RoundedCornerShape(17.dp)
        PersonalizationStore.THEME_VIOLET_STUDIO -> RoundedCornerShape(21.dp)
        "iOS-like" -> CircleShape
        "Rounded" -> RoundedCornerShape(26.dp)
        "Minimal" -> RoundedCornerShape(10.dp)
        "Material 3" -> RoundedCornerShape(16.dp)
        else -> RoundedCornerShape(20.dp)
    }

private val DIAL_KEYS = listOf(
    DialKey("1", ""),     DialKey("2", "ABC"),  DialKey("3", "DEF"),
    DialKey("4", "GHI"),  DialKey("5", "JKL"),  DialKey("6", "MNO"),
    DialKey("7", "PQRS"), DialKey("8", "TUV"),  DialKey("9", "WXYZ"),
    DialKey("*", ""),     DialKey("0", "+"),     DialKey("#", ""),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialpadSheet(
    onDismiss: () -> Unit,
    onAddContact: (phoneNumber: String) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        DialpadContent(onDismiss = onDismiss, onAddContact = onAddContact)
    }
}

@Composable
fun DialpadContent(
    modifier: Modifier = Modifier,
    initialDigits: String = "",
    contacts: List<ContactEntity> = emptyList(),
    recentCalls: List<RecentCall> = emptyList(),
    dialpadStyle: String = "Classic",
    onDismiss: () -> Unit = {},
    onAddContact: (phoneNumber: String) -> Unit = {},
) {
    val context = LocalContext.current
    val style = effectiveDialpadVisualStyle(dialpadStyle)
    var digits by rememberSaveable { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    val tones =
        remember {
            try {
                ToneGenerator(AudioManager.STREAM_DTMF, 70)
            } catch (_: Throwable) {
                null
            }
        }
    DisposableEffect(tones) {
        onDispose { tones?.release() }
    }

    LaunchedEffect(initialDigits) {
        if (initialDigits.isNotEmpty()) {
            digits = sanitizeForTelDial(initialDigits)
        }
    }

    var debouncedDigits by remember { mutableStateOf("") }
    LaunchedEffect(digits) {
        delay(75)
        debouncedDigits = digits
    }

    val matchRows by produceState(initialValue = emptyList<DialpadMatchRow>(), debouncedDigits, contacts, recentCalls) {
        value =
            withContext(Dispatchers.Default) {
                computeDialpadMatches(contacts, recentCalls, debouncedDigits)
            }
    }

    val scrollState = rememberScrollState()
    val queryDigits = dialDigitsOnly(digits)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Number display
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (digits.isNotEmpty()) {
                    IconButton(onClick = { onAddContact(digits) }) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = stringResource(R.string.dialpad_add_contact),
                            tint = PhoniqAccent,
                        )
                    }
                }
            }
            Text(
                text = digits.ifEmpty { stringResource(R.string.dialpad_enter_number_hint) },
                style = MaterialTheme.typography.headlineLarge,
                color =
                    if (digits.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (digits.isNotEmpty()) {
                    IconButton(onClick = { digits = digits.dropLast(1) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.cd_dialpad_backspace),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (matchRows.isNotEmpty()) {
            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )
            Column(Modifier.fillMaxWidth()) {
                matchRows.forEach { row ->
                    DialpadMatchRowView(
                        row = row,
                        queryDigits = queryDigits,
                        onApplyNumber = { digits = row.telSanitized },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Key grid — 3 columns; shape/spacing follow personalization dialpad style.
        DIAL_KEYS.chunked(3).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dialpadRowHorizontalSpacing(style)),
            ) {
                rowKeys.forEach { key ->
                    val keyModifier =
                        if (style == "iOS-like") {
                            Modifier.weight(1f).aspectRatio(1f)
                        } else {
                            Modifier.weight(1f).height(dialpadKeyHeight(style))
                        }
                    DialKeyButton(
                        modifier = keyModifier,
                        visualStyle = style,
                        digit = key.digit,
                        letters = key.letters,
                        onClick = {
                            digits += key.digit
                            if (hapticsOn) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            tones?.startTone(dtmfToneForDigit(key.digit), 90)
                        },
                        onLongClick = {
                            if (hapticsOn) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            when (key.digit) {
                                "0" -> digits += "+"
                                "1" -> {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("voicemail:"))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        onDismiss()
                                    }
                                }
                                else -> { /* haptic only */ }
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(dialpadRowVerticalSpacing(style)))
        }

        Spacer(Modifier.height(10.dp))

        // Call button — green; Samsung uses liquid squircle + gradient.
        val callFabShape =
            when (style) {
                "Samsung",
                "Daily Dial",
                -> RoundedCornerShape(22.dp)
                "Neo Mirror" -> RoundedCornerShape(16.dp)
                "Dialer 360" -> RoundedCornerShape(20.dp)
                "Nothing Dial" -> RoundedCornerShape(18.dp)
                "Glass Dial" -> CircleShape
                "AI Translator" -> RoundedCornerShape(24.dp)
                "SaaS Widget" -> RoundedCornerShape(16.dp)
                PersonalizationStore.THEME_MESSAGE_APP -> RoundedCornerShape(22.dp)
                PersonalizationStore.THEME_MODERN_MESSAGING -> RoundedCornerShape(18.dp)
                PersonalizationStore.THEME_CONVERSATION_FLOW -> RoundedCornerShape(20.dp)
                PersonalizationStore.THEME_MICRO_MOTION -> RoundedCornerShape(22.dp)
                PersonalizationStore.THEME_TEAL_TIDE -> RoundedCornerShape(19.dp)
                PersonalizationStore.THEME_INDIGO_LINE -> RoundedCornerShape(18.dp)
                PersonalizationStore.THEME_SKY_PANEL -> RoundedCornerShape(17.dp)
                PersonalizationStore.THEME_VIOLET_STUDIO -> RoundedCornerShape(20.dp)
                else -> CircleShape
            }
        val callFabBg: Brush? =
            when {
                style == "Samsung" && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF3EE09A), Color(0xFF00A854)),
                    )
                style == "Samsung" ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A5568), Color(0xFF2F3847)),
                    )
                style == "Daily Dial" && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF2EE6C9), Color(0xFF00A3C4)),
                    )
                style == "Daily Dial" ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A3F6B), Color(0xFF2D2540), Color(0xFF1A1428)),
                    )
                style == "Neo Mirror" && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF00F5FF), Color(0xFF00A8CC)),
                    )
                style == "Neo Mirror" ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A242E), Color(0xFF0C1218), Color(0xFF05080C)),
                    )
                style == "Dialer 360" && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                    )
                style == "Dialer 360" ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A)),
                    )
                style == "Nothing Dial" && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFFFF4545), Color(0xFFCC0000)),
                    )
                style == "Nothing Dial" ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A2A2A), Color(0xFF141414), Color(0xFF050505)),
                    )
                style == "Glass Dial" && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF4CD964), Color(0xFF1E8E3E)),
                    )
                style == "Glass Dial" ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF6B7287), Color(0xFF52596C), Color(0xFF3D4354)),
                    )
                style == "AI Translator" && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF34E0C0), Color(0xFF0D9488)),
                    )
                style == "AI Translator" ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF3D3558), Color(0xFF2A2548), Color(0xFF1A1530)),
                    )
                style == "SaaS Widget" && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                    )
                style == "SaaS Widget" ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF4B5563), Color(0xFF3D4754), Color(0xFF2D353F)),
                    )
                style == PersonalizationStore.THEME_MESSAGE_APP && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFFFB7185), Color(0xFFEC4899)),
                    )
                style == PersonalizationStore.THEME_MESSAGE_APP ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A4258), Color(0xFF3A3348), Color(0xFF2C2634)),
                    )
                style == PersonalizationStore.THEME_MODERN_MESSAGING && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF56D364), Color(0xFF238636)),
                    )
                style == PersonalizationStore.THEME_MODERN_MESSAGING ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF30363D), Color(0xFF21262D), Color(0xFF161B22)),
                    )
                style == PersonalizationStore.THEME_CONVERSATION_FLOW && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF5BA4F8), Color(0xFF2F80ED)),
                    )
                style == PersonalizationStore.THEME_CONVERSATION_FLOW ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A5366), Color(0xFF3A4254), Color(0xFF2A3140)),
                    )
                style == PersonalizationStore.THEME_MICRO_MOTION && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFFFF8A65), Color(0xFFFF6B9D)),
                    )
                style == PersonalizationStore.THEME_MICRO_MOTION ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF524858), Color(0xFF403848), Color(0xFF302838)),
                    )
                style == PersonalizationStore.THEME_TEAL_TIDE && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF2DD4BF), Color(0xFF0D9488)),
                    )
                style == PersonalizationStore.THEME_TEAL_TIDE ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A454E), Color(0xFF1E3840), Color(0xFF152830)),
                    )
                style == PersonalizationStore.THEME_INDIGO_LINE && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF6366F1), Color(0xFF7C3AED)),
                    )
                style == PersonalizationStore.THEME_INDIGO_LINE ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF3D3A52), Color(0xFF2E2C40), Color(0xFF222030)),
                    )
                style == PersonalizationStore.THEME_SKY_PANEL && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)),
                    )
                style == PersonalizationStore.THEME_SKY_PANEL ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF3F3F46), Color(0xFF323238), Color(0xFF28282D)),
                    )
                style == PersonalizationStore.THEME_VIOLET_STUDIO && digits.isNotEmpty() ->
                    Brush.linearGradient(
                        listOf(Color(0xFFF472B6), Color(0xFFC026D3)),
                    )
                style == PersonalizationStore.THEME_VIOLET_STUDIO ->
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A3D58), Color(0xFF3A3048), Color(0xFF2C2438)),
                    )
                else -> null
            }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(callFabShape)
                    .then(
                        if (callFabBg != null) {
                            Modifier.background(callFabBg)
                        } else {
                            Modifier.background(
                                if (digits.isNotEmpty()) {
                                    DialpadCallMaterialGreen
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                },
                            )
                        },
                    )
                    .then(
                        if (style == "Samsung") {
                            Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), callFabShape)
                        } else if (style == "Daily Dial") {
                            Modifier.border(1.dp, Color(0xFF8B7CB8).copy(alpha = 0.45f), callFabShape)
                        } else if (style == "Neo Mirror") {
                            Modifier.border(1.dp, Color(0xFF00D4E8).copy(alpha = 0.42f), callFabShape)
                        } else if (style == "Dialer 360") {
                            Modifier.border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.38f), callFabShape)
                        } else if (style == "Nothing Dial") {
                            Modifier.border(
                                1.dp,
                                Color(0xFFFF3B30).copy(alpha = if (digits.isNotEmpty()) 0.5f else 0.28f),
                                callFabShape,
                            )
                        } else if (style == "Glass Dial") {
                            Modifier.border(1.dp, Color.White.copy(alpha = if (digits.isNotEmpty()) 0.28f else 0.16f), callFabShape)
                        } else if (style == "AI Translator") {
                            Modifier.border(1.dp, Color(0xFF5EEAD4).copy(alpha = if (digits.isNotEmpty()) 0.45f else 0.28f), callFabShape)
                        } else if (style == "SaaS Widget") {
                            Modifier.border(1.dp, Color(0xFF60A5FA).copy(alpha = if (digits.isNotEmpty()) 0.42f else 0.24f), callFabShape)
                        } else if (style == PersonalizationStore.THEME_MESSAGE_APP) {
                            Modifier.border(
                                1.dp,
                                Color(0xFFF472B6).copy(alpha = if (digits.isNotEmpty()) 0.48f else 0.26f),
                                callFabShape,
                            )
                        } else if (style == PersonalizationStore.THEME_MODERN_MESSAGING) {
                            Modifier.border(
                                1.dp,
                                Color(0xFF39D98A).copy(alpha = if (digits.isNotEmpty()) 0.50f else 0.28f),
                                callFabShape,
                            )
                        } else if (style == PersonalizationStore.THEME_CONVERSATION_FLOW) {
                            Modifier.border(
                                1.dp,
                                Color(0xFF64B5FF).copy(alpha = if (digits.isNotEmpty()) 0.48f else 0.28f),
                                callFabShape,
                            )
                        } else if (style == PersonalizationStore.THEME_MICRO_MOTION) {
                            Modifier.border(
                                1.dp,
                                Color(0xFFFFAB91).copy(alpha = if (digits.isNotEmpty()) 0.52f else 0.30f),
                                callFabShape,
                            )
                        } else if (style == PersonalizationStore.THEME_TEAL_TIDE) {
                            Modifier.border(
                                1.dp,
                                Color(0xFF2DD4BF).copy(alpha = if (digits.isNotEmpty()) 0.50f else 0.30f),
                                callFabShape,
                            )
                        } else if (style == PersonalizationStore.THEME_INDIGO_LINE) {
                            Modifier.border(
                                1.dp,
                                Color(0xFF818CF8).copy(alpha = if (digits.isNotEmpty()) 0.48f else 0.28f),
                                callFabShape,
                            )
                        } else if (style == PersonalizationStore.THEME_SKY_PANEL) {
                            Modifier.border(
                                1.dp,
                                Color(0xFF38BDF8).copy(alpha = if (digits.isNotEmpty()) 0.50f else 0.28f),
                                callFabShape,
                            )
                        } else if (style == PersonalizationStore.THEME_VIOLET_STUDIO) {
                            Modifier.border(
                                1.dp,
                                Color(0xFFE879F9).copy(alpha = if (digits.isNotEmpty()) 0.52f else 0.30f),
                                callFabShape,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                    enabled = digits.isNotEmpty(),
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    val toDial = sanitizeForTelDial(digits)
                    if (toDial.isNotEmpty() && context.placeOrDial(toDial, null)) {
                        onDismiss()
                    }
                },
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = stringResource(R.string.cd_call_contact),
                tint =
                    if (digits.isNotEmpty()) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DialpadMatchRowView(
    row: DialpadMatchRow,
    queryDigits: String,
    onApplyNumber: () -> Unit,
) {
    val applyCd = stringResource(R.string.cd_dialpad_apply_match, row.title)
    val g0 = Color(row.avatarStartArgb.toInt())
    val g1 = Color(row.avatarEndArgb.toInt())
    val initial =
        row.title
            .trim()
            .split(" ")
            .mapNotNull { w -> w.firstOrNull() }
            .take(2)
            .joinToString("") { ch -> ch.uppercaseChar().toString() }
            .ifEmpty { row.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?" }

    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val subtitleAnnotated =
        remember(row.subtitle, queryDigits, subtitleColor) {
            val range = dialpadPhoneHighlightRangeInSubtitle(row.subtitle, queryDigits)
            buildAnnotatedString {
                if (range == null) {
                    append(row.subtitle)
                    return@buildAnnotatedString
                }
                val s = row.subtitle
                if (range.first > 0) {
                    withStyle(SpanStyle(color = subtitleColor)) {
                        append(s.substring(0, range.first))
                    }
                }
                withStyle(
                    SpanStyle(
                        color = PhoniqAccent,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(s.substring(range.first, range.endInclusive + 1))
                }
                if (range.endInclusive + 1 < s.length) {
                    withStyle(SpanStyle(color = subtitleColor)) {
                        append(s.substring(range.endInclusive + 1))
                    }
                }
            }
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onApplyNumber,
                )
                .semantics { contentDescription = applyCd }
                .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (row.deviceContactId > 0L) {
            ContactPhotoAvatar(
                deviceContactId = row.deviceContactId,
                initials = initial,
                gradientStart = g0,
                gradientEnd = g1,
                size = 44.dp,
                fontSize = 15.sp,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .contactAvatarClip(44.dp)
                        .background(Brush.linearGradient(listOf(g0, g1))),
                contentAlignment = Alignment.Center,
            ) {
                AvatarInitialsText(
                    text = initial,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitleAnnotated,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKeyButton(
    modifier: Modifier = Modifier,
    visualStyle: String,
    digit: String,
    letters: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = dialKeyShape(visualStyle)
    val glossyKeyGradient =
        remember(visualStyle) {
            when (visualStyle) {
                "Samsung" ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF6B788F),
                                Color(0xFF455066),
                                Color(0xFF2E3848),
                            ),
                    )
                "Daily Dial" ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF5A4F80),
                                Color(0xFF3D3458),
                                Color(0xFF241838),
                            ),
                    )
                "Neo Mirror" ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF1C2832),
                                Color(0xFF0E141C),
                                Color(0xFF040608),
                            ),
                    )
                "Dialer 360" ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF475569),
                                Color(0xFF334155),
                                Color(0xFF1E293B),
                            ),
                    )
                "Nothing Dial" ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF2E2E2E),
                                Color(0xFF1A1A1A),
                                Color(0xFF0D0D0D),
                            ),
                    )
                "Glass Dial" ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF8E96B0),
                                Color(0xFF656E8A),
                                Color(0xFF454B5E),
                            ),
                    )
                "AI Translator" ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF6B5B94),
                                Color(0xFF4A3F72),
                                Color(0xFF302850),
                            ),
                    )
                "SaaS Widget" ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF565F73),
                                Color(0xFF454E60),
                                Color(0xFF3A4252),
                            ),
                    )
                PersonalizationStore.THEME_MESSAGE_APP ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF5C5470),
                                Color(0xFF48405E),
                                Color(0xFF383244),
                            ),
                    )
                PersonalizationStore.THEME_MODERN_MESSAGING ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF3D444D),
                                Color(0xFF30363D),
                                Color(0xFF21262D),
                            ),
                    )
                PersonalizationStore.THEME_CONVERSATION_FLOW ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF565E72),
                                Color(0xFF454C5E),
                                Color(0xFF353B4A),
                            ),
                    )
                PersonalizationStore.THEME_MICRO_MOTION ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF6B6078),
                                Color(0xFF524A64),
                                Color(0xFF3D3648),
                            ),
                    )
                PersonalizationStore.THEME_TEAL_TIDE ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF4A6B73),
                                Color(0xFF3A5660),
                                Color(0xFF2A454E),
                            ),
                    )
                PersonalizationStore.THEME_INDIGO_LINE ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF4B4964),
                                Color(0xFF3D3B58),
                                Color(0xFF2F2D48),
                            ),
                    )
                PersonalizationStore.THEME_SKY_PANEL ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF45454D),
                                Color(0xFF38383F),
                                Color(0xFF2C2C32),
                            ),
                    )
                PersonalizationStore.THEME_VIOLET_STUDIO ->
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF5C4D6B),
                                Color(0xFF483D5A),
                                Color(0xFF362C48),
                            ),
                    )
                else -> null
            }
        }
    val keyFillSolid =
        when (visualStyle) {
            "Material 3" -> MaterialTheme.colorScheme.secondaryContainer
            "Minimal" ->
                if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
                    Color(0xFF252525).copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)
                }
            else ->
                if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
                    Color(0xFF252525)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
        }
    val digitColor =
        when (visualStyle) {
            "Material 3" -> MaterialTheme.colorScheme.onSecondaryContainer
            "Samsung" -> Color(0xFFF2F4F8)
            "Daily Dial" -> Color(0xFFF5F0FF)
            "Neo Mirror" -> Color(0xFFE8FEFF)
            "Dialer 360" -> Color(0xFFF8FAFC)
            "Nothing Dial" -> Color(0xFFFAFAFA)
            "Glass Dial" -> Color(0xFFF5F7FC)
            "AI Translator" -> Color(0xFFF5F3FF)
            "SaaS Widget" -> Color(0xFFF8FAFC)
            PersonalizationStore.THEME_MESSAGE_APP -> Color(0xFFFEF7FF)
            PersonalizationStore.THEME_MODERN_MESSAGING -> Color(0xFFF0F6FC)
            PersonalizationStore.THEME_CONVERSATION_FLOW -> Color(0xFFF2F6FC)
            PersonalizationStore.THEME_MICRO_MOTION -> Color(0xFFFFF8F7)
            PersonalizationStore.THEME_TEAL_TIDE -> Color(0xFFF0FDFA)
            PersonalizationStore.THEME_INDIGO_LINE -> Color(0xFFF8FAFC)
            PersonalizationStore.THEME_SKY_PANEL -> Color(0xFFF8FAFC)
            PersonalizationStore.THEME_VIOLET_STUDIO -> Color(0xFFFDF4FF)
            else -> MaterialTheme.colorScheme.onSurface
        }
    val lettersColor =
        when (visualStyle) {
            "Samsung" -> Color(0xFFB8C0D0)
            "Daily Dial" -> Color(0xFFC4B8E8)
            "Neo Mirror" -> Color(0xFF5CA7B5)
            "Dialer 360" -> Color(0xFF94A3B8)
            "Nothing Dial" -> Color(0xFF8E8E93)
            "Glass Dial" -> Color(0xFFB0B8D4)
            "AI Translator" -> Color(0xFF7DD3C0)
            "SaaS Widget" -> Color(0xFFCBD5E1)
            PersonalizationStore.THEME_MESSAGE_APP -> Color(0xFFDDD6FE)
            PersonalizationStore.THEME_MODERN_MESSAGING -> Color(0xFF8B949E)
            PersonalizationStore.THEME_CONVERSATION_FLOW -> Color(0xFF8FA3BC)
            PersonalizationStore.THEME_MICRO_MOTION -> Color(0xFFD4B8E8)
            PersonalizationStore.THEME_TEAL_TIDE -> Color(0xFF7EC9C3)
            PersonalizationStore.THEME_INDIGO_LINE -> Color(0xFFA5B4FC)
            PersonalizationStore.THEME_SKY_PANEL -> Color(0xFF94A3B8)
            PersonalizationStore.THEME_VIOLET_STUDIO -> Color(0xFFF0ABFC)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .border(
                    width =
                        when (visualStyle) {
                            "Samsung",
                            "Daily Dial",
                            "Neo Mirror",
                            "Dialer 360",
                            "Nothing Dial",
                            "Glass Dial",
                            "AI Translator",
                            "SaaS Widget",
                            PersonalizationStore.THEME_MESSAGE_APP,
                            PersonalizationStore.THEME_MODERN_MESSAGING,
                            PersonalizationStore.THEME_CONVERSATION_FLOW,
                            PersonalizationStore.THEME_MICRO_MOTION,
                            PersonalizationStore.THEME_TEAL_TIDE,
                            PersonalizationStore.THEME_INDIGO_LINE,
                            PersonalizationStore.THEME_SKY_PANEL,
                            PersonalizationStore.THEME_VIOLET_STUDIO,
                            -> 1.dp
                            else -> 0.dp
                        },
                    color =
                        when (visualStyle) {
                            "Samsung" -> Color.White.copy(alpha = 0.14f)
                            "Daily Dial" -> Color(0xFF7C6BA8).copy(alpha = 0.35f)
                            "Neo Mirror" -> Color(0xFF00D4E8).copy(alpha = 0.28f)
                            "Dialer 360" -> Color(0xFF60A5FA).copy(alpha = 0.28f)
                            "Nothing Dial" -> Color(0xFFFF453A).copy(alpha = 0.22f)
                            "Glass Dial" -> Color.White.copy(alpha = 0.20f)
                            "AI Translator" -> Color(0xFF2DD4BF).copy(alpha = 0.32f)
                            "SaaS Widget" -> Color(0xFF64748B).copy(alpha = 0.45f)
                            PersonalizationStore.THEME_MESSAGE_APP -> Color(0xFFC4B5FD).copy(alpha = 0.32f)
                            PersonalizationStore.THEME_MODERN_MESSAGING -> Color(0xFF39D98A).copy(alpha = 0.28f)
                            PersonalizationStore.THEME_CONVERSATION_FLOW -> Color(0xFF64B5FF).copy(alpha = 0.30f)
                            PersonalizationStore.THEME_MICRO_MOTION -> Color(0xFFFFAB91).copy(alpha = 0.34f)
                            PersonalizationStore.THEME_TEAL_TIDE -> Color(0xFF2DD4BF).copy(alpha = 0.32f)
                            PersonalizationStore.THEME_INDIGO_LINE -> Color(0xFF818CF8).copy(alpha = 0.30f)
                            PersonalizationStore.THEME_SKY_PANEL -> Color(0xFF38BDF8).copy(alpha = 0.28f)
                            PersonalizationStore.THEME_VIOLET_STUDIO -> Color(0xFFE879F9).copy(alpha = 0.32f)
                            else -> Color.Transparent
                        },
                    shape = shape,
                )
                .clip(shape)
                .then(
                    if (glossyKeyGradient != null) {
                        Modifier.background(glossyKeyGradient)
                    } else {
                        Modifier.background(keyFillSolid)
                    },
                )
                .combinedClickable(
                    indication = ripple(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Text(
                text = digit,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = digitColor,
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 10.sp,
                    letterSpacing = 0.9.sp,
                    color = lettersColor,
                    fontWeight = FontWeight.Normal,
                )
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

/** Compact read-only dialpad row for personalization settings (style preview). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonalizationDialpadStylePreview(
    dialpadStyle: String,
    modifier: Modifier = Modifier,
) {
    val style = effectiveDialpadVisualStyle(dialpadStyle)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dialpadRowHorizontalSpacing(style)),
        ) {
            val previewKeys =
                listOf(
                    DialKey("1", ""),
                    DialKey("2", "ABC"),
                    DialKey("3", "DEF"),
                )
            previewKeys.forEach { key ->
                val keyModifier =
                    if (style == "iOS-like") {
                        Modifier.weight(1f).aspectRatio(1f)
                    } else {
                        Modifier
                            .weight(1f)
                            .height((dialpadKeyHeight(style).value * 0.72f).dp)
                    }
                DialKeyButton(
                    modifier = keyModifier,
                    visualStyle = style,
                    digit = key.digit,
                    letters = key.letters,
                    onClick = {},
                    onLongClick = null,
                )
            }
        }
    }
}
