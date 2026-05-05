package com.phoniq.app

import android.app.Application
import androidx.room.Room
import com.phoniq.app.data.db.PhonIQDatabase
import com.phoniq.app.data.repository.CallLogRepository
import com.phoniq.app.data.repository.ContactsRepository
import com.phoniq.app.data.repository.SmsRepository
import com.phoniq.app.data.repository.TransactionRepository
import com.phoniq.app.domain.sms.SmsParser

class PhonIQApp : Application() {

    val database: PhonIQDatabase by lazy {
        Room.databaseBuilder(this, PhonIQDatabase::class.java, "phoniq.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val smsParser: SmsParser by lazy { SmsParser() }

    val smsRepository: SmsRepository by lazy {
        SmsRepository(this, database.smsDao(), database.otpLogDao(), database.transactionDao(), smsParser)
    }

    val callLogRepository: CallLogRepository by lazy {
        CallLogRepository(this, database.callLogDao(), database.spamNumberDao())
    }

    val contactsRepository: ContactsRepository by lazy {
        ContactsRepository(this, database.contactDao())
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao(), database.budgetDao(), database.accountDao())
    }
}

