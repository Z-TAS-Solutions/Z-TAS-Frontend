package com.example.z_tas.network

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════
// Registration
// ═══════════════════════════════════════════════════════════════

data class RegisterRequest(
    val name: String,
    val email: String,
    val nic: String,
    val phone: String
)

data class RegisterResponse(
    // Old/legacy shape (some backends returned this)
    @SerializedName(value = "message", alternate = ["detail"]) val message: String? = null,
    @SerializedName(value = "user_id", alternate = ["userId", "custom_id"]) val userId: String? = null,

    // Current backend shape (per API response):
    // { "data": { "success": true, "custom_id": "USR-..." }, "status": "success" }
    val data: RegisterResponseData? = null,
    val status: String? = null
)

data class RegisterResponseData(
    val success: Boolean? = null,
    @SerializedName("custom_id") val customId: String? = null
)

// ═══════════════════════════════════════════════════════════════
// OTP Verification
// ═══════════════════════════════════════════════════════════════

data class VerifyOtpRequest(
    // Use backend-friendly snake_case keys (and keep both id keys for compatibility)
    @SerializedName("custom_id") val customId: String,
    @SerializedName("user_id") val userId: String? = null,
    val otp: String
)

data class VerifyOtpResponse(
    @SerializedName(value = "message", alternate = ["detail"]) val message: String = "",
    @SerializedName(value = "verified", alternate = ["success"]) val verified: Boolean = false,
    val status: String? = null,
    val data: VerifyOtpResponseData? = null
)

data class VerifyOtpResponseData(
    @SerializedName(value = "success", alternate = ["verified"]) val success: Boolean = false,
    @SerializedName(value = "message", alternate = ["detail"]) val message: String? = null
)

// ═══════════════════════════════════════════════════════════════
// WebAuthn — Login (begin / finish)
// ═══════════════════════════════════════════════════════════════

data class BeginLoginRequest(
    val username: String
)

data class BeginLoginResponse(
    val challenge: String,
    @SerializedName(value = "rp_id", alternate = ["rpId"]) val rpId: String,
    @SerializedName(value = "allow_credentials", alternate = ["allowCredentials"]) val allowCredentials: List<AllowCredential>,
    val timeout: Long,
    @SerializedName(value = "user_verification", alternate = ["userVerification"]) val userVerification: String
)

data class AllowCredential(
    val id: String,
    val type: String
)

data class FinishLoginRequest(
    val id: String,
    val rawId: String,
    val response: AssertionResponseBody,
    val type: String = "public-key"
)

data class AssertionResponseBody(
    val authenticatorData: String,
    val clientDataJSON: String,
    val signature: String,
    val userHandle: String
)

data class FinishLoginResponse(
    val token: String,
    @SerializedName(value = "user_id", alternate = ["userId"]) val userId: String,
    val role: String
)

// ═══════════════════════════════════════════════════════════════
// WebAuthn — Register (begin / finish)
// ═══════════════════════════════════════════════════════════════

data class BeginRegisterRequest(
    @SerializedName("custom_id") val customId: String,
    @SerializedName("user_id") val userId: String? = null,
    val email: String
)

data class BeginRegisterResponse(
    val challenge: String,
    val rp: RelyingParty,
    val user: WebAuthnUser,
    @SerializedName(value = "pub_key_cred_params", alternate = ["pubKeyCredParams"]) val pubKeyCredParams: List<PubKeyCredParam>,
    val timeout: Long,
    val attestation: String
)

data class RelyingParty(
    val id: String,
    val name: String
)

data class WebAuthnUser(
    val id: String,
    val name: String,
    @SerializedName(value = "display_name", alternate = ["displayName"]) val displayName: String
)

data class PubKeyCredParam(
    val type: String,
    val alg: Int
)

data class FinishRegisterRequest(
    val id: String,
    val rawId: String,
    val response: AttestationResponseBody,
    val type: String = "public-key"
)

data class AttestationResponseBody(
    val attestationObject: String,
    val clientDataJSON: String
)

data class FinishRegisterResponse(
    @SerializedName(value = "message", alternate = ["detail"]) val message: String,
    @SerializedName(value = "credential_id", alternate = ["credentialId"]) val credentialId: String
)

// ═══════════════════════════════════════════════════════════════
// Sessions
// ═══════════════════════════════════════════════════════════════

data class SessionsResponse(
    val sessions: List<SessionData>,
    val pagination: Pagination
)

data class SessionData(
    @SerializedName("session_id")  val sessionId: String,
    @SerializedName("device_id")   val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    val location: String,
    @SerializedName("ip_address")  val ipAddress: String,
    @SerializedName("last_active") val lastActive: Long,
    val current: Boolean
)

data class Pagination(
    @SerializedName("has_more") val hasMore: Boolean
)

// ═══════════════════════════════════════════════════════════════
// Notifications
// ═══════════════════════════════════════════════════════════════

data class NotificationsResponse(
    val notifications: List<NotificationData>,
    val pagination: Pagination
)

data class NotificationData(
    @SerializedName("notification_id") val notificationId: String? = null,
    val title: String,
    val details: String,
    val timestamp: Long,
    val status: String   // "read" | "unread"
)

data class NotificationStatusRequest(
    val status: String   // "read" | "unread"
)

data class NotificationStatusResponse(
    val message: String,
    val data: NotificationStatusData
)

data class NotificationStatusData(
    @SerializedName("notification_id") val notificationId: String,
    val status: String
)

data class MarkAllReadResponse(
    val message: String,
    val data: MarkAllReadData
)

data class MarkAllReadData(
    @SerializedName("updated_count") val updatedCount: Int
)

// ═══════════════════════════════════════════════════════════════
// User Profile
// ═══════════════════════════════════════════════════════════════

data class UserProfileResponse(
    @SerializedName(value = "user_id", alternate = ["userId"]) val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val status: String,
    @SerializedName(value = "active_devices", alternate = ["activeDevices"]) val activeDevices: Int,
    @SerializedName(value = "security_level", alternate = ["securityLevel"]) val securityLevel: String,
    @SerializedName(value = "last_login", alternate = ["lastLogin"]) val lastLogin: Long
)

// ═══════════════════════════════════════════════════════════════
// Session Management
// ═══════════════════════════════════════════════════════════════

data class LogoutOthersResponse(
    val message: String,
    val data: LogoutOthersData
)

data class LogoutOthersData(
    @SerializedName("sessions_terminated") val sessionsTerminated: Int
)

data class ForceLogoutResponse(
    val message: String,
    val data: ForceLogoutData
)

data class ForceLogoutData(
    @SerializedName("user_id") val userId: String,
    @SerializedName("devices_terminated") val devicesTerminated: Int
)

// ═══════════════════════════════════════════════════════════════
// Delete Account
// ═══════════════════════════════════════════════════════════════

data class DeleteAccountRequest(
    val password: String
)

data class DeleteAccountResponse(
    val message: String
)
