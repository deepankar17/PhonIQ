package com.phoniq.app.util

import android.content.Context
import androidx.compose.runtime.Immutable

/**
 * Persistent UI preferences (SharedPreferences). Wired from [com.phoniq.app.ui.settings.SettingsOverlay]
 * and consumed at the root by [com.phoniq.app.ui.theme.PhonIQTheme].
 */
object PersonalizationStore {

    private const val PREFS = "phoniq_personalization"
    private const val K_DARK = "dark_theme"
    private const val K_ACCENT = "accent_argb"
    private const val K_AMOLED = "amoled_black"
    private const val K_MATERIAL_YOU = "material_you"
    private const val K_DENSE_THREADS = "dense_threads"
    private const val K_DIALPAD_STYLE = "dialpad_style"
    private const val K_ANSWER_CALL_STYLE = "answer_call_style"
    private const val K_THEME_PRESET = "theme_preset"
    private const val K_FOLLOW_SYSTEM = "follow_system_theme"
    private const val K_FONT_FAMILY = "font_family"
    private const val K_FONT_SIZE_TIER = "font_size_tier"
    private const val K_HAPTICS = "haptics_enabled"
    private const val K_SHOW_CALL_TIMER = "show_call_timer"
    private const val K_VERIFIED_BADGE = "verified_caller_badge"
    private const val K_OTP_AUTO_COPY = "otp_auto_copy"
    private const val K_RCS_UI = "rcs_ui_enabled"
    private const val K_OVER_BUDGET_ALERTS = "over_budget_alerts"
    private const val K_APP_LOCK = "app_lock_enabled"
    private const val K_BLUR_MONEY = "blur_money_amounts"
    private const val K_STEALTH = "stealth_mode"
    private const val K_CONTACT_AVATAR_STYLE = "contact_avatar_style"

    const val DEFAULT_ACCENT_ARGB = 0xFF6C63FFL

    /** Incoming / in-call answer controls (pill row, glass slide, Samsung liquid). */
    const val ANSWER_STYLE_CLASSIC = "Classic"
    /** Frosted / liquid glass + slide-to-answer (handset-style). */
    const val ANSWER_STYLE_GLASS = "Glass"
    /** Samsung One UI–style squircle action cells. */
    const val ANSWER_STYLE_SAMSUNG_LIQUID = "Samsung liquid"

    /** Contact header / list photo mask. */
    const val CONTACT_AVATAR_ROUND = "Round"
    const val CONTACT_AVATAR_SQUARE = "Square"
    const val CONTACT_AVATAR_SQUIRCLE = "Squircle"
    const val CONTACT_AVATAR_STAR = "Star"
    const val CONTACT_AVATAR_TEARDROP = "Teardrop"
    /** Hexagon outline (extra style). */
    const val CONTACT_AVATAR_EXTRA = "Extra"

    /** Theme chip keys — must match [com.phoniq.app.ui.settings.SettingsOverlay] theme tiles. */
    const val THEME_AMOLED = "AMOLED"
    const val THEME_DEEP_DARK = "Deep Dark"
    const val THEME_DARK_NAVY = "Dark Navy"
    const val THEME_FOREST = "Forest"
    const val THEME_WINE = "Wine"
    const val THEME_LIGHT = "Light"
    const val THEME_SAMSUNG = "Samsung"
    /** Dark violet “Daily UI” dialpad aesthetic. See theme tile + dialpad styling. */
    const val THEME_DAILY_UI_DIAL = "Daily Dial"
    /** Futuristic / “Black Mirror” dialer: obsidian + neon cyan chrome + dialpad styling. */
    const val THEME_NEO_MIRROR = "Neo Mirror"
    /** Legal / CRM dialer aesthetic (Dialer 360–style): navy chrome + professional blue accents. */
    const val THEME_DIALER_360 = "Dialer 360"
    /** Nothing-inspired dialer: true black, minimal tiles, red accents. */
    const val THEME_NOTHING_DIAL = "Nothing Dial"
    /** Frosted “glass” dialpad (Google Dialer–inspired): soft slate + pill keys + green call. */
    const val THEME_GLASS_DIAL = "Glass Dial"
    /** AI voice / translator app aesthetic: indigo depth + teal accents. */
    const val THEME_AI_TRANSLATOR = "AI Translator"
    /** SaaS dashboard / call-widget aesthetic: cool slate “cards” + blue accents. */
    const val THEME_SAAS_WIDGET = "SaaS Widget"
    /** Warm plum, violet, and rose chat chrome. */
    const val THEME_MESSAGE_APP = "Plum Inbox"
    /** Teal–mint on dark slate chat chrome. */
    const val THEME_MODERN_MESSAGING = "Carbon Thread"
    /** Blue-gray thread surfaces with soft blue accents. */
    const val THEME_CONVERSATION_FLOW = "Blue Thread"
    /** Dusk violet surfaces with coral highlights. */
    const val THEME_MICRO_MOTION = "Coral Dusk"
    /** Deep teal–navy shell with bright teal accents. */
    const val THEME_TEAL_TIDE = "Teal Tide"
    /** Cool indigo–violet depth with soft indigo accents. */
    const val THEME_INDIGO_LINE = "Indigo Line"
    /** Neutral elevated dark with sky accent. */
    const val THEME_SKY_PANEL = "Sky Panel"
    /** Deep purple with fuchsia–pink highlights. */
    const val THEME_VIOLET_STUDIO = "Violet Studio"
    /** True black + grouped gray surfaces, handset / iOS-like dark chrome. */
    const val THEME_PHONE_STYLE = "Phone Style"
    /** Material Design 3 baseline dark — tonal surfaces, expressive corners. */
    const val THEME_MATERIAL3 = "Material 3"

    @Immutable
    data class Snapshot(
        val darkTheme: Boolean = true,
        val accentArgb: Long = DEFAULT_ACCENT_ARGB,
        val amoledBlack: Boolean = false,
        val materialYou: Boolean = false,
        val denseThreads: Boolean = true,
        val dialpadStyle: String = "Classic",
        /** Answer / decline layout on incoming & in-call (when theme does not bundle call UI). */
        val answerCallStyle: String = ANSWER_STYLE_CLASSIC,
        /** Current theme preset tile (Navy / Forest / Wine / …). Drives tinted dark backgrounds when not Material You. */
        val themePreset: String = THEME_DEEP_DARK,
        /** When true, light/dark follows [android.content.res.Configuration] (system night mode). */
        val followSystemTheme: Boolean = false,
        val fontFamily: String = "Roboto",
        val fontSizeTier: String = "Normal",
        val hapticsEnabled: Boolean = true,
        val showInCallTimer: Boolean = true,
        val verifiedCallerBadge: Boolean = true,
        val otpAutoCopy: Boolean = true,
        val rcsUiEnabled: Boolean = true,
        val overBudgetAlerts: Boolean = true,
        val appLockEnabled: Boolean = false,
        val blurMoneyAmounts: Boolean = false,
        val stealthMode: Boolean = false,
        /** Clip shape for contact photos & initials avatars across the app. */
        val contactAvatarStyle: String = CONTACT_AVATAR_ROUND,
    )

    /** Maps deprecated preset labels saved in older builds to current [THEME_*] values. */
    private fun migrateLegacyThemePreset(raw: String): String =
        when (raw) {
            "Message App" -> THEME_MESSAGE_APP
            "Modern Messaging" -> THEME_MODERN_MESSAGING
            "Conversation Flow" -> THEME_CONVERSATION_FLOW
            "Micro Motion" -> THEME_MICRO_MOTION
            "Chatme" -> THEME_TEAL_TIDE
            "Direct Messaging" -> THEME_INDIGO_LINE
            "Messaging UI" -> THEME_SKY_PANEL
            "UI Exploration" -> THEME_VIOLET_STUDIO
            else -> raw
        }

    private fun migrateAnswerCallStyle(raw: String): String =
        when (raw) {
            ANSWER_STYLE_CLASSIC,
            ANSWER_STYLE_GLASS,
            ANSWER_STYLE_SAMSUNG_LIQUID,
            -> raw
            else -> ANSWER_STYLE_CLASSIC
        }

    private fun migrateContactAvatarStyle(raw: String): String =
        when (raw) {
            CONTACT_AVATAR_ROUND,
            CONTACT_AVATAR_SQUARE,
            CONTACT_AVATAR_SQUIRCLE,
            CONTACT_AVATAR_STAR,
            CONTACT_AVATAR_TEARDROP,
            CONTACT_AVATAR_EXTRA,
            -> raw
            else -> CONTACT_AVATAR_ROUND
        }

    fun load(context: Context): Snapshot {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val darkTheme = p.getBoolean(K_DARK, true)
        val amoledBlack = p.getBoolean(K_AMOLED, false)
        val migratedPreset =
            migrateLegacyThemePreset(
                p.getString(K_THEME_PRESET, null)
                    ?: when {
                        !darkTheme -> THEME_LIGHT
                        amoledBlack -> THEME_AMOLED
                        else -> THEME_DEEP_DARK
                    },
            )
        return Snapshot(
            darkTheme = darkTheme,
            accentArgb = p.getLong(K_ACCENT, DEFAULT_ACCENT_ARGB),
            amoledBlack = amoledBlack,
            materialYou = p.getBoolean(K_MATERIAL_YOU, false),
            denseThreads = p.getBoolean(K_DENSE_THREADS, true),
            dialpadStyle = p.getString(K_DIALPAD_STYLE, "Classic") ?: "Classic",
            answerCallStyle =
                migrateAnswerCallStyle(
                    p.getString(K_ANSWER_CALL_STYLE, null) ?: ANSWER_STYLE_CLASSIC,
                ),
            themePreset = migratedPreset,
            followSystemTheme = p.getBoolean(K_FOLLOW_SYSTEM, false),
            fontFamily = p.getString(K_FONT_FAMILY, "Roboto") ?: "Roboto",
            fontSizeTier = p.getString(K_FONT_SIZE_TIER, "Normal") ?: "Normal",
            hapticsEnabled = p.getBoolean(K_HAPTICS, true),
            showInCallTimer = p.getBoolean(K_SHOW_CALL_TIMER, true),
            verifiedCallerBadge = p.getBoolean(K_VERIFIED_BADGE, true),
            otpAutoCopy = p.getBoolean(K_OTP_AUTO_COPY, true),
            rcsUiEnabled = p.getBoolean(K_RCS_UI, true),
            overBudgetAlerts = p.getBoolean(K_OVER_BUDGET_ALERTS, true),
            appLockEnabled = p.getBoolean(K_APP_LOCK, false),
            blurMoneyAmounts = p.getBoolean(K_BLUR_MONEY, false),
            stealthMode = p.getBoolean(K_STEALTH, false),
            contactAvatarStyle =
                migrateContactAvatarStyle(
                    p.getString(K_CONTACT_AVATAR_STYLE, null) ?: CONTACT_AVATAR_ROUND,
                ),
        )
    }

    fun save(context: Context, snapshot: Snapshot) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().apply {
            putBoolean(K_DARK, snapshot.darkTheme)
            putLong(K_ACCENT, snapshot.accentArgb)
            putBoolean(K_AMOLED, snapshot.amoledBlack)
            putBoolean(K_MATERIAL_YOU, snapshot.materialYou)
            putBoolean(K_DENSE_THREADS, snapshot.denseThreads)
            putString(K_DIALPAD_STYLE, snapshot.dialpadStyle)
            putString(K_ANSWER_CALL_STYLE, snapshot.answerCallStyle)
            putString(K_THEME_PRESET, snapshot.themePreset)
            putBoolean(K_FOLLOW_SYSTEM, snapshot.followSystemTheme)
            putString(K_FONT_FAMILY, snapshot.fontFamily)
            putString(K_FONT_SIZE_TIER, snapshot.fontSizeTier)
            putBoolean(K_HAPTICS, snapshot.hapticsEnabled)
            putBoolean(K_SHOW_CALL_TIMER, snapshot.showInCallTimer)
            putBoolean(K_VERIFIED_BADGE, snapshot.verifiedCallerBadge)
            putBoolean(K_OTP_AUTO_COPY, snapshot.otpAutoCopy)
            putBoolean(K_RCS_UI, snapshot.rcsUiEnabled)
            putBoolean(K_OVER_BUDGET_ALERTS, snapshot.overBudgetAlerts)
            putBoolean(K_APP_LOCK, snapshot.appLockEnabled)
            putBoolean(K_BLUR_MONEY, snapshot.blurMoneyAmounts)
            putBoolean(K_STEALTH, snapshot.stealthMode)
            putString(K_CONTACT_AVATAR_STYLE, snapshot.contactAvatarStyle)
            apply()
        }
    }

    fun update(context: Context, transform: (Snapshot) -> Snapshot) {
        save(context, transform(load(context)))
    }
}
