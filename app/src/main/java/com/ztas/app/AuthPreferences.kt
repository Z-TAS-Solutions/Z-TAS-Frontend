package com.ztas.app

import android.content.Context

/**
 * Lightweight session store after WebAuthn login. Replace with EncryptedSharedPreferences / DataStore for production.
 */
object AuthPreferences {

    private const val PREFS = "ztas_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"
    private const val KEY_ROLE = "role"
    private const val KEY_PROFILE_IMAGE_PATH = "profile_image_path"
    /** Full name from registration (or login payload); used for UI before/without profile API. */
    private const val KEY_DISPLAY_NAME = "display_name"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveSession(context: Context, accessToken: String, userId: String, email: String, role: String) {
        val p = prefs(context)
        val prevUserId = p.getString(KEY_USER_ID, "").orEmpty().trim()
        val nextUserId = userId.trim()
        val e = p.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken.trim())
            .putString(KEY_USER_ID, nextUserId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role)
        // Avoid showing one user's saved full name after a different account signs in.
        if (prevUserId.isNotEmpty() && nextUserId.isNotEmpty() && prevUserId != nextUserId) {
            e.remove(KEY_DISPLAY_NAME)
        }
        e.apply()
    }

    fun bearerOrNull(context: Context): String? {
        val t = prefs(context).getString(KEY_ACCESS_TOKEN, null)?.trim().orEmpty()
        if (t.isEmpty()) return null
        return if (t.startsWith("Bearer ", ignoreCase = true)) t else "Bearer $t"
    }

    fun cachedEmail(context: Context): String =
        prefs(context).getString(KEY_EMAIL, "").orEmpty()

    fun cachedDisplayName(context: Context): String =
        prefs(context).getString(KEY_DISPLAY_NAME, "").orEmpty()

    /**
     * Persists the user's full name. The optional [emailForCheck] guards against accidentally
     * storing the email's local-part (e.g. `alishamohamed7864`) as the "real" name — which
     * happens when downstream code falls back to a server's WebAuthn `user.displayName`.
     * When [emailForCheck] is provided and [value] looks like that handle, we ignore the write
     * (and clear any existing matching value so old corrupt cache self-heals).
     */
    fun setCachedDisplayName(context: Context, value: String?, emailForCheck: String? = null) {
        val e = prefs(context).edit()
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            e.remove(KEY_DISPLAY_NAME).apply()
            return
        }
        val email = emailForCheck?.trim().orEmpty().ifEmpty { cachedEmail(context) }
        if (email.isNotEmpty() && DisplayNameHints.isEmailLocalHandle(trimmed, email)) {
            // Looks like the email handle, not a real full name. Discard it
            // and remove anything stale that already matched it.
            val current = prefs(context).getString(KEY_DISPLAY_NAME, "").orEmpty().trim()
            if (current.isNotEmpty() && DisplayNameHints.isEmailLocalHandle(current, email)) {
                e.remove(KEY_DISPLAY_NAME).apply()
            }
            return
        }
        e.putString(KEY_DISPLAY_NAME, trimmed).apply()
    }

    /**
     * Drops a previously-cached display name when it turns out to just be the email's
     * local-part. Safe to call on every app launch or screen entry — it's a no-op for
     * good values.
     */
    fun clearCachedDisplayNameIfEmailHandle(context: Context) {
        val current = prefs(context).getString(KEY_DISPLAY_NAME, "").orEmpty().trim()
        if (current.isEmpty()) return
        val email = cachedEmail(context)
        if (email.isNotBlank() && DisplayNameHints.isEmailLocalHandle(current, email)) {
            prefs(context).edit().remove(KEY_DISPLAY_NAME).apply()
        }
    }

    fun cachedUserId(context: Context): String =
        prefs(context).getString(KEY_USER_ID, "").orEmpty()

    /** Persisted display name (set during registration and refreshed from /user/profile). */
    fun cachedName(context: Context): String =
        prefs(context).getString(KEY_NAME, "").orEmpty()

    fun setCachedName(context: Context, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        prefs(context).edit().putString(KEY_NAME, trimmed).apply()
    }

    fun setProfileImagePath(context: Context, absolutePath: String?) {
        val e = prefs(context).edit()
        if (absolutePath.isNullOrBlank()) e.remove(KEY_PROFILE_IMAGE_PATH)
        else e.putString(KEY_PROFILE_IMAGE_PATH, absolutePath)
        e.apply()
    }

    fun profileImagePath(context: Context): String? =
        prefs(context).getString(KEY_PROFILE_IMAGE_PATH, null)?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
