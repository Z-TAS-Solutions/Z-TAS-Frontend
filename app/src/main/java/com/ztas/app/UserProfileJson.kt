package com.ztas.app

import com.ztas.app.network.UserProfileResponse
import org.json.JSONObject

/** Parses Z-QryptGIN profile JSON (flat or `{ "data": { ... } }`). */
object UserProfileJson {

    fun parse(json: String): UserProfileResponse? {
        return try {
            val root = JSONObject(json)
            val p = root.optJSONObject("data") ?: root
            val name = readDisplayName(p)
            val userId = readUserId(p)
            val email = p.optString("email")
            val phone = p.optString("phone").ifEmpty { p.optString("phone_no") }
            val status = p.optString("status").ifEmpty { "Active" }
            val active = p.optInt("active_devices", p.optInt("activeDevices", 0))
            val sec = p.optString("security_level").ifEmpty { p.optString("securityLevel", "Low") }
            val lastLogin = readLastLogin(p)
            if (userId.isBlank() && email.isBlank()) return null
            UserProfileResponse(
                userId = userId,
                name = name,
                email = email,
                phone = phone,
                status = status,
                activeDevices = active,
                securityLevel = sec,
                lastLogin = lastLogin
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Prefer explicit full-name fields from the API over generic `name` (often mirrors email). */
    private fun readDisplayName(p: JSONObject): String {
        val keys = listOf(
            "full_name", "fullName",
            "display_name", "displayName",
            "name", "username"
        )
        for (k in keys) {
            val v = p.optString(k).trim()
            if (v.isNotEmpty()) return v
        }
        return ""
    }

    private fun readUserId(p: JSONObject): String {
        if (!p.has("user_id") || p.isNull("user_id")) {
            return p.optString("userId", "")
        }
        return when (val u = p.get("user_id")) {
            is Number -> u.toLong().toString()
            else -> u.toString()
        }
    }

    private fun readLastLogin(p: JSONObject): Long {
        val keys = listOf("last_login", "lastLogin")
        for (k in keys) {
            if (!p.has(k) || p.isNull(k)) continue
            when (val v = p.get(k)) {
                is Number -> return v.toLong()
                is String -> return v.toLongOrNull() ?: 0L
            }
        }
        return 0L
    }
}
