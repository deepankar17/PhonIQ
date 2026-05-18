package com.phoniq.app.data.model

/**
 * Monthly aggregates shown on rich transaction SMS notifications (from local Room data).
 */
data class TxnNotificationMoneyInsights(
    val monthSpendTotal: Double,
    /** Localized month name (e.g. "May"). */
    val monthDisplayName: String,
    val monthYear: Int,
    /** Sum of budget limits for the month, or null when no budgets. */
    val budgetTotal: Double?,
    /** Remaining budget spread over days left in month; null when not applicable. */
    val safeToSpendPerDay: Double?,
)
