package com.example.rescueapp.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.rescueapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var nameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var permissionsContainer: LinearLayout
    private lateinit var activateListeningCheckBox: CheckBox

    private lateinit var saveButton: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        nameEditText = rootView.findViewById(R.id.nameEditText)
        surnameEditText = rootView.findViewById(R.id.surnameEditText)
        emailEditText = rootView.findViewById(R.id.emailEditText)
        phoneEditText = rootView.findViewById(R.id.phoneEditText)
        permissionsContainer = rootView.findViewById(R.id.permissionsContainer)
        activateListeningCheckBox = rootView.findViewById(R.id.activateListeningCheckBox)
        saveButton = rootView.findViewById(R.id.saveButton)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserProfile(currentUser.uid)
        }

        saveButton.setOnClickListener {
            saveUserProfile()
        }

        return rootView
    }

    private fun loadUserProfile(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val role = document.getString("role")
                    nameEditText.setText(document.getString("name"))
                    surnameEditText.setText(document.getString("surname"))
                    emailEditText.setText(document.getString("email"))
                    phoneEditText.setText(document.getString("phone"))

                    if (role == "Admin") {
                        permissionsContainer.visibility = View.VISIBLE
                        activateListeningCheckBox.isChecked =
                            document.get("permissions.activateListening") as? Boolean ?: false
                    } else {
                        permissionsContainer.visibility = View.GONE
                    }

                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error loading user profile", e)
            }
    }

    private fun saveUserProfile() {
        val updates = hashMapOf<String, Any>(
            "name" to nameEditText.text.toString(),
            "surname" to surnameEditText.text.toString(),
            "phone" to phoneEditText.text.toString()
        )

        if (permissionsContainer.visibility == View.VISIBLE) {
            updates["permissions.activateListening"] = activateListeningCheckBox.isChecked
        }

        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error updating user profile", e)
            }
    }
}
