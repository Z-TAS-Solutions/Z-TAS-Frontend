package com.ztas.app

import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.FragmentActivity
import com.ztas.app.network.AssertionResponseBody
import com.ztas.app.network.BeginLoginRequest
import com.ztas.app.network.FinishLoginRequest
import com.ztas.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Shared WebAuthn passkey login: [RetrofitClient.webAuthnApi] `login/begin` → CredentialManager → `login/finish`.
 * Used by [LoginActivity] and for step-up confirmation (e.g. account deletion) in [ProfileActivity].
 */
object WebAuthnLoginFlow {

    private const val TAG = "WebAuthnLoginFlow"

    private val webAuthnApi = RetrofitClient.webAuthnApi

    data class FinishData(
        val token: String,
        val userId: String,
        val role: String,
        val email: String,
        val displayName: String = ""
    )

    sealed class PasskeyOutcome {
        data class Success(val data: FinishData) : PasskeyOutcome()
        data class Error(val message: String) : PasskeyOutcome()
    }

    /**
     * @param emailOrNull registered email, or null for discoverable credential (device picker).
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        credentialManager: CredentialManager,
        emailOrNull: String?
    ): PasskeyOutcome {
        return try {
            val beginRequest = when (val e = emailOrNull?.trim().orEmpty()) {
                "" -> BeginLoginRequest(email = null)
                else -> BeginLoginRequest(email = e)
            }

            val beginResponse = withContext(Dispatchers.IO) {
                webAuthnApi.beginLogin(beginRequest)
            }

            if (!beginResponse.isSuccessful || beginResponse.body() == null) {
                val errorBody = beginResponse.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Begin login failed: ${beginResponse.code()} — $errorBody")
                val userMessage = when {
                    beginResponse.code() == 401 &&
                        errorBody.contains("User not found", ignoreCase = true) ->
                        "No account for this email. Use the email you registered with."
                    errorBody.contains("Found no credentials", ignoreCase = true) ||
                        errorBody.contains("no credentials for user", ignoreCase = true) ->
                        "No passkey for this account yet. Finish sign-up: verify email, then complete passkey setup before logging in."
                    else -> "Login failed: $errorBody"
                }
                return PasskeyOutcome.Error(userMessage)
            }

            val beginJson = beginResponse.body()!!.string()
            val parsedBegin = parseLoginBeginPayload(beginJson)
            if (parsedBegin == null) {
                Log.e(TAG, "Could not parse login/begin JSON: $beginJson")
                val hint = serverMessageFromJson(beginJson)
                    .ifEmpty { "Could not read passkey options from server. Check Logcat tag $TAG for the raw JSON." }
                return PasskeyOutcome.Error("Login failed: $hint")
            }

            val requestJson = buildGetCredentialJson(parsedBegin)
            Log.d(TAG, "CredentialManager request JSON: $requestJson")

            val getOption = GetPublicKeyCredentialOption(requestJson)
            val getRequest = GetCredentialRequest(listOf(getOption))

            val result = withContext(Dispatchers.Main.immediate) {
                credentialManager.getCredential(activity, getRequest)
            }

            val credential = result.credential
            if (credential !is PublicKeyCredential) {
                return PasskeyOutcome.Error("Unexpected credential type")
            }

            val assertionJson = JSONObject(credential.authenticationResponseJson)
            Log.d(TAG, "Assertion JSON: $assertionJson")

            val responseObj = assertionJson.getJSONObject("response")

            val finishRequest = FinishLoginRequest(
                id = assertionJson.getString("id"),
                rawId = assertionJson.getString("rawId"),
                response = AssertionResponseBody(
                    authenticatorData = responseObj.getString("authenticatorData"),
                    clientDataJSON = responseObj.getString("clientDataJSON"),
                    signature = responseObj.getString("signature"),
                    userHandle = responseObj.optString("userHandle", "")
                ),
                type = assertionJson.optString("type", "public-key")
            )

            val finishResponse = withContext(Dispatchers.IO) {
                webAuthnApi.finishLogin(
                    sessionToken = parsedBegin.sessionToken,
                    request = finishRequest
                )
            }

            if (!finishResponse.isSuccessful || finishResponse.body() == null) {
                val errorBody = finishResponse.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Finish login failed: ${finishResponse.code()} — $errorBody")
                return PasskeyOutcome.Error("Login verification failed: $errorBody")
            }

            val finishJson = finishResponse.body()!!.string()
            val finishData = parseLoginFinishPayload(finishJson)
            if (finishData == null) {
                Log.e(TAG, "Could not parse login/finish JSON: $finishJson")
                val hint = serverMessageFromJson(finishJson)
                    .ifEmpty { "Could not read token from server. Check Logcat tag $TAG for the raw JSON." }
                return PasskeyOutcome.Error("Login failed: $hint")
            }

            Log.d(TAG, "Passkey auth success — userId=${finishData.userId}, role=${finishData.role}")
            PasskeyOutcome.Success(finishData)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential retrieval failed", e)
            PasskeyOutcome.Error("Passkey authentication failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during passkey auth", e)
            PasskeyOutcome.Error("An error occurred: ${e.message}")
        }
    }

    fun bearerHeaderForToken(rawToken: String): String {
        val t = rawToken.trim()
        return if (t.startsWith("Bearer ", ignoreCase = true)) t else "Bearer $t"
    }

    private fun buildGetCredentialJson(begin: ParsedLoginBegin): String {
        val json = JSONObject()
        json.put("challenge", begin.challenge)
        json.put("rpId", begin.rpId)
        json.put("timeout", begin.timeout)
        json.put("userVerification", begin.userVerification)
        json.put("allowCredentials", begin.allowCredentials)
        return json.toString()
    }

    private data class ParsedLoginBegin(
        val sessionToken: String,
        val challenge: String,
        val rpId: String,
        val timeout: Long,
        val userVerification: String,
        val allowCredentials: JSONArray
    )

    private fun serverMessageFromJson(raw: String): String {
        return runCatching {
            JSONObject(raw).optString("message").trim()
        }.getOrDefault("")
    }

    private fun parseLoginBeginPayload(raw: String): ParsedLoginBegin? {
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONObject("data")
            val sessionToken = sequenceOf(
                data?.optString("session_token").orEmpty(),
                data?.optString("sessionToken").orEmpty(),
                root.optString("session_token"),
                root.optString("sessionToken")
            ).firstOrNull { it.isNotEmpty() } ?: ""
            if (sessionToken.isEmpty()) return null

            val optionRoots = mutableListOf<JSONObject>()
            fun add(obj: JSONObject?) {
                if (obj != null) optionRoots.add(obj)
            }
            data?.let { d ->
                add(d.optJSONObject("assertion_data"))
                add(d.optJSONObject("assertionData"))
                add(d.optJSONObject("assertion"))
                add(d.optJSONObject("publicKey"))
                add(d.optJSONObject("options"))
                for (key in arrayOf("assertion_data", "assertionData")) {
                    val s = d.optString(key)
                    if (s.isNotBlank() && s.trimStart().startsWith("{")) {
                        runCatching { add(JSONObject(s)) }
                    }
                }
            }
            add(root.optJSONObject("assertion_data"))
            add(root.optJSONObject("publicKey"))

            for (assertion in optionRoots) {
                val options = assertion.optJSONObject("response")
                    ?: assertion.optJSONObject("publicKey")
                    ?: assertion
                parsedBeginFromWebAuthnOptions(options, sessionToken)?.let { return it }
            }
            data?.let { parsedBeginFromWebAuthnOptions(it, sessionToken) }
        } catch (e: Exception) {
            Log.e(TAG, "parseLoginBeginPayload", e)
            null
        }
    }

    private fun parsedBeginFromWebAuthnOptions(
        options: JSONObject,
        sessionToken: String
    ): ParsedLoginBegin? {
        if (sessionToken.isEmpty()) return null
        val challenge = options.optString("challenge").ifEmpty { options.optString("Challenge") }
        if (challenge.isEmpty()) return null
        var rpId = options.optString("rpId").ifEmpty { options.optString("rp_id") }
        if (rpId.isEmpty()) {
            rpId = options.optJSONObject("rp")?.optString("id").orEmpty()
        }
        if (rpId.isEmpty()) return null
        val timeout = when {
            options.has("timeout") && !options.isNull("timeout") ->
                options.optLong("timeout", 120_000L).takeIf { it > 0 }
                    ?: options.optInt("timeout", 120_000).toLong()
            else -> 120_000L
        }
        val uv = options.optString("userVerification").ifEmpty {
            options.optString("user_verification")
        }.ifEmpty { "required" }
        val allow = options.optJSONArray("allowCredentials")
            ?: options.optJSONArray("allow_credentials")
            ?: JSONArray()
        return ParsedLoginBegin(sessionToken, challenge, rpId, timeout, uv, allow)
    }

    private fun parseLoginFinishPayload(raw: String): FinishData? {
        return try {
            val root = JSONObject(raw)
            val data = root.optJSONObject("data") ?: root
            var token = data.optString("token").ifEmpty { data.optString("access_token") }
                .ifEmpty { data.optString("accessToken") }
            if (token.isEmpty()) {
                token = data.optJSONObject("data")?.optString("token").orEmpty()
                    .ifEmpty { data.optJSONObject("data")?.optString("access_token").orEmpty() }
            }
            if (token.isEmpty()) return null
            val userId = when {
                data.has("user_id") && !data.isNull("user_id") && data.get("user_id") is Number ->
                    data.getLong("user_id").toString()
                else -> data.optString("user_id").ifEmpty { data.optString("userId") }
            }
            val role = data.optString("role").ifEmpty { "Client" }
            val email = data.optString("email")
            val displayName = sequenceOf(
                data.optString("full_name"),
                data.optString("fullName"),
                data.optString("display_name"),
                data.optString("displayName"),
                data.optString("name")
            ).map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
            FinishData(token, userId, role, email, displayName)
        } catch (e: Exception) {
            Log.e(TAG, "parseLoginFinishPayload", e)
            null
        }
    }
}
