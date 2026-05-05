package com.phoniq.app.data.model

data class BudgetStatus(
    val category: String,
    val displayName: String,
    val emoji: String,
    val spent: Double,
    /** 0 means no limit set for this category. */
    val limit: Double,
    /** spent / limit, capped 0..1. If no limit set, 0f. */
    val fraction: Float,
    val isOverBudget: Boolean,
) {
    val spentLabel: String get() = "₹${spent.toInt()}"
    val limitLabel: String get() = if (limit > 0) "of ₹${limit.toInt()}" else "No limit set"
}
