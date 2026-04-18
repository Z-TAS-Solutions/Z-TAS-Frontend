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
    private const val KEY_ROLE = "role"
    private const val KEY_PROFILE_IMAGE_PATH = "profile_image_path"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveSession(context: Context, accessToken: String, userId: String, email: String, role: String) {
        prefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken.trim())
            .putString(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun bearerOrNull(context: Context): String? {
        val t = prefs(context).getString(KEY_ACCESS_TOKEN, null)?.trim().orEmpty()
        if (t.isEmpty()) return null
        return if (t.startsWith("Bearer ", ignoreCase = true)) t else "Bearer $t"
    }

    fun cachedEmail(context: Context): String =
        prefs(context).getString(KEY_EMAIL, "").orEmpty()

    fun cachedUserId(context: Context): String =
        prefs(context).getString(KEY_USER_ID, "").orEmpty()

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
