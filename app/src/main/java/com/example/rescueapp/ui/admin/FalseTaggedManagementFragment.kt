package com.example.rescueapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescueapp.R
import com.example.rescueapp.ui.controller.api
import com.example.rescueapp.ui.models.DebrisSite
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class FalseTaggedManagementFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FalseTaggedAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_false_tagged, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.falseTaggedRecyclerView)
        recyclerView.visibility = View.GONE

        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserPermissions(currentUser.uid)
        } else {
            Toast.makeText(requireContext(), "User not authenticated!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        return view
    }

    private fun checkUserPermissions(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val role = document.getString("role")
                    val permissions = document.get("permissions") as? Map<String, Boolean>

                    if (role == "Admin" && permissions?.get("editDisasterData") == true) {
                        recyclerView.visibility = View.VISIBLE
                        setupRecyclerView()
                        loadFalseTaggedEntries()
                    } else {
                        Toast.makeText(requireContext(), "Access denied: Insufficient permissions", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error checking permissions", e)
                findNavController().navigateUp()
            }
    }

    private fun setupRecyclerView() {
        adapter = FalseTaggedAdapter(
            onPlayClick = { fileName ->
                playAudio(fileName)
            },
            onDeleteClick = { fileName ->
                deleteEntry(fileName)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadFalseTaggedEntries() {
        api.getFalseTagged().enqueue(object : Callback<List<DebrisSite>> {
            override fun onResponse(call: Call<List<DebrisSite>>, response: Response<List<DebrisSite>>) {
                if (response.isSuccessful) {
                    adapter.updateEntries(response.body() ?: emptyList())
                }
            }
            override fun onFailure(call: Call<List<DebrisSite>>, t: Throwable) {
            }
        })
    }

    private fun deleteEntry(fileName: String) {
        api.deleteEntry(fileName).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    loadFalseTaggedEntries()
                }
            }
            override fun onFailure(call: Call<Unit>, t: Throwable) {
            }
        })
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun playAudio(fileName: String) {
        val sanitizedFileName = if (fileName.endsWith(".wav")) {
            fileName.substringBeforeLast(".wav")
        } else {
            fileName
        }

        val audioUrl = "http://umay.develop-er.org/get-audio?fileName=$sanitizedFileName"

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()

        try {
            mediaPlayer?.apply {
                setDataSource(audioUrl)
                setOnPreparedListener {
                    it.start()
                    Toast.makeText(requireContext(), "Playing audio: $sanitizedFileName", Toast.LENGTH_SHORT).show()
                }
                setOnCompletionListener {
                    Toast.makeText(requireContext(), "Audio playback completed", Toast.LENGTH_SHORT).show()
                    release()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayer", "Error occurred: what=$what, extra=$extra")
                    Toast.makeText(requireContext(), "Error playing audio", Toast.LENGTH_SHORT).show()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error initializing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}