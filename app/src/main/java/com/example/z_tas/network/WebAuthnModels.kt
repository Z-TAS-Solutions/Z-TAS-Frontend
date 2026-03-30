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
    val message: String,
    val userId: String
)

// ═══════════════════════════════════════════════════════════════
// OTP Verification
// ═══════════════════════════════════════════════════════════════

data class VerifyOtpRequest(
    val userId: String,
    val otp: String
)

data class VerifyOtpResponse(
    val message: String,
    val verified: Boolean
)

// ═══════════════════════════════════════════════════════════════
// WebAuthn — Login (begin / finish)
// ═══════════════════════════════════════════════════════════════

data class BeginLoginRequest(
    val username: String
)

data class BeginLoginResponse(
    val challenge: String,
    val rpId: String,
    val allowCredentials: List<AllowCredential>,
    val timeout: Long,
    val userVerification: String
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
    val userId: String,
    val role: String
)

// ═══════════════════════════════════════════════════════════════
// WebAuthn — Register (begin / finish)
// ═══════════════════════════════════════════════════════════════

data class BeginRegisterRequest(
    val userId: String
)

data class BeginRegisterResponse(
    val challenge: String,
    val rp: RelyingParty,
    val user: WebAuthnUser,
    val pubKeyCredParams: List<PubKeyCredParam>,
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
    val displayName: String
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
    val message: String,
    val credentialId: String
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
    val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val status: String,
    val activeDevices: Int,
    val securityLevel: String,
    val lastLogin: Long
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
