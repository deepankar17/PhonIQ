package com.phoniq.app.ui.shell

import com.phoniq.app.R

enum class MainTabRoute {
    Phone,
    Messages,
    Money,
}

fun mainTabFromRoute(route: String?): MainTabRoute =
    when (route) {
        "messages" -> MainTabRoute.Messages
        "money" -> MainTabRoute.Money
        else -> MainTabRoute.Phone
    }

enum class ShellMenuAction {
    Settings,
    PhoneDeleteAllCalls,
    PhoneCommunicationInsights,
    PhoneWhoIsThis,
    PhoneMergeContacts,
    PhoneAfterCall,
    PhoneRecording,
    MessagesMarkAllRead,
    MessagesInboxCleaner,
    MessagesBillHygiene,
    MessagesOtpCenter,
    MoneyBudget,
    MoneyBillDue,
    MoneyRecurring,
    MoneySalaryYearly,
    MoneyInvestments,
    MoneyExport,
}

/** Wire copy for overflow actions (null → handled elsewhere, e.g. Settings). */
fun ShellMenuAction.wireStrings(): Pair<Int, Int>? =
    when (this) {
        ShellMenuAction.Settings -> null
        ShellMenuAction.PhoneDeleteAllCalls -> null
        ShellMenuAction.PhoneCommunicationInsights ->
            R.string.wire_phone_insights_title to R.string.wire_phone_insights_body
        ShellMenuAction.PhoneWhoIsThis ->
            R.string.wire_phone_who_title to R.string.wire_phone_who_body
        ShellMenuAction.PhoneMergeContacts ->
            R.string.wire_phone_merge_title to R.string.wire_phone_merge_body
        ShellMenuAction.PhoneAfterCall ->
            R.string.wire_phone_aftercall_title to R.string.wire_phone_aftercall_body
        ShellMenuAction.PhoneRecording ->
            R.string.wire_phone_recording_title to R.string.wire_phone_recording_body
        ShellMenuAction.MessagesMarkAllRead -> null
        ShellMenuAction.MessagesInboxCleaner -> null
        ShellMenuAction.MessagesBillHygiene ->
            R.string.wire_messages_bill_title to R.string.wire_messages_bill_body
        ShellMenuAction.MessagesOtpCenter ->
            R.string.wire_messages_otp_title to R.string.wire_messages_otp_body
        ShellMenuAction.MoneyBudget -> null
        ShellMenuAction.MoneyBillDue ->
            R.string.wire_money_bill_title to R.string.wire_money_bill_body
        ShellMenuAction.MoneyRecurring ->
            R.string.wire_money_recurring_title to R.string.wire_money_recurring_body
        ShellMenuAction.MoneySalaryYearly ->
            R.string.wire_money_salary_title to R.string.wire_money_salary_body
        ShellMenuAction.MoneyInvestments ->
            R.string.wire_money_invest_title to R.string.wire_money_invest_body
        ShellMenuAction.MoneyExport ->
            R.string.wire_money_export_title to R.string.wire_money_export_body
    }
