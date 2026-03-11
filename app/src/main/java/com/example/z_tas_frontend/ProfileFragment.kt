package com.example.z_tas_frontend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // sample user's data
        val userProfile = UserProfile(
            username = "Username",
            email = "for.example@gmail.com",
            status = "ACTIVE",
            activeDevices = 2,
            biometricEngineStatus = "ONLINE",
            lastSync = "3h ago",
            securityLevel = "HIGH",
        )

        // find the username TextView and set it
        view.findViewById<TextView>(R.id.username).text = userProfile.username

        view.findViewById<TextView>(R.id.useremail).text = userProfile.email

        // combining "STATUS: " with the actual status
        view.findViewById<TextView>(R.id.status).text = "STATUS: ${userProfile.status}"


        // active devices count
        view.findViewById<TextView>(R.id.activeDevicesCount).text =
            "${userProfile.activeDevices} devices connected"

        // biometric status
        view.findViewById<TextView>(R.id.biometricStatus).text =
            userProfile.biometricEngineStatus

        // last sync time
        view.findViewById<TextView>(R.id.lastSync).text = userProfile.lastSync


        // Change password click listener
        view.findViewById<LinearLayout>(R.id.changePasswordCard).setOnClickListener {
            showChangePasswordDialog()
        }

        // Active sessions click listener
        view.findViewById<LinearLayout>(R.id.activeSessionsCard).setOnClickListener {
            showActiveSessionsDialog()
        }

        // Sign out
        view.findViewById<LinearLayout>(R.id.signOutCard).setOnClickListener {
            showSignOutDialog()
        }

        // Delete account
        view.findViewById<LinearLayout>(R.id.deleteAccountCard).setOnClickListener {
            showDeleteAccount()
        }
    }  //change pass dialog/screen TBD


    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.change_pass_dialog,
            null
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set transparent background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_buttonid)
        val continueBtn = dialogView.findViewById<Button>(R.id.continue_buttonid)

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        continueBtn.setOnClickListener {
            showEmailSentDialog()
            dialog.dismiss()
        }

        dialog.show()
    }

    //consider fingerprint biometric

    private fun showEmailSentDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.email_sent_dialog,
            null
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set transparent background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val okBtn = dialogView.findViewById<Button>(R.id.ok_dialogbutton)

        okBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    //active sessions dialog box function
    private fun showActiveSessionsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.active_sessions_dialog,
            null
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set transparent background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val closeBtn = dialogView.findViewById<Button>(R.id.closesignout_id)
        val signOutOtherBtn = dialogView.findViewById<Button>(R.id.signout_otherid)

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        signOutOtherBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Other devices signed out", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    //sign out dialog

    private fun showSignOutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.signout_dialog,
            null
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set transparent background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_buttonid)
        val signOutBtn = dialogView.findViewById<Button>(R.id.signout_buttonid)

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        signOutBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Successfully signed out", Toast.LENGTH_SHORT).show()

        // Aftet this :- must navigate to login screen

        }

        dialog.show()
    }

    private fun showDeleteAccount() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.delete_account_dialog,
            null
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set transparent background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_button)
        val deleteBtn = dialogView.findViewById<Button>(R.id.dialog_delete_btn)

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        deleteBtn.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog()
        }

        dialog.show()
    }



    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Are you sure?")
            .setMessage("Press 'DELETE' to confirm permanent deletion.")
            .setPositiveButton("Delete") { dialog, which ->
                Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }
}