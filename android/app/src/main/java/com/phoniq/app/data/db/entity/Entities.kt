package com.phoniq.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ---------------------------------------------------------------------------
// contacts
// ---------------------------------------------------------------------------
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val number: String,
    val tag: String? = null,
    @ColumnInfo(name = "spam_score") val spamScore: Int = 0,
    val notes: String? = null,
    @ColumnInfo(name = "avatar_color") val avatarColor: String = "#6C63FF",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

// ---------------------------------------------------------------------------
// call_log
// ---------------------------------------------------------------------------
@Entity(
    tableName = "call_log",
    foreignKeys = [ForeignKey(
        entity = ContactEntity::class,
        parentColumns = ["id"],
        childColumns = ["contact_id"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("contact_id")],
)
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "contact_id") val contactId: Long? = null,
    val number: String,
    @ColumnInfo(name = "duration_sec") val durationSec: Int = 0,
    /** "INCOMING" | "OUTGOING" | "MISSED" | "REJECTED" | "BLOCKED" */
    val type: String,
    val timestamp: Long,
    val notes: String? = null,
    @ColumnInfo(name = "recording_path") val recordingPath: String? = null,
)

// ---------------------------------------------------------------------------
// sms_messages
// ---------------------------------------------------------------------------
@Entity(tableName = "sms_messages")
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Long,
    /** "OTP" | "TRANSACTION" | "SPAM" | "PERSONAL" | "PROMO" | "BILL" | "DELIVERY" | "TRAVEL" | "OTHER" */
    val category: String = "OTHER",
    @ColumnInfo(name = "thread_id") val threadId: String,
    @ColumnInfo(name = "is_transaction") val isTransaction: Boolean = false,
    @ColumnInfo(name = "is_otp") val isOtp: Boolean = false,
    @ColumnInfo(name = "is_spam") val isSpam: Boolean = false,
    @ColumnInfo(name = "is_read") val isRead: Boolean = false,
)

// ---------------------------------------------------------------------------
// accounts
// ---------------------------------------------------------------------------
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "bank_name") val bankName: String,
    val last4: String,
    /** "SAVINGS" | "CURRENT" | "CREDIT" | "WALLET" | "UPI" */
    @ColumnInfo(name = "account_type") val accountType: String,
    val balance: Double = 0.0,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis(),
)

// ---------------------------------------------------------------------------
// transactions
// ---------------------------------------------------------------------------
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = SmsMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["sms_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("sms_id"), Index("account_id")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sms_id") val smsId: Long? = null,
    val amount: Double,
    /** "DEBIT" | "CREDIT" */
    @ColumnInfo(name = "txn_type") val txnType: String,
    @ColumnInfo(name = "account_id") val accountId: Long? = null,
    val merchant: String? = null,
    val category: String = "OTHER",
    val date: Long,
    @ColumnInfo(name = "is_manual") val isManual: Boolean = false,
)

// ---------------------------------------------------------------------------
// budgets
// ---------------------------------------------------------------------------
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category", "month_year"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    @ColumnInfo(name = "monthly_limit") val monthlyLimit: Double,
    /** Format: "YYYY-MM" e.g. "2026-04" */
    @ColumnInfo(name = "month_year") val monthYear: String,
)

// ---------------------------------------------------------------------------
// spam_numbers
// ---------------------------------------------------------------------------
@Entity(
    tableName = "spam_numbers",
    indices = [Index(value = ["number"], unique = true)],
)
data class SpamNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    /** "USER" | "LOCAL_LIST" | "PATTERN" */
    val source: String = "USER",
    val confidence: Float = 1.0f,
    @ColumnInfo(name = "reported_count") val reportedCount: Int = 1,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
)

// ---------------------------------------------------------------------------
// categories
// ---------------------------------------------------------------------------
@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["parent_id"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("parent_id")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "category",
    val color: String = "#6C63FF",
    @ColumnInfo(name = "parent_id") val parentId: Long? = null,
    @ColumnInfo(name = "is_system") val isSystem: Boolean = false,
)

// ---------------------------------------------------------------------------
// otp_log
// ---------------------------------------------------------------------------
@Entity(
    tableName = "otp_log",
    foreignKeys = [ForeignKey(
        entity = SmsMessageEntity::class,
        parentColumns = ["id"],
        childColumns = ["sms_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sms_id")],
)
data class OtpLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sms_id") val smsId: Long,
    @ColumnInfo(name = "otp_code") val otpCode: String,
    val sender: String,
    @ColumnInfo(name = "expiry_at") val expiryAt: Long,
    @ColumnInfo(name = "was_copied") val wasCopied: Boolean = false,
)
