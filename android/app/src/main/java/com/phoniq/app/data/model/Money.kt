package com.phoniq.app.data.model

data class MoneySummary(
    val monthLabel: String,
    val spentLabel: String,
    val savingsLabel: String,
    val currencyHint: String,
    val budgetProgress: Float,
    val budgetCaption: String,
)

data class CategorySpend(
    val name: String,
    val amountLabel: String,
    val fraction: Float,
    val emoji: String = "",
    val budgetLabel: String = "",
)

data class RecentTransaction(
    val merchant: String,
    val dateLine: String,
    val amountLabel: String,
    val isCredit: Boolean,
    val emoji: String,
    val categoryTag: String,
)
