package com.phoniq.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MoneyNotifMode {
    DEFAULT,
    STATS,
    SPLIT,
}

data class MoneyNotifExtras(
    val mode: MoneyNotifMode = MoneyNotifMode.DEFAULT,
    val splitAmount: Double? = null,
    val splitMerchant: String? = null,
)

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

    private val _pendingOpenMessageThreadId = MutableStateFlow<String?>(null)
    val pendingOpenMessageThreadId: StateFlow<String?> = _pendingOpenMessageThreadId.asStateFlow()

    private val _pendingMoneyNotif = MutableStateFlow<MoneyNotifExtras?>(null)
    val pendingMoneyNotif: StateFlow<MoneyNotifExtras?> = _pendingMoneyNotif.asStateFlow()

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
            when (r) {
                "money" -> _pendingMoneyNotif.value = MoneyNotifExtras(MoneyNotifMode.DEFAULT)
                else -> _pendingMoneyNotif.value = null
            }
        }
    }

    /** Opens Money with structured extras from transaction notifications (Stats / Split). */
    fun offerMoneyFromTxnNotification(extras: MoneyNotifExtras) {
        _pendingMainTabRoute.value = "money"
        _pendingMoneyNotif.value = extras
    }

    fun consumeMainTabRoute() {
        _pendingMainTabRoute.value = null
    }

    fun consumeMoneyNotifExtras() {
        _pendingMoneyNotif.value = null
    }

    fun offerOpenMessageThread(threadId: String) {
        val id = threadId.trim()
        if (id.isNotEmpty()) {
            _pendingOpenMessageThreadId.value = id
        }
    }

    fun consumeOpenMessageThreadId() {
        _pendingOpenMessageThreadId.value = null
    }
}
