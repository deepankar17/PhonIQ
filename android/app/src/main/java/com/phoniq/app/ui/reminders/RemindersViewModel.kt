package com.phoniq.app.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.domain.reminders.BillDueReminderExtractor
import com.phoniq.app.domain.reminders.BillReminderCandidate
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** REM-1…3 row; persisted in-memory until Room lands (structural MVP). */
data class ReminderRow(
    val id: Long,
    val title: String,
    val dueAt: Long,
    val isDone: Boolean,
    val linkedThreadId: String? = null,
    val upiVpa: String? = null,
    /** Extra line, e.g. bill due date when imported from SMS. */
    val detail: String? = null,
)

class RemindersViewModel : ViewModel() {

    private val _reminders = MutableStateFlow<List<ReminderRow>>(emptyList())
    private var nextId = 1L

    /** REM-4 default: remind this many days before the bill due date when importing. */
    private val _remindBeforeDays = MutableStateFlow(1)
    val remindBeforeDays: StateFlow<Int> = _remindBeforeDays.asStateFlow()

    val reminders: StateFlow<List<ReminderRow>> = _reminders.asStateFlow()

    fun setRemindBeforeDays(days: Int) {
        _remindBeforeDays.value = days.coerceIn(1, 14)
    }

    fun addReminder(title: String, dueAtMillis: Long, linkedThreadId: String? = null, upiVpa: String? = null, detail: String? = null) {
        val t = title.trim()
        if (t.isEmpty()) return
        val id = nextId++
        _reminders.update { list ->
            list +
                ReminderRow(
                    id = id,
                    title = t,
                    dueAt = dueAtMillis,
                    isDone = false,
                    linkedThreadId = linkedThreadId,
                    upiVpa = upiVpa?.trim()?.takeIf { it.isNotEmpty() },
                    detail = detail?.trim()?.takeIf { it.isNotEmpty() },
                )
        }
    }

    fun setDone(id: Long, done: Boolean) {
        _reminders.update { list -> list.map { if (it.id == id) it.copy(isDone = done) else it } }
    }

    /** REM-2 snooze: push due time forward. */
    fun snooze(id: Long, addMillis: Long) {
        if (addMillis <= 0L) return
        _reminders.update { list ->
            list.map { r ->
                if (r.id == id && !r.isDone) r.copy(dueAt = r.dueAt + addMillis) else r
            }
        }
    }

    /**
     * REM-4: Import one reminder per bill thread where a due date is parsed.
     * @return number of new rows added.
     */
    fun importFromBillThreads(threads: List<MessageThread>): Int {
        val before = _reminders.value.size
        val policy = _remindBeforeDays.value
        val now = System.currentTimeMillis()
        val fmt = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        val candidates = BillDueReminderExtractor.candidatesFromBillThreads(threads, policy, now)
        for (c in candidates) {
            if (isDuplicateImport(c)) continue
            val detail =
                buildString {
                    append("Bill due ")
                    append(fmt.format(c.billDueAtMillis))
                    if (policy > 0) append(" · Remind ${policy}d before")
                }
            addReminder(
                title = c.title,
                dueAtMillis = c.reminderDueAtMillis,
                linkedThreadId = c.threadId,
                upiVpa = c.upiVpa,
                detail = detail,
            )
        }
        return _reminders.value.size - before
    }

    private fun isDuplicateImport(c: BillReminderCandidate): Boolean {
        return _reminders.value.any { existing ->
            existing.linkedThreadId == c.threadId && sameCalendarDay(existing.dueAt, c.reminderDueAtMillis)
        }
    }

    private fun sameCalendarDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RemindersViewModel() as T
    }
}

enum class ReminderSection {
    Overdue,
    Upcoming,
    Done,
}

fun ReminderRow.section(now: Long): ReminderSection =
    when {
        isDone -> ReminderSection.Done
        dueAt < now -> ReminderSection.Overdue
        else -> ReminderSection.Upcoming
    }
