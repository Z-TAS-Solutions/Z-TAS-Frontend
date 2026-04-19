package com.ztas.app

import java.util.Locale

/**
 * Shared helpers so profile UI can tell an email-style handle from a plausible full name.
 */
object DisplayNameHints {

    /** True when [name] is a single token matching the email local-part (letters/digits only). */
    fun isEmailLocalHandle(name: String, email: String): Boolean {
        if (email.isBlank()) return false
        if (name.any { it.isWhitespace() }) return false
        val local = email.substringBefore('@', missingDelimiterValue = email)
            .lowercase(Locale.getDefault())
            .filter { it.isLetterOrDigit() }
        if (local.isEmpty()) return false
        val compact = name.lowercase(Locale.getDefault()).filter { it.isLetterOrDigit() }
        return compact == local
    }
}
