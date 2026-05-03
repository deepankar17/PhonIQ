package com.phoniq.app.data

import com.phoniq.app.data.model.CallChannel
import com.phoniq.app.data.model.CallDirection
import com.phoniq.app.data.model.CategorySpend
import com.phoniq.app.data.model.ContactRow
import com.phoniq.app.data.model.MessageThread
import com.phoniq.app.data.model.MessageThreadCategory
import com.phoniq.app.data.model.MoneySummary
import com.phoniq.app.data.model.QuickCallEntry
import com.phoniq.app.data.model.RecentCall

object SampleData {
    val recentCalls: List<RecentCall> =
        listOf(
            RecentCall(
                id = "1",
                contactName = "HDFC Bank",
                numberOrLabel = "HDFCBK",
                timeLabel = "10:42",
                direction = CallDirection.Missed,
                channel = CallChannel.Pstn,
                missedStreak = 2,
            ),
            RecentCall(
                id = "2",
                contactName = "Priya Sharma",
                numberOrLabel = "+91 98765 43210",
                timeLabel = "Yesterday",
                direction = CallDirection.Incoming,
                channel = CallChannel.WhatsAppVoice,
            ),
            RecentCall(
                id = "3",
                contactName = "Spam Likely",
                numberOrLabel = "+1 415 555 0199",
                timeLabel = "Mon",
                direction = CallDirection.Incoming,
                channel = CallChannel.Pstn,
                isSpam = true,
                isInternational = true,
            ),
            RecentCall(
                id = "4",
                contactName = "Blocked caller",
                numberOrLabel = "Private number",
                timeLabel = "Mon",
                direction = CallDirection.Rejected,
                channel = CallChannel.Pstn,
                isBlocked = true,
            ),
            RecentCall(
                id = "5",
                contactName = "Rahul Verma",
                numberOrLabel = "+91 99887 76655",
                timeLabel = "Sun",
                direction = CallDirection.Outgoing,
                channel = CallChannel.WhatsAppVideo,
            ),
            RecentCall(
                id = "6",
                contactName = "Swiggy Delivery",
                numberOrLabel = "080-4718-xxxx",
                timeLabel = "Sun",
                direction = CallDirection.Missed,
                channel = CallChannel.Pstn,
            ),
            RecentCall(
                id = "7",
                contactName = "Mom",
                numberOrLabel = "Home",
                timeLabel = "Sat",
                direction = CallDirection.Outgoing,
                channel = CallChannel.Pstn,
            ),
        )

    val quickCalls: List<QuickCallEntry> =
        listOf(
            QuickCallEntry("q1", "Priya Sharma", "WA Video · 5x", "P", 0xFF6C63FFL),
            QuickCallEntry("q2", "Rahul Verma", "Work Phone · 3x", "R", 0xFFE87D20L),
            QuickCallEntry("q3", "Ananya Singh", "WA Audio · 3x", "A", 0xFF20A060L),
            QuickCallEntry("q4", "HDFC Bank", "Phone · 2x", "H", 0xFF1A6FD4L),
            QuickCallEntry("q5", "Mom", "Phone · 2x", "M", 0xFFE040A0L),
            QuickCallEntry("q6", "Karan Mehta", "WA Audio · 1x", "K", 0xFF4488CCL),
        )

    val contacts: List<ContactRow> =
        listOf(
            ContactRow("c1", "Priya Sharma", "Mobile · +91 98765 43210"),
            ContactRow("c2", "Rahul Verma", "Mobile · +91 99887 76655", riskNote = "Likely Spam"),
            ContactRow("c3", "HDFC Bank", "Short code · HDFCBK"),
            ContactRow("c4", "Swiggy", "Support · 080-4718-xxxx"),
        )

    val favoriteContactIds: Set<String> = setOf("c1", "c4")

    val messageThreads: List<MessageThread> =
        listOf(
            MessageThread(
                id = "m1",
                title = "Priya Sharma",
                snippet = "See you at 7 — corner table?",
                timeLabel = "16:02",
                unread = true,
                categories = setOf(MessageThreadCategory.Personal),
            ),
            MessageThread(
                id = "m2",
                title = "HDFCBK",
                snippet = "INR 2,450.00 debited at BLINKIT…",
                timeLabel = "15:40",
                unread = false,
                categories = setOf(MessageThreadCategory.Transaction),
                subtitleBadge = "Txn",
            ),
            MessageThread(
                id = "m3",
                title = "VM-VFSOTP",
                snippet = "OTP 482910 is your login code. Valid 3 min.",
                timeLabel = "15:12",
                unread = true,
                categories = setOf(MessageThreadCategory.Otp),
                subtitleBadge = "OTP",
            ),
            MessageThread(
                id = "m4",
                title = "WIN SPIN",
                snippet = "You won ₹0! Click here now!!!",
                timeLabel = "Tue",
                unread = false,
                categories = setOf(MessageThreadCategory.Spam),
            ),
        )

    val moneySummary =
        MoneySummary(
            monthLabel = "May 2026 · Total spent",
            spentLabel = "₹24,180",
            incomeLabel = "₹82,000",
            currencyHint = "Parsed from SMS on device — sample data",
            budgetProgress = 0.74f,
            budgetCaption = "of ₹25,000 budget · 26% remaining",
        )

    val categorySpends: List<CategorySpend> =
        listOf(
            CategorySpend("Food", "₹8,420", 0.35f),
            CategorySpend("Transport", "₹3,100", 0.13f),
            CategorySpend("Bills", "₹6,200", 0.26f),
            CategorySpend("Shopping", "₹4,460", 0.18f),
            CategorySpend("Other", "₹2,000", 0.08f),
        )
}
