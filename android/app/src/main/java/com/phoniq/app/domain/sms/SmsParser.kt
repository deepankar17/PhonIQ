package com.phoniq.app.domain.sms

/**
 * Offline SMS parser: classifies messages and extracts structured data
 * (OTP codes, transaction amounts/merchants/types) from bank/UPI/CC SMS bodies.
 *
 * No network access — pure regex on local SMS body text.
 * v2: expanded bank/sender list, richer spam heuristics, more OTP patterns.
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

        // Hard spam signals (high confidence)
        if (isSpam(lowerBody, lowerSender)) return ParseResult(SmsCategory.SPAM)

        // Promo signals
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
        // "OTP is 123456" / "OTP: 123456"
        Regex("""(?:otp|one.time.password)[:\s]+(\d{4,8})""", RegexOption.IGNORE_CASE),
        // "verification code 123456"
        Regex("""(?:verification|auth(?:entication)?|login|access|security|confirm)\s+(?:code|pin|password)[:\s]+(\d{4,8})""", RegexOption.IGNORE_CASE),
        // "123456 is your OTP / code / pin"
        Regex("""(\d{4,8})\s+is\s+(?:your|the)\s+(?:otp|one.time|verification|login|auth|code|pin)""", RegexOption.IGNORE_CASE),
        // "use 123456 to / as"
        Regex("""(?:use|enter)\s+(\d{4,8})\s+(?:to|as|for)""", RegexOption.IGNORE_CASE),
        // "code: 123456"
        Regex("""(?:^|[^\d])code[:\s]+(\d{4,8})""", RegexOption.IGNORE_CASE),
        // generic 6-digit at start/end of sentence
        Regex("""(\d{6})\s+is\s+(?:the|your)""", RegexOption.IGNORE_CASE),
        // HDFC-style "Your OTP for ... is 123456. Valid for"
        Regex("""is\s+(\d{4,8})\s*[.。,]?\s*(?:valid|do not|don't|please)""", RegexOption.IGNORE_CASE),
        // "T-OTP: 123456"
        Regex("""t-otp[:\s]+(\d{4,8})""", RegexOption.IGNORE_CASE),
        // "123456 is valid"
        Regex("""(\d{6})\s+(?:is|are)\s+valid""", RegexOption.IGNORE_CASE),
        // "pin is 1234"
        Regex("""pin\s+(?:is|:)\s+(\d{4,8})""", RegexOption.IGNORE_CASE),
    )

    private val ttlPatterns = listOf(
        Regex("""valid\s+(?:for|till|until)\s+(\d+)\s*min""", RegexOption.IGNORE_CASE) to 60,
        Regex("""expires?\s+in\s+(\d+)\s*min""", RegexOption.IGNORE_CASE) to 60,
        Regex("""valid\s+for\s+(\d+)\s*hour""", RegexOption.IGNORE_CASE) to 3600,
        Regex("""valid\s+for\s+(\d+)\s*sec""", RegexOption.IGNORE_CASE) to 1,
        Regex("""(\d+)\s*(?:minute|min)\s+(?:only|validity)""", RegexOption.IGNORE_CASE) to 60,
        Regex("""(\d+)\s*min(?:utes?)?\s*valid""", RegexOption.IGNORE_CASE) to 60,
    )

    private val otpTriggerWords = setOf(
        "otp", "one time", "one-time", "verification", "password", "code", "pin",
        "passcode", "authenticate", "2fa", "two-factor",
    )

    private fun tryParseOtp(body: String, lower: String): OtpResult? {
        if (otpTriggerWords.none { lower.contains(it) }) return null

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
        """(?:INR|Rs\.?|₹|inr)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    // Also match patterns like "debited for 1,234.56"
    private val amountFallbackPattern = Regex(
        """(?:debited|credited|spent|paid|received|refund(?:ed)?)\s+(?:with\s+|for\s+|of\s+)?(?:INR|Rs\.?|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "payment of", "withdrawn", "purchase",
        "charged", "transferred to", "sent to", "emi of", "emi due", "mandate executed",
        "auto debit", "si executed", "bill paid", "utility payment", "pos txn", "neft sent",
        "imps sent", "upi debit", "payment done", "successful payment",
    )
    private val creditKeywords = listOf(
        "credited", "credit", "received", "refund", "cashback", "salary", "deposited",
        "transfer from", "received from", "neft credit", "imps credit", "upi credit",
        "money added", "wallet loaded", "top up", "inward credit", "amount credited",
    )

    private val merchantPatterns = listOf(
        Regex("""at\s+([A-Za-z0-9 &._'/-]{3,40})""", RegexOption.IGNORE_CASE),
        Regex("""to\s+VPA\s+[\w.@-]+\s*(?:of|for)?\s*([A-Za-z0-9 &._'/-]{3,30})""", RegexOption.IGNORE_CASE),
        Regex("""(?:merchant|payee|to)\s*[:\-]?\s*([A-Za-z0-9 &._'/-]{3,40})""", RegexOption.IGNORE_CASE),
        Regex("""(?:from|at)\s+([A-Za-z0-9 &._'/-]{3,40})""", RegexOption.IGNORE_CASE),
    )

    private val accountPattern = Regex(
        """(?:a/c|account|ac|card|acct)\s*(?:no\.?|number|ending|xx+|last)?\s*[*xX·•-]*(\d{4,6})""",
        RegexOption.IGNORE_CASE,
    )

    private fun tryParseTransaction(body: String, lower: String, sender: String): TransactionResult? {
        val amountMatch = amountPattern.find(body) ?: amountFallbackPattern.find(body) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        val type = when {
            debitKeywords.any { lower.contains(it) } -> "DEBIT"
            creditKeywords.any { lower.contains(it) } -> "CREDIT"
            else -> return null
        }

        val merchant = merchantPatterns.firstNotNullOfOrNull { pat ->
            pat.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.length >= 3 && !it.all { c -> c.isDigit() } }
        }
        val account = accountPattern.find(body)?.groupValues?.get(1)
        val category = inferCategory(lower, merchant?.lowercase() ?: "")
        return TransactionResult(amount, type, merchant, account, category)
    }

    // -----------------------------------------------------------------------
    // Category inference
    // -----------------------------------------------------------------------

    private val categoryKeywords = mapOf(
        "FOOD" to listOf(
            "swiggy", "zomato", "restaurant", "food", "cafe", "coffee", "pizza", "burger",
            "hotel", "dining", "blinkit", "zepto", "instamart", "dunzo", "grofers", "bigbasket",
            "dominos", "kfc", "mcdonald", "starbucks", "chai", "biryani", "bakery", "canteen",
        ),
        "SHOPPING" to listOf(
            "amazon", "flipkart", "myntra", "meesho", "nykaa", "ajio", "shopping", "mall",
            "store", "retail", "snapdeal", "tata cliq", "reliance", "dmart", "croma", "jiomart",
            "firstcry", "pepperfry", "ikea", "decathlon",
        ),
        "TRANSPORT" to listOf(
            "uber", "ola", "rapido", "metro", "irctc", "flight", "indigo", "air india",
            "spicejet", "bus", "cab", "auto", "fastag", "petrol", "fuel", "toll", "redbus",
            "makemytrip", "easemytrip", "goibibo", "yatra", "meru", "bluesmart",
        ),
        "BILLS" to listOf(
            "bescom", "bses", "tata power", "electricity", "recharge", "jio", "airtel",
            "vodafone", "vi ", "vi-", "broadband", "internet", "gas", "water", "rent",
            "maintenance", "tatasky", "dish tv", "sun direct", "d2h", "act fibernet",
            "hathway", "sify", "bsnl", "mtnl", "landline", "bbps", "utility",
        ),
        "EMI" to listOf("emi", "loan", "equated", "installment", "emi deducted"),
        "SALARY" to listOf("salary", "payroll", "wages", "stipend", "month salary", "arrears"),
        "INVESTMENT" to listOf(
            "mutual fund", "mf ", " sip", "nsc", "ppf", "stocks", "zerodha", "groww",
            "upstox", "coin by", "elss", "demat", "brokerage", "nps ", "lic premium",
            "insurance premium", "ulip", "fd ", "fixed deposit", "rd ", "recurring deposit",
        ),
        "HEALTH" to listOf(
            "pharmacy", "hospital", "clinic", "medic", "apollo", "doctor", "lab",
            "medplus", "netmeds", "1mg", "practo", "diagnostic", "pathology", "dental",
            "pharmeasy", "tata 1mg", "health insurance",
        ),
        "ENTERTAINMENT" to listOf(
            "netflix", "hotstar", "spotify", "amazon prime", "youtube premium", "disney",
            "movie", "pvr", "inox", "bookmyshow", "zee5", "voot", "sonyliv", "jiocinema",
            "mxplayer", "alt balaji", "ludo", "gaming", "steam",
        ),
        "ATM" to listOf("atm withdrawal", "cash withdrawal", "cash at atm"),
    )

    private fun inferCategory(lower: String, merchant: String): String {
        val combined = "$lower $merchant"
        if (combined.contains("folio") ||
            combined.contains("purchase of units") ||
            combined.contains("allotted units") ||
            combined.contains("sip installment") ||
            combined.contains("lumpsum") && combined.contains("fund") ||
            combined.contains("demat") && combined.contains("credit") && combined.contains("fund")
        ) {
            return "INVESTMENT"
        }
        for ((cat, keywords) in categoryKeywords) {
            if (keywords.any { combined.contains(it) }) return cat
        }
        return "OTHER"
    }

    // -----------------------------------------------------------------------
    // Spam detection (expanded)
    // -----------------------------------------------------------------------

    /** High-confidence spam phrases. */
    private val spamKeywords = listOf(
        // Prize / lottery scams
        "lottery", "won rs", "prize money", "lucky winner", "click to claim",
        "claim your prize", "you have won", "congratulations you won", "gift voucher worth",
        // Phishing
        "kyc update", "kyc expired", "kyc not updated", "kyc pending",
        "your account will be blocked", "account suspended", "account will be suspended",
        "send otp to", "verify now to avoid", "immediate action required",
        "click link to verify", "click here to update", "log in immediately",
        // Free goods / scams
        "free iphone", "free samsung", "free gift", "free laptop",
        // Job scams
        "earn rs daily", "earn from home", "data entry job", "part time job offer",
        "work from home earn", "online job offer",
        // Loan scams
        "instant loan approved", "pre-approved loan", "loan offer in 2 minutes",
        "apply now for loan", "loan without documents",
        // Investment scams
        "guaranteed returns", "100% profit", "risk-free investment",
        // Explicit spam markers
        "unsubscribe now", "reply stop to", "to unsubscribe sms stop",
        "to opt out sms",
    )

    /** Moderate-confidence spam: require 2+ hits or sender is suspicious. */
    private val softSpamKeywords = listOf(
        "offer", "discount", "click here", "limited time", "exclusive deal",
        "sale", "earn reward", "cashback offer", "special offer",
        "act now", "hurry", "expires tonight", "last chance",
    )

    /** Known spam / promo sender ID fragments (Indian DLT sender IDs). */
    private val spamSenderFragments = setOf(
        "ad-", "tm-", "bl-", "lz-", "mf-", "cp-", "mg-",
        "-spam", "-promo", "-alerts", "-ads", "-offer", "-deal", "-mkt",
    )

    private fun isSpam(lower: String, lowerSender: String): Boolean {
        if (spamKeywords.any { lower.contains(it) }) return true
        // 2+ soft hits → likely spam
        val softHits = softSpamKeywords.count { lower.contains(it) }
        if (softHits >= 3) return true
        return false
    }

    // -----------------------------------------------------------------------
    // Promo detection
    // -----------------------------------------------------------------------

    private val promoSenderSuffixes = setOf(
        "-promo", "-offer", "-alerts", "-deals", "-ads", "-mkt", "-mktg",
        "-info", "-news", "-notif",
    )

    private val promoKeywords = listOf(
        "offer", "discount", "cashback", "win ", "congratulations", "free ",
        "limited time", "exclusive deal", "sale", "click here", "unsubscribe",
        "earn reward", "up to % off", "use code", "coupon code", "voucher",
        "happy hour", "flash sale",
    )

    private fun isPromo(lower: String, lowerSender: String): Boolean {
        if (promoSenderSuffixes.any { lowerSender.endsWith(it) }) return true
        val promoHits = promoKeywords.count { lower.contains(it) }
        return promoHits >= 2
    }

    // -----------------------------------------------------------------------
    // Transactional sender detection (expanded to full known DLT header set)
    // -----------------------------------------------------------------------

    private val transactionalSenderPrefixes = setOf(
        // HDFC
        "hdfcbk", "hdfcbank", "hdfccc",
        // SBI
        "sbibnk", "sbipay", "sbi-", "sbisms",
        // ICICI
        "icicib", "icicibank", "icici-",
        // Axis
        "axisbk", "axisbank", "axis-",
        // Kotak
        "kotakbk", "kotak-",
        // Yes Bank
        "yesbnk", "yesbank",
        // IndusInd
        "indusb", "indusind",
        // PNB
        "pnbsms", "pnb-",
        // Canara
        "canbnk", "canarabank",
        // BOB / BOI
        "bobsms", "boisms",
        // UPI / wallets
        "phonpe", "ph-phnpe", "bh-phonpe",
        "gpay", "googlepay",
        "paytm", "paytmbank",
        "amznpay", "amazon", "amazonpay",
        "mobikw", "mobikwik",
        "bhim",
        "freecharge", "fc-",
        "jiomoney",
        "airtelp",
        // Generic
        "upi-", "-upi",
        "atm-", "vm-",
        "neft-", "imps-",
        // Federal, IDFC, AU Small Finance
        "fedbkm", "idfcbk", "ausfbl",
        // RBL
        "rblbnk",
        // Standard Chartered
        "scbnk",
        // Citi
        "citibnk",
        // American Express
        "amexin",
        // NSDL / CDSL
        "nsdl-", "cdsl-",
    )

    private fun isTransactionalSender(lower: String): Boolean =
        transactionalSenderPrefixes.any { lower.contains(it) }

    private fun hasTransactionKeywords(lower: String): Boolean =
        (debitKeywords + creditKeywords).any { lower.contains(it) }

    // -----------------------------------------------------------------------
    // Keyword helpers
    // -----------------------------------------------------------------------

    private fun hasBillKeywords(lower: String): Boolean =
        listOf(
            "bill", "due date", "overdue", "invoice", "payment due", "recharge",
            "expiry", "bill generated", "outstanding", "minimum due", "total due",
            "bill amount", "credit card bill", "statement", "amount due",
        ).any { lower.contains(it) }

    private fun hasDeliveryKeywords(lower: String): Boolean =
        listOf(
            "delivered", "out for delivery", "shipped", "dispatched", "order",
            "package", "parcel", "tracking", "estimated delivery", "delivery partner",
            "arriving today", "your shipment", "order placed", "order confirmed",
            "order cancelled", "return initiated", "refund initiated",
        ).any { lower.contains(it) }

    private fun hasTravelKeywords(lower: String): Boolean =
        listOf(
            "flight", "boarding pass", "pnr", "irctc", "train", "bus ticket",
            "hotel booking", "cab booked", "seat confirmed", "e-ticket",
            "reservation", "check-in", "departure", "arrival", "layover",
        ).any { lower.contains(it) }
}
