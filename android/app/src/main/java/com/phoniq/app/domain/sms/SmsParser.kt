package com.phoniq.app.domain.sms

/**
 * Offline SMS parser: classifies messages and extracts structured data
 * (OTP codes, transaction amounts/merchants/types) from bank/UPI/CC SMS bodies.
 *
 * No network access — pure regex on local SMS body text.
 */
class SmsParser {

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    data class ParseResult(
        val category: SmsCategory,
        val otp: OtpResult? = null,
        val transaction: TransactionResult? = null,
    )

    data class OtpResult(
        val code: String,
        /** Inferred TTL in seconds (300 = 5 min default when not stated). */
        val ttlSeconds: Int = 300,
    )

    data class TransactionResult(
        val amount: Double,
        /** "DEBIT" | "CREDIT" */
        val type: String,
        val merchant: String?,
        val account: String?,
        val category: String,
    )

    enum class SmsCategory {
        OTP, TRANSACTION, SPAM, PERSONAL, PROMO, BILL, DELIVERY, TRAVEL, OTHER
    }

    fun parse(sender: String, body: String): ParseResult {
        val lowerBody = body.lowercase()
        val lowerSender = sender.lowercase()

        // OTP check first — short-circuits other checks
        val otpResult = tryParseOtp(body, lowerBody)
        if (otpResult != null) return ParseResult(SmsCategory.OTP, otp = otpResult)

        // Spam / promo signals
        if (isSpam(lowerBody, lowerSender)) return ParseResult(SmsCategory.SPAM)
        if (isPromo(lowerBody, lowerSender)) return ParseResult(SmsCategory.PROMO)

        // Transaction detection (bank / UPI / wallet senders)
        if (isTransactionalSender(lowerSender) || hasTransactionKeywords(lowerBody)) {
            val txn = tryParseTransaction(body, lowerBody, sender)
            if (txn != null) return ParseResult(SmsCategory.TRANSACTION, transaction = txn)
        }

        // Bill / utility
        if (hasBillKeywords(lowerBody)) return ParseResult(SmsCategory.BILL)

        // Delivery / e-commerce
        if (hasDeliveryKeywords(lowerBody)) return ParseResult(SmsCategory.DELIVERY)

        // Travel
        if (hasTravelKeywords(lowerBody)) return ParseResult(SmsCategory.TRAVEL)

        return ParseResult(SmsCategory.OTHER)
    }

    // -----------------------------------------------------------------------
    // OTP
    // -----------------------------------------------------------------------

    private val otpPatterns = listOf(
        Regex("""(?:otp|one.time.password|verification.code|auth(?:entication)?.code)[^\d]*(\d{4,8})""", RegexOption.IGNORE_CASE),
        Regex("""(\d{4,8})\s*(?:is your|as your)\s*(?:otp|one.time|verification|login|auth)""", RegexOption.IGNORE_CASE),
        Regex("""use\s+(\d{4,8})\s+(?:to|as|for)""", RegexOption.IGNORE_CASE),
        Regex("""code[:\s]+(\d{4,8})""", RegexOption.IGNORE_CASE),
        Regex("""(\d{6})\s+is\s+(?:the|your)""", RegexOption.IGNORE_CASE),
    )

    private val ttlPatterns = listOf(
        Regex("""valid\s+for\s+(\d+)\s*min""", RegexOption.IGNORE_CASE) to 60,
        Regex("""expires?\s+in\s+(\d+)\s*min""", RegexOption.IGNORE_CASE) to 60,
        Regex("""valid\s+for\s+(\d+)\s*hour""", RegexOption.IGNORE_CASE) to 3600,
        Regex("""valid\s+for\s+(\d+)\s*sec""", RegexOption.IGNORE_CASE) to 1,
    )

    private fun tryParseOtp(body: String, lower: String): OtpResult? {
        if (!lower.contains("otp") && !lower.contains("one time") &&
            !lower.contains("verification") && !lower.contains("password") &&
            !lower.contains("code") && !lower.contains("pin")
        ) return null

        for (pattern in otpPatterns) {
            val match = pattern.find(body) ?: continue
            val code = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: continue
            if (code.length < 4) continue
            val ttl = parseTtl(lower)
            return OtpResult(code = code, ttlSeconds = ttl)
        }
        return null
    }

    private fun parseTtl(lower: String): Int {
        for ((pattern, multiplier) in ttlPatterns) {
            val match = pattern.find(lower) ?: continue
            val value = match.groupValues[1].toIntOrNull() ?: continue
            return value * multiplier
        }
        return 300
    }

    // -----------------------------------------------------------------------
    // Transaction
    // -----------------------------------------------------------------------

    private val amountPattern = Regex(
        """(?:INR|Rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "payment of", "withdrawn", "purchase",
        "charged", "transferred to", "sent to", "emi of", "emi due",
    )
    private val creditKeywords = listOf(
        "credited", "credit", "received", "refund", "cashback", "salary", "deposited",
        "transfer from", "received from",
    )

    private val merchantPatterns = listOf(
        Regex("""at\s+([A-Za-z0-9 &._'-]{2,40})""", RegexOption.IGNORE_CASE),
        Regex("""to\s+([A-Za-z0-9 &._'-]{2,40})\s*(?:via|using|on|$)""", RegexOption.IGNORE_CASE),
        Regex("""(?:from|at)\s+([A-Za-z0-9 &._'-]{2,40})""", RegexOption.IGNORE_CASE),
    )

    private val accountPattern = Regex("""(?:a/c|account|ac|card)\s*(?:no\.?|number|ending|xx+)?\s*[*xX]*(\d{4,6})""", RegexOption.IGNORE_CASE)

    private fun tryParseTransaction(body: String, lower: String, sender: String): TransactionResult? {
        val amountMatch = amountPattern.find(body) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        val type = when {
            debitKeywords.any { lower.contains(it) } -> "DEBIT"
            creditKeywords.any { lower.contains(it) } -> "CREDIT"
            else -> return null
        }

        val merchant = merchantPatterns.firstNotNullOfOrNull { it.find(body)?.groupValues?.get(1)?.trim() }
        val account = accountPattern.find(body)?.groupValues?.get(1)

        val category = inferCategory(lower, merchant?.lowercase() ?: "")
        return TransactionResult(amount, type, merchant, account, category)
    }

    // -----------------------------------------------------------------------
    // Category inference
    // -----------------------------------------------------------------------

    private val categoryKeywords = mapOf(
        "FOOD" to listOf("swiggy", "zomato", "restaurant", "food", "cafe", "coffee", "pizza", "burger", "hotel", "dining", "blinkit", "zepto", "instamart"),
        "SHOPPING" to listOf("amazon", "flipkart", "myntra", "meesho", "nykaa", "ajio", "shopping", "mall", "store", "retail"),
        "TRANSPORT" to listOf("uber", "ola", "rapido", "metro", "irctc", "flight", "indigo", "air india", "spicejet", "bus", "cab", "auto", "fastag", "petrol", "fuel"),
        "BILLS" to listOf("bescom", "bses", "tata power", "electricity", "recharge", "jio", "airtel", "vodafone", "vi ", "broadband", "internet", "gas", "water", "rent", "maintenance"),
        "EMI" to listOf("emi", "loan", "equated"),
        "SALARY" to listOf("salary", "payroll", "wages", "stipend"),
        "INVESTMENT" to listOf("mutual fund", "mf ", "sip", "nsc", "ppf", "stocks", "zerodha", "groww", "upstox", "coin", "elss"),
        "HEALTH" to listOf("pharmacy", "hospital", "clinic", "medic", "apollo", "doctor", "lab"),
        "ENTERTAINMENT" to listOf("netflix", "hotstar", "spotify", "amazon prime", "youtube premium", "disney", "movie", "pvr", "inox"),
        "ATM" to listOf("atm withdrawal", "cash withdrawal"),
    )

    private fun inferCategory(lower: String, merchant: String): String {
        val combined = "$lower $merchant"
        for ((cat, keywords) in categoryKeywords) {
            if (keywords.any { combined.contains(it) }) return cat
        }
        return "OTHER"
    }

    // -----------------------------------------------------------------------
    // Classification helpers
    // -----------------------------------------------------------------------

    private val transactionalSenderPrefixes = setOf(
        "hdfcbk", "hdfcbank", "sbibnk", "sbipay", "icicib", "axisbk", "kotakbk",
        "yesbnk", "indusb", "pnbsms", "phonpe", "gpay", "paytm", "amznpay",
        "mobikw", "bhim", "upi", "atm-", "vm-",
    )

    private fun isTransactionalSender(lower: String): Boolean =
        transactionalSenderPrefixes.any { lower.contains(it) }

    private fun hasTransactionKeywords(lower: String): Boolean =
        (debitKeywords + creditKeywords).any { lower.contains(it) }

    private fun hasBillKeywords(lower: String): Boolean =
        listOf("bill", "due", "overdue", "invoice", "payment due", "recharge", "expiry").any { lower.contains(it) }

    private fun hasDeliveryKeywords(lower: String): Boolean =
        listOf("delivered", "out for delivery", "shipped", "dispatched", "order", "package", "parcel", "tracking").any { lower.contains(it) }

    private fun hasTravelKeywords(lower: String): Boolean =
        listOf("flight", "boarding pass", "pnr", "irctc", "train", "bus ticket", "hotel booking", "cab booked").any { lower.contains(it) }

    private val promoKeywords = listOf(
        "offer", "discount", "cashback", "win", "congratulations", "free", "limited time",
        "exclusive deal", "sale", "click here", "unsubscribe", "earn reward",
    )
    private val promoSenderSuffixes = listOf("-promo", "-offer", "-alerts")

    private fun isPromo(lower: String, lowerSender: String): Boolean {
        if (promoSenderSuffixes.any { lowerSender.endsWith(it) }) return true
        val promoHits = promoKeywords.count { lower.contains(it) }
        return promoHits >= 2
    }

    private val spamKeywords = listOf(
        "lottery", "won rs", "prize", "click to claim", "free iphone", "free gift",
        "kyc update", "your account will be blocked", "send otp to", "verify now to avoid",
    )

    private fun isSpam(lower: String, lowerSender: String): Boolean =
        spamKeywords.any { lower.contains(it) }
}
