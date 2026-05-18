package com.phoniq.app.domain.reminders

import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.MessageThreadCategory
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * REM-4: On-device bill / due-date extraction from SMS thread snippets for reminder import.
 * Conservative: only proposes rows when a calendar date is found near bill keywords.
 */
object BillDueReminderExtractor {

    private val monthNames =
        mapOf(
            "jan" to Calendar.JANUARY,
            "feb" to Calendar.FEBRUARY,
            "mar" to Calendar.MARCH,
            "apr" to Calendar.APRIL,
            "may" to Calendar.MAY,
            "jun" to Calendar.JUNE,
            "jul" to Calendar.JULY,
            "aug" to Calendar.AUGUST,
            "sep" to Calendar.SEPTEMBER,
            "oct" to Calendar.OCTOBER,
            "nov" to Calendar.NOVEMBER,
            "dec" to Calendar.DECEMBER,
        )

    private val ddMmYy =
        Pattern.compile(
            """\b(\d{1,2})[/.-](\d{1,2})[/.-](\d{2,4})\b""",
            Pattern.CASE_INSENSITIVE,
        )

    private val ddMonYyyy =
        Pattern.compile(
            """\b(\d{1,2})[\s,.-]+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)[\s,.-]+(\d{4})\b""",
            Pattern.CASE_INSENSITIVE,
        )

    private val upiVpa = Pattern.compile("""\b([a-zA-Z0-9][a-zA-Z0-9._-]{1,240}@[a-zA-Z][a-zA-Z0-9._-]{1,62})\b""")

    private val billContext =
        listOf(
            "bill",
            "due",
            "overdue",
            "invoice",
            "payment",
            "outstanding",
            "statement",
            "minimum due",
            "total due",
            "credit card",
            "recharge",
            "utility",
            "postpaid",
            "broadband",
        )

    fun extractUpiVpa(text: String): String? {
        val m = upiVpa.matcher(text)
        return if (m.find()) m.group(1)?.trim() else null
    }

    fun extractDueAtMillis(body: String, nowMillis: Long): Long? {
        val lower = body.lowercase(Locale.ROOT)
        if (!billContext.any { it in lower }) return null

        ddMmYy.matcher(body).let { m ->
            if (m.find()) {
                parseNumericDate(m.group(1), m.group(2), m.group(3), nowMillis)?.let { return it }
            }
        }
        ddMonYyyy.matcher(body).let { m ->
            if (m.find()) {
                val day = m.group(1)?.toIntOrNull() ?: return null
                val monStr = m.group(2)?.lowercase(Locale.ROOT)?.take(3) ?: return null
                val year = m.group(3)?.toIntOrNull() ?: return null
                val month = monthNames[monStr] ?: return null
                return atEndOfLocalDay(day, month, year)
            }
        }
        return null
    }

    /** Bill due calendar day → reminder fire time (start of that day minus [remindBeforeDays]). */
    fun reminderDueMillisForBillDue(billDueMillis: Long, remindBeforeDays: Int): Long {
        val days = remindBeforeDays.coerceIn(0, 30)
        return billDueMillis - days * 86_400_000L
    }

    fun suggestTitle(thread: MessageThread): String {
        val from = thread.title.trim().ifEmpty { "Bill" }
        val line = thread.snippet.trim().lineSequence().firstOrNull().orEmpty().take(48)
        return if (line.isNotEmpty()) "$from · $line" else from
    }

    /**
     * @return candidate rows (caller dedupes vs existing reminders).
     */
    fun candidatesFromBillThreads(
        threads: Iterable<MessageThread>,
        remindBeforeDays: Int,
        nowMillis: Long,
    ): List<BillReminderCandidate> {
        val out = ArrayList<BillReminderCandidate>()
        for (t in threads) {
            if (MessageThreadCategory.Bill !in t.categories) continue
            val body = "${t.snippet} ${t.subtitleBadge.orEmpty()}"
            val billDue = extractDueAtMillis(body, nowMillis) ?: continue
            val reminderAt = reminderDueMillisForBillDue(billDue, remindBeforeDays)
            val vpa = extractUpiVpa(body)
            out.add(
                BillReminderCandidate(
                    threadId = t.id,
                    title = suggestTitle(t),
                    reminderDueAtMillis = reminderAt,
                    billDueAtMillis = billDue,
                    upiVpa = vpa,
                ),
            )
        }
        return out
    }

    private fun parseNumericDate(dd: String?, mm: String?, yy: String?, nowMillis: Long): Long? {
        val d = dd?.toIntOrNull() ?: return null
        val m = mm?.toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        var year = yy?.toIntOrNull() ?: return null
        if (year in 0..99) {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val century = cal.get(Calendar.YEAR) / 100 * 100
            year += if (year < 70) century else century - 100
        }
        return atEndOfLocalDay(d, m - 1, year)
    }

    private fun atEndOfLocalDay(day: Int, monthZeroBased: Int, year: Int): Long {
        val cal =
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroBased)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 21)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return cal.timeInMillis
    }
}

data class BillReminderCandidate(
    val threadId: String,
    val title: String,
    val reminderDueAtMillis: Long,
    val billDueAtMillis: Long,
    val upiVpa: String?,
)
