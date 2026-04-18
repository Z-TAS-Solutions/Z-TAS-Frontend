package com.ztas.app.network

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════
// Registration
// ═══════════════════════════════════════════════════════════════

/**
 * POST `admin/users/register/new` — mirrors `dto.UserRegistrationDetailsRequest`.
 *
 * [role] must be a valid `database.UserRole` string: **`Admin`** or **`Client`** (see Z-QryptGIN `model_validation.go`).
 */
data class RegisterRequest(
    val name: String,
    val email: String,
    val nic: String,
    @SerializedName("phone_no") val phone: String,
    /** Default `Client` matches Go `RoleClient`; only `Admin` and `Client` pass `UserRole.Validate()`. */
    val role: String = "Client"
)

data class RegisterResponse(
    // Old/legacy shape (some backends returned this)
    @SerializedName(value = "message", alternate = ["detail"]) val message: String? = null,
    @SerializedName(value = "user_id", alternate = ["userId"]) val userId: String? = null,
    @SerializedName("custom_id") val customId: String? = null,

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

/**
 * POST `admin/users/register/resendOTP` — asks the server to send another registration OTP.
 * Backend may only require [customId]; [email] / [phone] help if the API matches on those fields.
 */
data class ResendOtpRequest(
    @SerializedName("custom_id") val customId: String,
    @SerializedName("user_id") val userId: String? = null,
    val email: String? = null,
    @SerializedName("phone_no") val phone: String? = null
)

// ═══════════════════════════════════════════════════════════════
// WebAuthn — Login (begin / finish)
// ═══════════════════════════════════════════════════════════════

/**
 * POST `webauthn/login/begin`.
 *
 * - **`username` omitted or null** → Gin receives empty `Username` → server runs **discoverable**
 *   login (`BeginDiscoverableLogin`). Use this when the passkey lives on the device (e.g. Samsung Pass).
 * - **`username` set** → server uses `FindByEmail(username)` then `BeginLogin(user)`; that path
 *   requires the API repo to **Preload("Credentials")** on the user, otherwise you get
 *   "Found no credentials for user" even when credentials exist in the DB.
 */
data class BeginLoginRequest(
    @SerializedName("username") val email: String? = null
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

// ═══════════════════════════════════════════════════════════════
// WebAuthn — Register (begin / finish)
// ═══════════════════════════════════════════════════════════════

data class BeginRegisterRequest(
    @SerializedName("custom_id") val customId: String,
    @SerializedName("user_id") val userId: String? = null,
    val email: String
)

data class BeginRegisterResponse(
    // Flat shape support
    val challenge: String? = null,
    val rp: RelyingParty? = null,
    val user: WebAuthnUser? = null,
    @SerializedName(value = "pub_key_cred_params", alternate = ["pubKeyCredParams"]) val pubKeyCredParams: List<PubKeyCredParam>? = null,
    val timeout: Long? = null,
    val attestation: String? = null,

    // Wrapped shape support:
    // { "data": { "creation_data": { "publicKey": { ... } } }, "status": "success" }
    val data: BeginRegisterResponseData? = null,
    val status: String? = null
)

data class BeginRegisterResponseData(
    @SerializedName("session_token") val sessionToken: String? = null,
    @SerializedName("creation_data") val creationData: BeginRegisterCreationData? = null
)

data class BeginRegisterCreationData(
    @SerializedName("publicKey") val publicKey: BeginRegisterPublicKeyOptions? = null
)

data class BeginRegisterPublicKeyOptions(
    val challenge: String? = null,
    val rp: RelyingParty? = null,
    val user: WebAuthnUser? = null,
    @SerializedName(value = "pub_key_cred_params", alternate = ["pubKeyCredParams"]) val pubKeyCredParams: List<PubKeyCredParam>? = null,
    val timeout: Long? = null,
    val attestation: String? = null,
    @SerializedName(value = "authenticator_selection", alternate = ["authenticatorSelection"]) val authenticatorSelection: AuthenticatorSelection? = null,
    val hints: List<String>? = null
)

data class AuthenticatorSelection(
    @SerializedName(value = "require_resident_key", alternate = ["requireResidentKey"]) val requireResidentKey: Boolean? = null,
    @SerializedName(value = "resident_key", alternate = ["residentKey"]) val residentKey: String? = null,
    @SerializedName(value = "user_verification", alternate = ["userVerification"]) val userVerification: String? = null
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

// ═══════════════════════════════════════════════════════════════
// Utility — ping
// ═══════════════════════════════════════════════════════════════

data class PingResponse(
    val message: String,
    val data: PingData
)

data class PingData(
    @SerializedName("server_timestamp") val serverTimestamp: Long
)

// ═══════════════════════════════════════════════════════════════
// Admin — dashboard
// ═══════════════════════════════════════════════════════════════

data class DashboardAnalyticsResponse(
    val totalUsers: Int,
    val activeSessions: Int,
    val successRate: Double,
    val failedRate: Double,
    val suspiciousActivity: Int
)

data class AuthTrendsResponse(
    val interval: String,
    val data: List<AuthTrendPoint>
)

data class AuthTrendPoint(
    val timestamp: String,
    val successCount: Int,
    val failureCount: Int
)

data class RecentAuthActivityResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val data: List<RecentAuthActivityItem>
)

data class RecentAuthActivityItem(
    val userId: String,
    val device: String,
    val method: String,
    val status: String,
    val timestamp: String
)

// ═══════════════════════════════════════════════════════════════
// Admin — users
// ═══════════════════════════════════════════════════════════════

data class AdminUsersListResponse(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val data: List<AdminUserRow>
)

data class AdminUserRow(
    val userId: String,
    val name: String,
    val email: String,
    val phone: String?,
    val mfaEnabled: Boolean,
    val lastLogin: String?,
    val status: String
)

data class LockStatusRequest(
    val locked: Boolean
)

data class LockStatusResponse(
    val message: String,
    val data: LockStatusData
)

data class LockStatusData(
    val userId: String,
    val locked: Boolean
)

// ═══════════════════════════════════════════════════════════════
// Admin — devices
// ═══════════════════════════════════════════════════════════════

data class AdminDevicesEnvelope(
    val message: String,
    val data: AdminDevicesPayload
)

data class AdminDevicesPayload(
    val devices: List<AdminDeviceRow>,
    val pagination: AdminOffsetPagination
)

data class AdminDeviceRow(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    val location: String,
    @SerializedName("last_active") val lastActive: Long
)

data class AdminOffsetPagination(
    val limit: Int,
    val offset: Int,
    val returned: Int,
    @SerializedName("has_more") val hasMore: Boolean
)

data class AdminForceDeviceLogoutResponse(
    val message: String,
    val data: AdminForceDeviceLogoutData
)

data class AdminForceDeviceLogoutData(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("logged_out") val loggedOut: Boolean
)

// ═══════════════════════════════════════════════════════════════
// Admin — security
// ═══════════════════════════════════════════════════════════════

data class MfaEnforcementRequest(
    val enabled: Boolean
)

data class MfaEnforcementResponse(
    val message: String,
    val data: MfaEnforcementData
)

data class MfaEnforcementData(
    val enabled: Boolean
)

// ═══════════════════════════════════════════════════════════════
// Admin — auth logs & analytics
// ═══════════════════════════════════════════════════════════════

data class AuthLogsEnvelope(
    val message: String,
    val data: AuthLogsPayload
)

data class AuthLogsPayload(
    val logs: List<AuthLogRow>,
    val pagination: AdminOffsetPagination
)

data class AuthLogRow(
    @SerializedName("log_id") val logId: String,
    val timestamp: Long,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_id") val userId: String,
    val status: String,
    val location: String,
    val device: String
)

data class AuthAnalyticsEnvelope(
    val message: String,
    val data: AuthAnalyticsPayload
)

data class AuthAnalyticsPayload(
    @SerializedName("time_range") val timeRange: AuthAnalyticsTimeRange,
    val metrics: AuthAnalyticsMetrics
)

data class AuthAnalyticsTimeRange(
    val from: Long,
    val to: Long
)

data class AuthAnalyticsMetrics(
    @SerializedName("successful_logins") val successfulLogins: Int,
    @SerializedName("failed_logins") val failedLogins: Int,
    @SerializedName("suspicious_activities") val suspiciousActivities: Int
)
