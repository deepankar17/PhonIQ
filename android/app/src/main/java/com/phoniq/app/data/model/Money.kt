package com.phoniq.app.data.model

data class MoneySummary(
    val monthLabel: String,
    val spentLabel: String,
    val incomeLabel: String,
    val currencyHint: String,
    /** 0f–1f spent vs monthly budget (sample). */
    val budgetProgress: Float,
    val budgetCaption: String,
)

data class CategorySpend(
    val name: String,
    val amountLabel: String,
    val fraction: Float,
)
