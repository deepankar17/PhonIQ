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
import com.phoniq.app.data.model.RecentTransaction

object SampleData {
    /** Matches `design/phoniq-mockup-v1.html` Recent Calls block (order + scenarios). */
    val recentCalls: List<RecentCall> =
        listOf(
            RecentCall(
                id = "rc_priya",
                contactName = "Priya Sharma",
                numberOrLabel = "+91 98765 43210",
                timeLabel = "2m ago",
                direction = CallDirection.Incoming,
                channel = CallChannel.Pstn,
                metaCaption = "Incoming · 3m",
            ),
            RecentCall(
                id = "rc_hdfc",
                contactName = "HDFC Bank",
                numberOrLabel = "1800 267 6161",
                timeLabel = "1h ago",
                direction = CallDirection.Outgoing,
                channel = CallChannel.Pstn,
                metaCaption = "Outgoing · 2m",
            ),
            RecentCall(
                id = "rc_vikram",
                contactName = "Vikram Sinha",
                numberOrLabel = "+91 99001 12233",
                timeLabel = "58m ago",
                direction = CallDirection.Missed,
                channel = CallChannel.Pstn,
                missedStreak = 2,
                metaCaption = "Missed (2)",
            ),
            RecentCall(
                id = "rc_spam_num",
                contactName = "+91 78901 23456",
                numberOrLabel = "+91 78901 23456",
                timeLabel = "3h ago",
                direction = CallDirection.Missed,
                channel = CallChannel.Pstn,
                isSpam = true,
                metaCaption = "Missed",
            ),
            RecentCall(
                id = "rc_rahul",
                contactName = "Rahul Verma",
                numberOrLabel = "+91 91234 56789",
                timeLabel = "Yesterday",
                direction = CallDirection.Outgoing,
                channel = CallChannel.WhatsAppVideo,
                metaCaption = "WhatsApp · 7m",
            ),
            RecentCall(
                id = "rc_ananya",
                contactName = "Ananya Singh",
                numberOrLabel = "+91 99887 76655",
                timeLabel = "Yesterday",
                direction = CallDirection.Incoming,
                channel = CallChannel.WhatsAppVoice,
                metaCaption = "WhatsApp · 11m",
            ),
            RecentCall(
                id = "rc_mom",
                contactName = "Mom",
                numberOrLabel = "+91 98222 33445",
                timeLabel = "Yesterday · 10:42 PM",
                direction = CallDirection.Outgoing,
                channel = CallChannel.Pstn,
                metaCaption = "Outgoing · 9m",
            ),
            RecentCall(
                id = "rc_karan",
                contactName = "Karan Mehta",
                numberOrLabel = "+91 98123 44556",
                timeLabel = "Thu · 8:14 PM",
                direction = CallDirection.Incoming,
                channel = CallChannel.WhatsAppVoice,
                metaCaption = "WhatsApp · 18m",
            ),
            RecentCall(
                id = "rc_neha",
                contactName = "Neha Jain",
                numberOrLabel = "+91 88990 11223",
                timeLabel = "Thu · 6:57 PM",
                direction = CallDirection.Incoming,
                channel = CallChannel.WhatsAppVideo,
                metaCaption = "WhatsApp · 4m",
            ),
            RecentCall(
                id = "rc_icici",
                contactName = "ICICI Bank",
                numberOrLabel = "1860 120 7777",
                timeLabel = "Wed · 11:06 AM",
                direction = CallDirection.Incoming,
                channel = CallChannel.Pstn,
                metaCaption = "Incoming · 2m",
            ),
            RecentCall(
                id = "rc_intl",
                contactName = "+1 202 555 0198",
                numberOrLabel = "+1 202 555 0198",
                timeLabel = "Wed · 12:48 AM",
                direction = CallDirection.Missed,
                channel = CallChannel.Pstn,
                isInternational = true,
                metaCaption = "Missed · International",
            ),
            RecentCall(
                id = "rc_unknown",
                contactName = "+91 90000 12345",
                numberOrLabel = "+91 90000 12345",
                timeLabel = "Tue · 1m",
                direction = CallDirection.Outgoing,
                channel = CallChannel.Pstn,
                metaCaption = "Outgoing · 1m",
            ),
            RecentCall(
                id = "rc_blocked",
                contactName = "Blocked Caller",
                numberOrLabel = "+91 70000 11111",
                timeLabel = "Tue · 9:26 PM",
                direction = CallDirection.Rejected,
                channel = CallChannel.Pstn,
                isBlocked = true,
                metaCaption = "Rejected automatically (2)",
            ),
            RecentCall(
                id = "rc_scam",
                contactName = "Scam Caller",
                numberOrLabel = "+91 78901 23456",
                timeLabel = "Mon · 5 attempts",
                direction = CallDirection.Missed,
                channel = CallChannel.Pstn,
                isSpam = true,
                missedStreak = 5,
                metaCaption = "Missed (5)",
            ),
        )

    /** Matches mockup quick-call strip (same order as HTML). */
    val quickCalls: List<QuickCallEntry> =
        listOf(
            QuickCallEntry("q1", "Priya Sharma", "WA Video · 5x", "P", 0xFF6C63FFL),
            QuickCallEntry("q2", "Rahul Verma", "Work Phone · 3x", "R", 0xFFE87D20L),
            QuickCallEntry("q3", "Ananya Singh", "WA Audio · 3x", "A", 0xFF20A060L),
            QuickCallEntry("q4", "Priya Sharma", "Phone · 2x", "P", 0xFF6C63FFL),
            QuickCallEntry("q5", "HDFC Bank", "Phone · 2x", "H", 0xFF1A6FD4L),
            QuickCallEntry("q6", "Mom", "Phone · 2x", "M", 0xFFE040A0L),
            QuickCallEntry("q7", "Karan Mehta", "WA Audio · 1x", "K", 0xFF4488CCL),
            QuickCallEntry("q8", "Vikram Sinha", "Phone · 1x", "V", 0xFF607D8BL),
            QuickCallEntry("q9", "Neha Jain", "WA Video · 1x", "N", 0xFFAB47BCL),
            QuickCallEntry("q10", "Amit Rao", "Work Phone · 1x", "A", 0xFF26A69AL),
            QuickCallEntry("q11", "Ritika Sen", "Phone · 1x", "R", 0xFFFF7043L),
            QuickCallEntry("q12", "Deepak Iyer", "WA Audio · 1x", "D", 0xFF5C6BC0L),
            QuickCallEntry("q13", "Sonia Kapoor", "Phone · 1x", "S", 0xFFEC407AL),
            QuickCallEntry("q14", "Arjun Das", "WA Video · 1x", "A", 0xFF7CB342L),
            QuickCallEntry("q15", "Tanya Mehra", "Work Phone · 1x", "T", 0xFF8D6E63L),
            QuickCallEntry("q16", "Nikhil Shah", "Phone · 1x", "N", 0xFF29B6F6L),
            QuickCallEntry("q17", "Paytm Care", "Phone · 1x", "P", 0xFF00ACC1L),
            QuickCallEntry("q18", "ICICI Bank", "Phone · 1x", "I", 0xFFEF6C00L),
            QuickCallEntry("q19", "Aakash", "WA Audio · 1x", "A", 0xFF66BB6AL),
            QuickCallEntry("q20", "Rohini", "Phone · 1x", "R", 0xFF7E57C2L),
        )

    val contacts: List<ContactRow> =
        listOf(
            ContactRow("c1", "Priya Sharma", "Mobile · +91 98765 43210"),
            ContactRow("c2", "Rahul Verma", "Mobile · +91 99887 76655", riskNote = "Likely Spam"),
            ContactRow("c3", "HDFC Bank", "Short code · HDFCBK"),
            ContactRow("c4", "Swiggy", "Support · 080-4718-xxxx"),
        )

    val favoriteContactIds: Set<String> = setOf("c1", "c4")

    /** Order and richness aligned with `design/phoniq-mockup-v1.html` “All” inbox. */
    val messageThreads: List<MessageThread> =
        listOf(
            MessageThread(
                id = "m_hdfc",
                title = "HDFCBK",
                snippet = "HDFC NetBanking OTP is 482916",
                timeLabel = "10:33",
                unread = false,
                categories =
                    setOf(
                        MessageThreadCategory.Transaction,
                        MessageThreadCategory.Otp,
                    ),
                rowPills = listOf("OTP", "TXN"),
                peerAddress = "VM-HDFCBK",
                otpCode = "482916",
                otpExpiresSeconds = 180,
            ),
            MessageThread(
                id = "m1",
                title = "Priya Sharma",
                snippet = "Are you coming to the office tomorrow?",
                timeLabel = "9:15",
                unread = true,
                categories = setOf(MessageThreadCategory.Personal),
                showRcsBadge = true,
                lastCallSummary = "Yesterday · Incoming · WhatsApp voice · 4 min",
                localNote = "Close friend — dinner plans",
                peerAddress = "+91 98765 43210",
            ),
            MessageThread(
                id = "m3",
                title = "VM-VFSOTP",
                snippet = "Your OTP is 847291. Valid for 10 min",
                timeLabel = "8:47",
                unread = false,
                categories = setOf(MessageThreadCategory.Otp),
                subtitleBadge = "OTP",
                rowPills = listOf("OTP"),
                peerAddress = "VM-VFSOTP",
                otpCode = "847291",
                otpExpiresSeconds = 480,
            ),
            MessageThread(
                id = "m_sbi",
                title = "SBI",
                snippet = "₹45,000 credited to A/c XX7823. Sal…",
                timeLabel = "Yesterday",
                unread = false,
                categories = setOf(MessageThreadCategory.Transaction),
                rowPills = listOf("TXN"),
            ),
            MessageThread(
                id = "m_flip_promo",
                title = "Flipkart",
                snippet = "Big Billion Days! Upto 80% off on…",
                timeLabel = "Yesterday",
                unread = false,
                categories = setOf(MessageThreadCategory.Spam),
                rowPills = listOf("Promo"),
            ),
            MessageThread(
                id = "m_rahul",
                title = "Rahul Verma",
                snippet = "Sounds good, see you at 1",
                timeLabel = "Yesterday",
                unread = true,
                categories = setOf(MessageThreadCategory.Personal),
                showRcsBadge = true,
                listTypingHint = true,
                peerAddress = "+91 91234 56789",
            ),
            MessageThread(
                id = "m_phonepe",
                title = "PhonePe",
                snippet = "₹799 paid at Blinkit · UPI Ref 423897233",
                timeLabel = "7:12",
                unread = false,
                categories = setOf(MessageThreadCategory.Transaction),
                rowPills = listOf("TXN"),
                peerAddress = "BH-PhonePe",
            ),
            MessageThread(
                id = "m5",
                title = "BESCOM",
                snippet = "Electricity bill ₹2,160 due on 05 May",
                timeLabel = "Today",
                unread = true,
                categories = setOf(MessageThreadCategory.Bill),
                rowPills = listOf("BILL", "Due"),
            ),
            MessageThread(
                id = "m_jio",
                title = "Jio",
                snippet = "Recharge ₹239 successful · plan active 28 days",
                timeLabel = "11:12",
                unread = false,
                categories =
                    setOf(
                        MessageThreadCategory.Bill,
                        MessageThreadCategory.Delivery,
                    ),
                rowPills = listOf("BILL", "Delivery"),
            ),
            MessageThread(
                id = "m_rent",
                title = "Rent Reminder",
                snippet = "Apartment rent ₹28,000 pending since 01 May",
                timeLabel = "Yesterday",
                unread = false,
                categories = setOf(MessageThreadCategory.Bill),
                rowPills = listOf("BILL", "Overdue"),
            ),
            MessageThread(
                id = "m_amazon",
                title = "Amazon",
                snippet = "Order out for delivery · arrives by 8 PM",
                timeLabel = "10:08",
                unread = false,
                categories = setOf(MessageThreadCategory.Delivery),
                rowPills = listOf("Tracking"),
                peerAddress = "AD-AMAZON",
            ),
            MessageThread(
                id = "m_swiggy",
                title = "Swiggy",
                snippet = "Order picked up · rider 12 min away",
                timeLabel = "9:42",
                unread = false,
                categories = setOf(MessageThreadCategory.Delivery),
                rowPills = listOf("Tracking"),
                peerAddress = "VK-SWIGGY",
            ),
            MessageThread(
                id = "m_flip_log",
                title = "Flipkart Logistics",
                snippet = "Shipment reached nearest hub",
                timeLabel = "8:30",
                unread = false,
                categories = setOf(MessageThreadCategory.Delivery),
                rowPills = listOf("Tracking"),
                peerAddress = "VK-FLPKRT",
            ),
            MessageThread(
                id = "m7",
                title = "IndiGo",
                snippet = "BLR → DEL · 6E 204 · Gate closes in 35m",
                timeLabel = "7:55",
                unread = false,
                categories = setOf(MessageThreadCategory.Travel),
                rowPills = listOf("Travel"),
            ),
            MessageThread(
                id = "m_ola",
                title = "Ola",
                snippet = "Ride receipt ₹412 · Airport to Koramangala",
                timeLabel = "7:10",
                unread = false,
                categories =
                    setOf(
                        MessageThreadCategory.Travel,
                        MessageThreadCategory.Transaction,
                    ),
                rowPills = listOf("Travel"),
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
            monthLabel = "April 2026 · Total Spent",
            spentLabel = "₹18,450",
            savingsLabel = "+₹26,550",
            currencyHint = "Parsed from SMS on device — sample data",
            budgetProgress = 0.74f,
            budgetCaption = "of ₹25,000 budget · 26% remaining",
        )

    val categorySpends: List<CategorySpend> =
        listOf(
            CategorySpend("Food & Dining",     "₹5,535", 0.30f, "🍔", "of ₹8,000"),
            CategorySpend("Shopping",          "₹4,613", 0.25f, "🛍️", "of ₹5,000"),
            CategorySpend("Bills & Utilities", "₹3,690", 0.20f, "💡", "of ₹4,000"),
            CategorySpend("Transport",         "₹2,768", 0.15f, "🚗", "of ₹4,000"),
            CategorySpend("Others",            "₹1,845", 0.10f, "📦", "of ₹2,000"),
        )

    val recentTransactions: List<RecentTransaction> =
        listOf(
            RecentTransaction("Swiggy",              "Today · HDFC XX4521",        "-₹2,450",  false, "🍔", "food"),
            RecentTransaction("Salary — April 2026", "Yesterday · SBI XX7823",     "+₹45,000", true,  "💰", "salary"),
            RecentTransaction("Amazon.in",           "28 Apr · Axis XX9921 · UPI", "-₹12,999", false, "🛍️", "shopping"),
            RecentTransaction("Electricity Bill",    "27 Apr · HDFC XX4521",       "-₹1,890",  false, "💡", "bills"),
            RecentTransaction("Rapido / Ola",        "26 Apr · Paytm Wallet",      "-₹280",    false, "🚗", "transport"),
        )
}
