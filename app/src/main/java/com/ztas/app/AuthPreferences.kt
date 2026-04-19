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

    fun setCachedDisplayName(context: Context, value: String?) {
        val e = prefs(context).edit()
        if (value.isNullOrBlank()) e.remove(KEY_DISPLAY_NAME)
        else e.putString(KEY_DISPLAY_NAME, value.trim())
        e.apply()
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
