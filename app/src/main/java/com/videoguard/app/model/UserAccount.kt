package com.videoguard.app.model

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val handle: String,
    val initial: String = if (name.isNotBlank()) name.first().uppercase() else "U",
    val avatarUrl: String? = null,
    val avatarBgColorHex: Long = 0xFF8E24AAL,
    val isCurrent: Boolean = false,
    val isGoogleAuthenticated: Boolean = false
)
