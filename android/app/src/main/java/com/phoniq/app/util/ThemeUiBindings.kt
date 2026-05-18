package com.phoniq.app.util

/**
 * Maps theme presets to bundled call / dialpad UI.
 * Base themes keep dialpad + answer controls user-configurable; branded packs use fixed chrome.
 */
object ThemeUiBindings {

    private val BASE_THEME_PRESETS =
        setOf(
            PersonalizationStore.THEME_AMOLED,
            PersonalizationStore.THEME_DEEP_DARK,
            PersonalizationStore.THEME_DARK_NAVY,
            PersonalizationStore.THEME_FOREST,
            PersonalizationStore.THEME_WINE,
            PersonalizationStore.THEME_LIGHT,
        )

    /** When true, dialpad + incoming / in-call layouts are fixed for this preset. */
    fun themeUsesDedicatedCallPack(preset: String): Boolean = preset !in BASE_THEME_PRESETS

    /**
     * Visual dialpad style consumed by [com.phoniq.app.ui.phone.DialpadSheet] / previews.
     * `null` → use stored [PersonalizationStore.Snapshot.dialpadStyle] (Classic / Rounded / …).
     */
    fun forcedDialpadVisualOrNull(preset: String): String? =
        when (preset) {
            in BASE_THEME_PRESETS -> null
            PersonalizationStore.THEME_PHONE_STYLE -> "iOS-like"
            PersonalizationStore.THEME_MATERIAL3 -> "Material 3"
            PersonalizationStore.THEME_SAMSUNG,
            PersonalizationStore.THEME_DAILY_UI_DIAL,
            PersonalizationStore.THEME_NEO_MIRROR,
            PersonalizationStore.THEME_DIALER_360,
            PersonalizationStore.THEME_NOTHING_DIAL,
            PersonalizationStore.THEME_GLASS_DIAL,
            PersonalizationStore.THEME_AI_TRANSLATOR,
            PersonalizationStore.THEME_SAAS_WIDGET,
            PersonalizationStore.THEME_MESSAGE_APP,
            PersonalizationStore.THEME_MODERN_MESSAGING,
            PersonalizationStore.THEME_CONVERSATION_FLOW,
            PersonalizationStore.THEME_MICRO_MOTION,
            PersonalizationStore.THEME_TEAL_TIDE,
            PersonalizationStore.THEME_INDIGO_LINE,
            PersonalizationStore.THEME_SKY_PANEL,
            PersonalizationStore.THEME_VIOLET_STUDIO,
            -> preset
            else -> null
        }

    fun effectiveDialpadVisual(preset: String, dialpadStyleRaw: String): String {
        forcedDialpadVisualOrNull(preset)?.let { return it }
        return if (dialpadStyleRaw in KNOWN_GENERIC_DIALPAD_STYLES) dialpadStyleRaw else "Classic"
    }

    /**
     * Incoming / in-call chrome key parsed by call UI screens.
     * `null` when [PersonalizationStore.Snapshot.answerCallStyle] should drive chrome.
     */
    fun forcedCallChromeKeyOrNull(preset: String): String? =
        when (preset) {
            in BASE_THEME_PRESETS -> null
            PersonalizationStore.THEME_SAMSUNG -> "Samsung"
            PersonalizationStore.THEME_DAILY_UI_DIAL -> "DailyDial"
            PersonalizationStore.THEME_NEO_MIRROR -> "NeoMirror"
            PersonalizationStore.THEME_DIALER_360 -> "Dialer360"
            PersonalizationStore.THEME_NOTHING_DIAL -> "NothingDial"
            PersonalizationStore.THEME_GLASS_DIAL -> "GlassDial"
            PersonalizationStore.THEME_AI_TRANSLATOR -> "AiTranslator"
            PersonalizationStore.THEME_SAAS_WIDGET -> "SaasWidget"
            PersonalizationStore.THEME_MESSAGE_APP -> "MessageApp"
            PersonalizationStore.THEME_MODERN_MESSAGING -> "ModernMessaging"
            PersonalizationStore.THEME_CONVERSATION_FLOW -> "ConversationFlow"
            PersonalizationStore.THEME_MICRO_MOTION -> "MicroMotion"
            PersonalizationStore.THEME_TEAL_TIDE -> "TealTide"
            PersonalizationStore.THEME_INDIGO_LINE -> "IndigoLine"
            PersonalizationStore.THEME_SKY_PANEL -> "SkyPanel"
            PersonalizationStore.THEME_VIOLET_STUDIO -> "VioletStudio"
            PersonalizationStore.THEME_PHONE_STYLE -> "Ios"
            PersonalizationStore.THEME_MATERIAL3 -> "Material3"
            else -> null
        }

    fun defaultDialpadStorageForTheme(preset: String): String =
        when (preset) {
            PersonalizationStore.THEME_PHONE_STYLE -> "iOS-like"
            PersonalizationStore.THEME_MATERIAL3 -> "Material 3"
            else -> "Classic"
        }

    fun defaultAnswerCallStyleForTheme(preset: String): String =
        when (preset) {
            PersonalizationStore.THEME_SAMSUNG -> PersonalizationStore.ANSWER_STYLE_SAMSUNG_LIQUID
            PersonalizationStore.THEME_PHONE_STYLE,
            PersonalizationStore.THEME_GLASS_DIAL,
            -> PersonalizationStore.ANSWER_STYLE_GLASS
            else -> PersonalizationStore.ANSWER_STYLE_CLASSIC
        }

    fun applyBundledDefaultsForTheme(snapshot: PersonalizationStore.Snapshot, newPreset: String): PersonalizationStore.Snapshot =
        snapshot.copy(
            themePreset = newPreset,
            dialpadStyle = defaultDialpadStorageForTheme(newPreset),
            answerCallStyle = defaultAnswerCallStyleForTheme(newPreset),
        )

    private val KNOWN_GENERIC_DIALPAD_STYLES =
        setOf("Classic", "Rounded", "Minimal", "iOS-like", "Material 3")
}
