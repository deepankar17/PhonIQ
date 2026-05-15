package com.phoniq.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds one-shot launch targets from [MainActivity] intents so Compose can navigate
 * without recreating the activity graph.
 */
object PhonIQLaunchRouter {
    private val _pendingSmsAddress = MutableStateFlow<String?>(null)
    val pendingSmsAddress: StateFlow<String?> = _pendingSmsAddress.asStateFlow()

    /**
     * Sanitized digits for the in-app dialpad when [Intent.ACTION_DIAL] (or `tel:` VIEW) is delivered
     * to PhonIQ as the default dialer — never rely on implicit DIAL from in-app; it resolves to itself.
     */
    private val _pendingDialDigits = MutableStateFlow<String?>(null)
    val pendingDialDigits: StateFlow<String?> = _pendingDialDigits.asStateFlow()

    /** New message with no recipient — never use implicit SENDTO from in-app; it resolves to PhonIQ itself. */
    private val _pendingBlankSmsCompose = MutableStateFlow(false)
    val pendingBlankSmsCompose: StateFlow<Boolean> = _pendingBlankSmsCompose.asStateFlow()

    private val _pendingMainTabRoute = MutableStateFlow<String?>(null)
    val pendingMainTabRoute: StateFlow<String?> = _pendingMainTabRoute.asStateFlow()

    fun offerSmsComposeDestination(raw: String?) {
        val a = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return
        _pendingSmsAddress.value = a
    }

    fun offerBlankSmsCompose() {
        _pendingBlankSmsCompose.value = true
    }

    fun offerDialDigits(sanitized: String) {
        val d = sanitized.trim()
        if (d.isNotEmpty() && d != "+") {
            _pendingDialDigits.value = d
        }
    }

    fun consumeSmsAddress() {
        _pendingSmsAddress.value = null
    }

    fun consumeDialDigits() {
        _pendingDialDigits.value = null
    }

    fun consumeBlankSmsCompose() {
        _pendingBlankSmsCompose.value = false
    }

    fun offerMainTabRoute(route: String) {
        val r = route.trim().lowercase()
        if (r in setOf("phone", "messages", "money")) {
            _pendingMainTabRoute.value = r
        }
    }

    fun consumeMainTabRoute() {
        _pendingMainTabRoute.value = null
    }
}
