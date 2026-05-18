package com.phoniq.app.domain.reminders

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * REM-8 subset: best-effort UPI deep link. User picks their payment app.
 */
fun buildUpiPayIntent(pa: String, payeeNote: String = "PhonIQ"): Intent {
    val encPa = Uri.encode(pa)
    val encNote = Uri.encode(payeeNote)
    return Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("upi://pay?pa=$encPa&pn=$encNote&am=&cu=INR")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

fun Context.tryStartUpiPay(pa: String): Boolean {
    val intent = buildUpiPayIntent(pa)
    return runCatching {
        startActivity(intent)
        true
    }.getOrDefault(false)
}
