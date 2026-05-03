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
    MessagesMarkAllRead,
    MessagesInboxCleaner,
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
        ShellMenuAction.PhoneDeleteAllCalls ->
            R.string.wire_phone_delete_title to R.string.wire_phone_delete_body
        ShellMenuAction.PhoneCommunicationInsights ->
            R.string.wire_phone_insights_title to R.string.wire_phone_insights_body
        ShellMenuAction.MessagesMarkAllRead ->
            R.string.wire_messages_mark_read_title to R.string.wire_messages_mark_read_body
        ShellMenuAction.MessagesInboxCleaner ->
            R.string.wire_messages_inbox_title to R.string.wire_messages_inbox_body
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
