package com.ztas.app

import android.content.Context

/**
 * Single place for “what name do we show?” — profile header, Home welcome, etc.
 * Existing users don’t need to re-register; a good value from [UserProfileJson] + this logic is enough.
 */
object ProfileDisplayName {

    /** Turns "ravi.kumar@example.com" → "Ravi Kumar" when we have nothing better. */
    fun displayNameFromEmail(email: String): String {
        val local = email.substringBefore('@', missingDelimiterValue = email)
        if (local.isBlank()) return "User"
        return local
            .split('.', '_', '-', '+')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
            }
    }

    /**
     * Prefer locally cached full name when `/user/profile` still returns an email-style handle,
     * otherwise prefer API / parsed profile name.
     */
    fun headerName(context: Context, profileName: String, profileEmail: String): String {
        val email = profileEmail.ifBlank { AuthPreferences.cachedEmail(context) }
        val cached = AuthPreferences.cachedDisplayName(context).trim()
        if (cached.isNotBlank()) {
            if (profileName.isBlank() || DisplayNameHints.isEmailLocalHandle(profileName, email)) {
                return cached
            }
            return profileName
        }
        if (profileName.isNotBlank()) return profileName
        return displayNameFromEmail(email)
    }

    /** Save a server-provided proper name so the next cold start isn’t stuck on email handles. */
    fun persistIfRichLabel(context: Context, resolved: String, emailForCheck: String) {
        if (resolved.isBlank()) return
        if (!DisplayNameHints.isEmailLocalHandle(resolved, emailForCheck)) {
            AuthPreferences.setCachedDisplayName(context, resolved.trim())
        }
    }
}
