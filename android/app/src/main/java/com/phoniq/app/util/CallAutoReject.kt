package com.phoniq.app.util

import android.content.Context
import android.os.Build
import android.provider.BlockedNumberContract

/**
 * Auto-reject incoming PSTN when the number is in the **system** blocked list (API 24+).
 *
 * Per PROJECT.md **IQ Offline Spam Engine**: spam is **warning-only**; **blocked** is enforcement.
 * User-marked spam ([SpamNumberEntity]) must not trigger auto-reject here — only
 * [BlockedNumberContract], which the user manages via system blocked numbers (see dialer “Block” CTAs).
 */
fun Context.shouldAutoRejectIncoming(dialableNumber: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
    return runCatching { BlockedNumberContract.isBlocked(this, dialableNumber) }
        .getOrDefault(false)
}
