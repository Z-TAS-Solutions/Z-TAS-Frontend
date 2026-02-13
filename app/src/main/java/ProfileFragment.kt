import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ztas_frontend_rafa.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // This converts the XML layout into actual UI objects
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create sample user data
        val userProfile = UserProfile(
            username = "Username",
            email = "for.example@gmail.com",
            status = "ACTIVE",
            lastPasswordChange = "25 days ago",
            activeDevices = 2,
            biometricEngineStatus = "ONLINE",
            lastSync = "3h ago",
        )

        // Find the username TextView and set its text
        view.findViewById<TextView>(R.id.username).text = userProfile.username

        // Find the email TextView and set its text
        view.findViewById<TextView>(R.id.useremail).text = userProfile.email

        // Combine "STATUS: " with the actual status
        view.findViewById<TextView>(R.id.status).text = "STATUS: ${userProfile.status}"

        // Set password change info
        view.findViewById<TextView>(R.id.lastPasswordChange).text =
            "Last changed ${userProfile.lastPasswordChange}"

        // Set active devices count
        view.findViewById<TextView>(R.id.activeDevicesCount).text =
            "${userProfile.activeDevices} devices connected"

        // Set biometric status
        view.findViewById<TextView>(R.id.biometricStatus).text =
            userProfile.biometricEngineStatus

        // Set last sync time
        view.findViewById<TextView>(R.id.lastSync).text = userProfile.lastSync


        // Handle Change Password click
        view.findViewById<LinearLayout>(R.id.changePasswordCard).setOnClickListener {
            // TODO: Navigate to change password screen
            // Later you'll add: val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            // startActivity(intent)
        }

        // Handle Active Sessions click
        view.findViewById<LinearLayout>(R.id.activeSessionsCard).setOnClickListener {
            // TODO: Navigate to active sessions screen
        }

        // Handle Sign Out click
        view.findViewById<LinearLayout>(R.id.signOutCard).setOnClickListener {
            // TODO: Handle sign out
        }

        // Handle Delete Account click
        view.findViewById<LinearLayout>(R.id.deleteAccountCard).setOnClickListener {
            // TODO: Show delete confirmation dialog
        }
    }
}