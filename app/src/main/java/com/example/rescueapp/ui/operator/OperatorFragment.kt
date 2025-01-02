package com.example.rescueapp.ui.operator

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rescueapp.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class OperatorFragment : Fragment() {

    private lateinit var listenButton: MaterialButton
    private lateinit var timerTextView: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val client = OkHttpClient()

    private var isListening = false
    private var seconds = 0
    private var isTimerRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_operator, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        listenButton = rootView.findViewById(R.id.listenButton)
        timerTextView = rootView.findViewById(R.id.timerTextView)

        listenButton.visibility = View.GONE
        initializeTimer()
        setupButton()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkOperatorAccess(currentUser.uid)
        } else {
            navigateBack()
        }

        return rootView
    }

    private fun initializeTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    seconds++
                    updateTimerDisplay()
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun updateTimerDisplay() {
        val hours = TimeUnit.SECONDS.toHours(seconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
        val secs = seconds % 60
        val time = String.format("%02d:%02d:%02d", hours, minutes, secs)
        timerTextView.text = time
    }

    private fun startTimer() {
        isTimerRunning = true
        handler.post(timerRunnable)
    }

    private fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
        seconds = 0
        updateTimerDisplay()
    }

    private fun setupButton() {
        listenButton.setOnClickListener {
            if (!isListening) {
                startListening()
            } else {
                stopListening()
            }
        }
    }

    private fun startListening() {
        isListening = true
        listenButton.apply {
            text = "LISTENING"
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success))
        }
        startTimer()
        sendPostRequest("https://raspi.develop-er.org/run-script")
    }

    private fun stopListening() {
        isListening = false
        listenButton.apply {
            text = "START"
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
        }
        stopTimer()
        sendPostRequest("https://raspi.develop-er.org/stop-script")
    }

    private fun checkOperatorAccess(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.getString("role") == "Operator") {
                    listenButton.visibility = View.VISIBLE
                } else {
                    navigateBack()
                }
            }
            .addOnFailureListener { e ->
                Log.e("OperatorFragment", "Error checking operator access", e)
                navigateBack()
            }
    }

    private fun navigateBack() {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Access denied: Operator privileges required", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
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
                        Toast.makeText(requireContext(), "Request failed: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
    }
}