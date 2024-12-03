package com.example.rescueapp.ui.operator

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.rescueapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import java.io.IOException

class OperatorFragment : Fragment() {

    private lateinit var startListeningButton: Button
    private lateinit var stopListeningButton: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_operator, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        startListeningButton = rootView.findViewById(R.id.startListeningButton)
        stopListeningButton = rootView.findViewById(R.id.stopListeningButton)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            checkUserRole(userId)
        } else {
            Toast.makeText(requireContext(), "User not authenticated!", Toast.LENGTH_SHORT).show()
        }

        startListeningButton.setOnClickListener {
            sendPostRequest("https://raspi.develop-er.org/run-script")
        }

        stopListeningButton.setOnClickListener {
            sendPostRequest("https://raspi.develop-er.org/stop-script")
        }

        return rootView
    }

    private fun checkUserRole(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val role = document.getString("role")
                    if (role == "Operator") {
                        // User is an operator, enable buttons
                        startListeningButton.visibility = View.VISIBLE
                        stopListeningButton.visibility = View.VISIBLE
                    } else {
                        // User is not an operator
                        Toast.makeText(requireContext(), "Access denied: Not an operator", Toast.LENGTH_SHORT).show()
                        startListeningButton.visibility = View.GONE
                        stopListeningButton.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching user role", e)
            }
    }

    private fun sendPostRequest(url: String) {
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "Request failed: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Request failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Request successful!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Request failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }
}
