package com.example.rescueapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rescueapp.R
import com.example.rescueapp.ui.controller.api
import com.example.rescueapp.ui.models.DebrisSite
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardFragment : Fragment() {
    private lateinit var manageUsersCard: CardView
    private lateinit var manageFalseTaggedCard: CardView
    private lateinit var totalUsersCount: TextView
    private lateinit var falseTaggedCount: TextView
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

        manageUsersCard = view.findViewById(R.id.manageUsersCard)
        manageFalseTaggedCard = view.findViewById(R.id.manageFalseTaggedCard)
        totalUsersCount = view.findViewById(R.id.totalUsersCount)
        falseTaggedCount = view.findViewById(R.id.falseTaggedCount)

        manageUsersCard.visibility = View.GONE
        manageFalseTaggedCard.visibility = View.GONE

        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRole(currentUser.uid)
        } else {
            Toast.makeText(requireContext(), "User not authenticated!", Toast.LENGTH_SHORT).show()
        }

        setupClickListeners()
        loadStatistics()

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
                            manageUsersCard.visibility = if (canEditUsers) View.VISIBLE else View.GONE
                            manageFalseTaggedCard.visibility = if (canEditDisasterData) View.VISIBLE else View.GONE
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

    private fun setupClickListeners() {
        manageUsersCard.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_userManagement)
        }

        manageFalseTaggedCard.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_falseTaggedManagement)
        }
    }

    private fun loadStatistics() {
        db.collection("users").get()
            .addOnSuccessListener { documents ->
                totalUsersCount.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Statistics", "Error getting users count", e)
                totalUsersCount.text = "?"
            }
        loadFalseTaggedCount()
    }

    private fun loadFalseTaggedCount() {
        api.getFalseTagged().enqueue(object : retrofit2.Callback<List<DebrisSite>> {
            override fun onResponse(
                call: retrofit2.Call<List<DebrisSite>>,
                response: retrofit2.Response<List<DebrisSite>>
            ) {
                if (response.isSuccessful) {
                    val count = response.body()?.size ?: 0
                    activity?.runOnUiThread {
                        falseTaggedCount.text = count.toString()
                    }
                } else {
                    Log.e("Statistics", "Error: ${response.code()}")
                    activity?.runOnUiThread {
                        falseTaggedCount.text = "?"
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<List<DebrisSite>>, t: Throwable) {
                Log.e("Statistics", "Error getting false tagged count", t)
                activity?.runOnUiThread {
                    falseTaggedCount.text = "?"
                }
            }
        })
    }

    private fun showAccessDenied() {
        Toast.makeText(requireContext(), "Access denied: Admin privileges required", Toast.LENGTH_SHORT).show()
        manageUsersCard.visibility = View.GONE
        manageFalseTaggedCard.visibility = View.GONE
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}