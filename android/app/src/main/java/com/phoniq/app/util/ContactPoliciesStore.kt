package com.phoniq.app.util

import android.content.Context
import android.content.SharedPreferences
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.data.model.effectivePhoneNumbers

/**
 * Persists per-contact routing UI from [com.phoniq.app.ui.phone.ContactPoliciesBottomSheet].
 * Keys prefer [ContactRow.deviceContactId]; otherwise normalized PSTN from the row.
 */
object ContactPoliciesStore {

    data class State(
        val ringNormally: Boolean = true,
        /** 0 = SIM 1, 1 = SIM 2 — used when placing calls via [com.phoniq.app.util.placeOutgoingTelCall]. */
        val defaultSimIndex: Int = 0,
        val customRingtoneEnabled: Boolean = false,
        /**
         * Persisted `content` / `android.resource` URI for incoming call notification sound
         * (used when [customRingtoneEnabled] and non-blank).
         */
        val customRingtoneUri: String = "",
    )

    private const val PREFS = "phoniq_contact_policies"

    private fun readState(prefs: SharedPreferences, base: String): State =
        State(
            ringNormally = prefs.getBoolean("$base.ring", true),
            defaultSimIndex = prefs.getInt("$base.sim", 0).coerceIn(0, 1),
            customRingtoneEnabled = prefs.getBoolean("$base.tone", false),
            customRingtoneUri = prefs.getString("$base.ringtone_uri", "").orEmpty(),
        )

    fun load(context: Context, contact: ContactRow): State {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return readState(prefs, keyFor(contact))
    }

    /**
     * Load policy for a raw dialed number (e.g. dialpad) using the same `n:<normalized>` key as contacts
     * without a device id.
     */
    fun loadForSanitizedNumber(context: Context, sanitizedTel: String): State {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = normalizePhoneKey(sanitizedTel)
        val base = if (k.isNotEmpty()) "n:$k" else return State()
        return readState(prefs, base)
    }

    fun save(context: Context, contact: ContactRow, state: State) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val base = keyFor(contact)
        prefs.edit().apply {
            putBoolean("$base.ring", state.ringNormally)
            putInt("$base.sim", state.defaultSimIndex.coerceIn(0, 1))
            putBoolean("$base.tone", state.customRingtoneEnabled)
            putString("$base.ringtone_uri", state.customRingtoneUri)
            apply()
        }
    }

    /**
     * Policy for an PSTN incoming call: prefers device contact id key, else normalized number.
     */
    fun loadForIncomingCall(context: Context, deviceContactId: Long, dialableNumber: String): State {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (deviceContactId > 0L) {
            return readState(prefs, "c:$deviceContactId")
        }
        val n = sanitizeForTelDial(dialableNumber)
        val k = normalizePhoneKey(n)
        val base = if (k.isNotEmpty()) "n:$k" else return State()
        return readState(prefs, base)
    }

    private fun keyFor(contact: ContactRow): String {
        if (contact.deviceContactId > 0L) {
            return "c:${contact.deviceContactId}"
        }
        val raw =
            contact.effectivePhoneNumbers().firstOrNull()
                ?: contact.detailNumber?.trim()?.takeIf { it.isNotEmpty() }
                ?: contact.subtitle.trim()
        val k = normalizePhoneKey(raw)
        return if (k.isNotEmpty()) "n:$k" else "id:${contact.id}"
    }
}
