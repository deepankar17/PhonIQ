package com.phoniq.app.ui.money

import com.phoniq.app.data.db.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val dayMs: Long = 86_400_000L

data class MoneyReminderLine(
    val title: String,
    val subtitle: String,
    val sortKey: Long = 0L,
)

data class SalaryFySummary(
    val fyLabel: String,
    val totalRupee: Int,
    val creditCount: Int,
)

data class UpcomingBillHint(val threadId: String, val title: String, val hint: String)

fun buildMoneyReminderLines(transactions: List<TransactionEntity>): List<MoneyReminderLine> {
    if (transactions.size < 4) return emptyList()
    val debits = transactions.filter { it.txnType == "DEBIT" && !it.merchant.isNullOrBlank() }
    val byMerchant = debits.groupBy { it.merchant!!.trim().lowercase(Locale.getDefault()) }
        .filter { it.value.size >= 2 }
    val now = System.currentTimeMillis()
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
    val out = mutableListOf<MoneyReminderLine>()
    for ((_, rows) in byMerchant) {
        val sorted = rows.sortedBy { it.date }
        val gaps = sorted.zipWithNext().map { (a, b) -> b.date - a.date }.filter { it > dayMs }
        if (gaps.isEmpty()) continue
        val medianGap = gaps.sorted()[gaps.size / 2]
        if (medianGap !in 18L * dayMs..45L * dayMs) continue
        val last = sorted.last().date
        val next = last + medianGap
        if (next in now..(now + 40L * dayMs)) {
            val days = ((next - now) / dayMs).coerceAtLeast(0)
            out.add(
                MoneyReminderLine(
                    title = sorted.last().merchant?.trim().orEmpty().ifBlank { "Recurring" },
                    subtitle = "Heuristic · ~due in ${days}d · ${fmt.format(Date(next))}",
                    sortKey = next,
                ),
            )
        }
    }

    val transferDebits =
        debits.filter { it.merchant?.contains("transfer", ignoreCase = true) == true }
            .sortedByDescending { it.date }
    if (transferDebits.size >= 2) {
        val last = transferDebits.first()
        val prev = transferDebits[1]
        val gap = last.date - prev.date
        if (gap in 25L * dayMs..40L * dayMs) {
            val next = last.date + gap
            if (next in now..(now + 35L * dayMs)) {
                val days = ((next - now) / dayMs).coerceAtLeast(0)
                out.add(
                    MoneyReminderLine(
                        title = last.merchant?.trim().orEmpty().ifBlank { "Transfer pattern" },
                        subtitle = "Similar gap to last transfers · ~${days}d",
                        sortKey = next,
                    ),
                )
            }
        }
    }
    return out.sortedBy { it.sortKey }.distinctBy { it.title to it.subtitle }.take(10)
}

fun buildSalaryFySummary(
    transactions: List<TransactionEntity>,
    zone: ZoneId = ZoneId.systemDefault(),
): SalaryFySummary? {
    if (transactions.isEmpty()) return null
    val now = LocalDate.now(zone)
    val fyStartYear = if (now.monthValue >= 4) now.year else now.year - 1
    val fyStart = LocalDate.of(fyStartYear, 4, 1)
    val fyEnd = LocalDate.of(fyStartYear + 1, 3, 31)
    val salaryTxns =
        transactions.filter { it.txnType == "CREDIT" }.filter { txn ->
            val isSalary =
                txn.category == "SALARY" ||
                    txn.merchant?.contains("salary", ignoreCase = true) == true ||
                    txn.merchant?.contains("payroll", ignoreCase = true) == true
            if (!isSalary) return@filter false
            val d = Instant.ofEpochMilli(txn.date).atZone(zone).toLocalDate()
            !d.isBefore(fyStart) && !d.isAfter(fyEnd)
        }
    if (salaryTxns.isEmpty()) return null
    val total = salaryTxns.sumOf { it.amount }.roundToInt()
    val label = "FY ${"%02d".format(fyStartYear % 100)}-${"%02d".format((fyStartYear + 1) % 100)}"
    return SalaryFySummary(
        fyLabel = label,
        totalRupee = total,
        creditCount = salaryTxns.size,
    )
}
