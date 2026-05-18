package com.phoniq.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.phoniq.app.data.model.ContactRow

/** [TelecomManager.EXTRA_PHONE_ACCOUNT] — use literal for toolchain compatibility. */
private const val TELECOM_EXTRA_PHONE_ACCOUNT = "android.telecom.extra.PHONE_ACCOUNT"

/**
 * Strips spaces/punctuation from [raw] so [Uri] `tel:` works reliably across devices.
 * Keeps a single leading `+`, digits, and `*` / `#` (DTMF).
 */
fun sanitizeForTelDial(raw: String): String {
    val out = StringBuilder()
    for (c in raw.trim()) {
        when {
            c.isDigit() -> out.append(c)
            c == '+' && out.isEmpty() -> out.append(c)
            c == '*' || c == '#' -> out.append(c)
        }
    }
    return out.toString()
}

/** Opens the system dialer with [rawNumber] prefilled (does not place a call). */
fun Context.startDialer(rawNumber: String): Boolean {
    val n = sanitizeForTelDial(rawNumber)
    if (n.isEmpty() || n == "+") return false
    return try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", n, null)))
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Places an outgoing PSTN call through [TelecomManager] when [Manifest.permission.CALL_PHONE] is granted.
 * When [contactForPolicy] is non-null (or when only a number is known), loads [ContactPoliciesStore] and
 * sets [TelecomManager.EXTRA_PHONE_ACCOUNT] for the preferred SIM slot when a matching account exists.
 *
 * Returns false if permission is missing or [TelecomManager.placeCall] fails — use [startDialer] or [placeOrDial].
 */
fun Context.placeOutgoingTelCall(
    rawNumber: String,
    contactForPolicy: ContactRow? = null,
): Boolean {
    val n = sanitizeForTelDial(rawNumber)
    if (n.isEmpty() || n == "+") return false
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    return try {
        val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val policy =
            if (contactForPolicy != null) {
                ContactPoliciesStore.load(this, contactForPolicy)
            } else {
                ContactPoliciesStore.loadForSanitizedNumber(this, n)
            }
        val extras = Bundle()
        val handle = resolvePhoneAccountForSimSlot(this, policy.defaultSimIndex)
        if (handle != null) {
            extras.putParcelable(TELECOM_EXTRA_PHONE_ACCOUNT, handle)
        } else {
            @SuppressLint("MissingPermission")
            val subMgr = getSystemService(SubscriptionManager::class.java)
            val subCount = subMgr?.activeSubscriptionInfoList?.size ?: 0
            if (subCount > 1) {
                Log.w("PhonIQ", "placeOutgoingTelCall: no PhoneAccount for SIM slot ${policy.defaultSimIndex}")
                Toast.makeText(
                    applicationContext,
                    applicationContext.getString(com.phoniq.app.R.string.toast_sim_routing_fallback),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        tm.placeCall(Uri.fromParts("tel", n, null), extras)
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Prefer immediate outgoing call via [placeOutgoingTelCall]; if that fails, open the system dialer
 * with the number prefilled ([startDialer]).
 */
fun Context.placeOrDial(
    rawNumber: String,
    contactForPolicy: ContactRow? = null,
): Boolean {
    if (placeOutgoingTelCall(rawNumber, contactForPolicy)) return true
    return startDialer(rawNumber)
}

@SuppressLint("MissingPermission")
private fun resolvePhoneAccountForSimSlot(
    context: Context,
    simSlotIndex: Int,
): PhoneAccountHandle? {
    if (simSlotIndex !in 0..1) return null
    val subMgr = context.getSystemService(SubscriptionManager::class.java) ?: return null
    val teleMgr = context.getSystemService(TelecomManager::class.java) ?: return null
    val infos = subMgr.activeSubscriptionInfoList ?: return null
    val info = infos.firstOrNull { it.simSlotIndex == simSlotIndex } ?: return null
    val subId = info.subscriptionId
    for (handle in teleMgr.callCapablePhoneAccounts) {
        val acc = teleMgr.getPhoneAccount(handle) ?: continue
        if (phoneAccountSubscriptionId(acc) == subId) return handle
    }
    return null
}

/** [PhoneAccount.getSubscriptionId] (API 28+); reflection fallback if stubs omit the accessor. */
private fun phoneAccountSubscriptionId(account: PhoneAccount): Int =
    runCatching {
        PhoneAccount::class.java
            .getMethod("getSubscriptionId")
            .invoke(account) as Int
    }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
