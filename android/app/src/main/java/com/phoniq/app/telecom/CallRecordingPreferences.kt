package com.phoniq.app.telecom

import android.content.Context

/** Persisted opt-in for automatic call recording (read by [PhonIQInCallService]). Default is off. */
object CallRecordingPreferences {
    private const val PREFS = "phoniq_telecom"
    private const val KEY_RECORDING = "call_recording_enabled"

    fun isEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_RECORDING, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RECORDING, enabled)
            .apply()
    }
}
