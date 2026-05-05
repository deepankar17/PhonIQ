package com.phoniq.app.domain.spam

import com.phoniq.app.data.db.dao.SpamNumberDao
import com.phoniq.app.data.db.entity.SpamNumberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds the local spam_numbers table with known high-confidence spam patterns.
 *
 * Sources: TRAI DND database patterns, community-reported IVR/robocall prefixes.
 * All offline — no network access required.
 */
object SpamCorpus {

    /**
     * Inbound number patterns (prefixes / full numbers) known to be spam/robocall.
     * Format: national numbers without country code.
     */
    private val knownSpamPatterns = listOf(
        // IVR / telemarketing number series (TRAI DND exempted but widely reported)
        "1800", "1860", "140",   // Toll-free / service numbers used for spam
        // Common Indian robocall prefixes
        "08030", "08040", "08041", "08042", "08043", "08044",
        "04044", "04041",
        "02228", "02261",
        // Widely-reported specific spam numbers (India)
        "9220092200", "1800120", "18001800",
    )

    /**
     * Known fraudulent / phishing sender IDs (as fragments).
     * These are DLT sender IDs that mimic banks but are not registered.
     */
    val suspiciousSenderFragments = setOf(
        "hdfk", "hdfcbnk",  // typo-squatting on HDFC
        "sbibk", "sbi0",    // typo-squatting on SBI
        "icicibk",          // typo ICICI
        "axisbk0",
        "paytmm",           // double letter
        "phonepe0",
        "amazon0",
    )

    /**
     * High-confidence phrase patterns in SMS body that indicate scam (used by SmsParser too).
     * Duplicated here for caller-ID spam scoring from body.
     */
    val highConfidenceScamPhrases = listOf(
        "your sim will be blocked",
        "your number will be deactivated",
        "click to verify your account",
        "update your kyc immediately",
        "send your aadhaar",
        "send your pan",
        "share your otp",
        "confirm your card details",
        "immediate suspension",
    )

    /**
     * Seeds the spam_numbers table with known spam entries.
     * Should be called once on first launch (guarded by a SharedPreferences flag).
     */
    suspend fun seed(dao: SpamNumberDao) = withContext(Dispatchers.IO) {
        val existing = dao.countAll()
        if (existing > 0) return@withContext   // already seeded

        val entities = knownSpamPatterns.map { pattern ->
            SpamNumberEntity(
                number = pattern,
                source = "LOCAL_LIST",
                confidence = 0.85f,
                reportedCount = 0,
            )
        }
        dao.insertAll(entities)
    }

    /** Returns true if [number] matches any known spam prefix/pattern. */
    fun matchesKnownSpam(number: String): Boolean {
        val clean = number.replace(Regex("[^0-9]"), "").trimStart('0')
        return knownSpamPatterns.any { pattern ->
            val cleanPat = pattern.replace(Regex("[^0-9]"), "")
            clean.startsWith(cleanPat) || clean == cleanPat
        }
    }
}
