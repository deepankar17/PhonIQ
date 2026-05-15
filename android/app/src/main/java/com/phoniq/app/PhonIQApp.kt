package com.phoniq.app

import android.app.Application
import androidx.room.Room
import com.phoniq.app.data.db.DatabaseMigrations
import com.phoniq.app.data.db.PhonIQDatabase
import com.phoniq.app.data.repository.CallLogRepository
import com.phoniq.app.data.repository.ContactsRepository
import com.phoniq.app.data.repository.SmsRepository
import com.phoniq.app.data.repository.TransactionRepository
import com.phoniq.app.domain.sms.SmsParser
import com.phoniq.app.domain.spam.SpamCorpus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhonIQApp : Application() {

    private val dbLock = Any()
    @Volatile
    private var databaseRef: PhonIQDatabase? = null

    val database: PhonIQDatabase
        get() =
            synchronized(dbLock) {
                if (databaseRef == null) {
                    databaseRef =
                        Room.databaseBuilder(this, PhonIQDatabase::class.java, "phoniq.db")
                            .addMigrations(DatabaseMigrations.MIGRATION_4_5)
                            .fallbackToDestructiveMigration(dropAllTables = true)
                            .build()
                }
                databaseRef!!
            }

    /** Close Room DB so a backup file can replace `phoniq.db` on disk (restore). */
    fun closeDatabase() {
        synchronized(dbLock) {
            databaseRef?.close()
            databaseRef = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Seed spam corpus once on first launch (no-op if already seeded)
        CoroutineScope(Dispatchers.IO).launch {
            SpamCorpus.seed(database.spamNumberDao())
        }
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

