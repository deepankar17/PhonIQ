package com.phoniq.app.data.model

data class QuickCallEntry(
    val id: String,
    val name: String,
    val meta: String,
    val initial: String,
    /** ARGB color for avatar circle (mockup-style gradients simplified to solid). */
    val avatarColorArgb: Long,
)
