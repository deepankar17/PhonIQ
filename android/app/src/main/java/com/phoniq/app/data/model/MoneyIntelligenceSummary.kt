package com.phoniq.app.data.model

import com.phoniq.app.data.db.entity.TransactionEntity

/** Lightweight on-device heuristics from parsed SMS transactions (Money tools / IQ strip). */
data class MoneyIntelligenceSummary(
    val recurringMerchantCount: Int,
    val salaryCreditCount: Int,
    val investmentTxnCount: Int,
    val hasAny: Boolean,
) {
    companion object {
        val Empty = MoneyIntelligenceSummary(0, 0, 0, false)
    }
}

private val investmentHints =
    listOf(
        "SIP",
        "MF",
        "MUTUAL",
        "NSDL",
        "CDSL",
        "ZERODHA",
        "GROWW",
        "HDFC MF",
        "ICICI PRU",
        "CAMS",
        "KFIN",
    )

private fun merchantLooksLikeInvestment(merchant: String?): Boolean {
    if (merchant.isNullOrBlank()) return false
    val m = merchant.uppercase()
    return investmentHints.any { hint -> m.contains(hint.uppercase()) }
}

private fun merchantLooksLikeSalaryCredit(merchant: String?): Boolean {
    if (merchant.isNullOrBlank()) return false
    val m = merchant.uppercase()
    return m.contains("SALARY") || m.contains("PAYROLL") || m.contains("WAGES") ||
        (m.contains("NEFT") && m.contains("CREDIT"))
}

fun buildMoneyIntelligence(transactions: List<TransactionEntity>): MoneyIntelligenceSummary {
    if (transactions.isEmpty()) return MoneyIntelligenceSummary.Empty
    val debits = transactions.filter { it.txnType == "DEBIT" }
    val byMerchant =
        debits
            .groupingBy { it.merchant?.trim()?.lowercase().orEmpty().ifBlank { "__" } }
            .eachCount()
    val recurringMerchantCount = byMerchant.count { (k, n) -> k != "__" && n >= 2 }
    val salaryCreditCount =
        transactions.count {
            it.txnType == "CREDIT" && merchantLooksLikeSalaryCredit(it.merchant)
        }
    val investmentTxnCount =
        transactions.count {
            it.category == "INVESTMENT" || merchantLooksLikeInvestment(it.merchant)
        }
    val hasAny = recurringMerchantCount > 0 || salaryCreditCount > 0 || investmentTxnCount > 0
    return MoneyIntelligenceSummary(
        recurringMerchantCount = recurringMerchantCount,
        salaryCreditCount = salaryCreditCount,
        investmentTxnCount = investmentTxnCount,
        hasAny = hasAny,
    )
}
