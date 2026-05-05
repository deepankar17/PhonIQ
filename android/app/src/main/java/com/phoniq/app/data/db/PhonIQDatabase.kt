package com.phoniq.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phoniq.app.data.db.dao.AccountDao
import com.phoniq.app.data.db.dao.BudgetDao
import com.phoniq.app.data.db.dao.CallLogDao
import com.phoniq.app.data.db.dao.ContactDao
import com.phoniq.app.data.db.dao.OtpLogDao
import com.phoniq.app.data.db.dao.SmsDao
import com.phoniq.app.data.db.dao.SpamNumberDao
import com.phoniq.app.data.db.dao.TransactionDao
import com.phoniq.app.data.db.entity.AccountEntity
import com.phoniq.app.data.db.entity.BudgetEntity
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.CategoryEntity
import com.phoniq.app.data.db.entity.ContactEntity
import com.phoniq.app.data.db.entity.OtpLogEntity
import com.phoniq.app.data.db.entity.SmsMessageEntity
import com.phoniq.app.data.db.entity.SpamNumberEntity
import com.phoniq.app.data.db.entity.TransactionEntity

@Database(
    entities = [
        ContactEntity::class,
        CallLogEntity::class,
        SmsMessageEntity::class,
        TransactionEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        SpamNumberEntity::class,
        CategoryEntity::class,
        OtpLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class PhonIQDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao
    abstract fun smsDao(): SmsDao
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun spamNumberDao(): SpamNumberDao
    abstract fun otpLogDao(): OtpLogDao
}
