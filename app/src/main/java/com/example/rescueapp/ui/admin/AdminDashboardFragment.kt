package com.example.rescueapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rescueapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardFragment : Fragment() {
    private lateinit var manageUsersButton: Button
    private lateinit var manageFalseTaggedButton: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        manageUsersButton = view.findViewById(R.id.manageUsersButton)
        manageFalseTaggedButton = view.findViewById(R.id.manageFalseTaggedButton)

        manageUsersButton.visibility = View.GONE
        manageFalseTaggedButton.visibility = View.GONE

        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRole(currentUser.uid)
        } else {
            Toast.makeText(requireContext(), "User not authenticated!", Toast.LENGTH_SHORT).show()
        }

        setupButtons()

        return view
    }

    private fun checkUserRole(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val role = document.getString("role")
                    val permissions = document.get("permissions") as? Map<String, Boolean>

                    if (role == "Admin") {
                        val canEditUsers = permissions?.get("editUsers") ?: false
                        val canEditDisasterData = permissions?.get("editDisasterData") ?: false
                        val hasAdminAccess = permissions?.get("adminDashboardAccess") ?: false

                        if (hasAdminAccess) {
                            manageUsersButton.visibility = if (canEditUsers) View.VISIBLE else View.GONE
                            manageFalseTaggedButton.visibility = if (canEditDisasterData) View.VISIBLE else View.GONE
                        } else {
                            showAccessDenied()
                        }
                    } else {
                        showAccessDenied()
                    }
                } else {
                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching user role", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtons() {
        manageUsersButton.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_userManagement)
        }

        manageFalseTaggedButton.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_falseTaggedManagement)
        }
    }

    private fun showAccessDenied() {
        Toast.makeText(requireContext(), "Access denied: Admin privileges required", Toast.LENGTH_SHORT).show()
        manageUsersButton.visibility = View.GONE
        manageFalseTaggedButton.visibility = View.GONE
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}