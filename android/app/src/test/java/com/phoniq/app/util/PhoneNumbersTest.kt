package com.phoniq.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun normalizePhoneKeyUsesLastTenDigits() {
        assertEquals("9876543210", normalizePhoneKey("+91 98765 43210"))
    }

    @Test
    fun normalizePhoneKeyEmpty() {
        assertEquals("", normalizePhoneKey(null))
        assertEquals("", normalizePhoneKey("   "))
    }
}
