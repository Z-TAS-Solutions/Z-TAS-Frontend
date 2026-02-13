//put package if needed

data class UserProfile (
    val username: String,
    val email: String,
    val status: String,
    val lastPasswordChange: String,
    val activeDevices: Int,
    val biometricEngineStatus: String,
    val lastSync: String,
    val securityLevel: String
)