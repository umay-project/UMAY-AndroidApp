package com.example.rescueapp.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescueapp.R
import com.example.rescueapp.ui.models.User
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class UserManagementFragment : Fragment() {
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserManagementAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val users = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_list, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.userRecyclerView)

        searchView.visibility = View.GONE
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

                    if (role == "Admin" && permissions?.get("editUsers") == true) {
                        searchView.visibility = View.VISIBLE
                        recyclerView.visibility = View.VISIBLE
                        setupRecyclerView()
                        setupSearchView()
                        loadUsers()
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
        adapter = UserManagementAdapter(users) { user ->
            showEditUserDialog(user)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText)
                return true
            }
        })
    }

    private fun loadUsers() {
        FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener { documents ->
                users.clear()
                for (document in documents) {
                    document.toObject(User::class.java)?.let {
                        users.add(it)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun filterUsers(query: String?) {
        if (query.isNullOrEmpty()) {
            loadUsers()
        } else {
            val filtered = users.filter { user ->
                user.name.contains(query, true) ||
                        user.email.contains(query, true) ||
                        user.phone.contains(query, true)
            }
            adapter.updateUsers(filtered)
        }
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_user, null)

        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val surnameInput = dialogView.findViewById<TextInputEditText>(R.id.surnameInput)
        val phoneInput = dialogView.findViewById<TextInputEditText>(R.id.phoneInput)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.roleSpinner)
        val editUsersCheck = dialogView.findViewById<CheckBox>(R.id.editUsersCheck)
        val editDisasterDataCheck = dialogView.findViewById<CheckBox>(R.id.editDisasterDataCheck)
        val adminDashboardCheck = dialogView.findViewById<CheckBox>(R.id.adminDashboardCheck)
        val activateListeningCheck = dialogView.findViewById<CheckBox>(R.id.activateListeningCheck)

        nameInput.setText(user.name)
        surnameInput.setText(user.surname)
        phoneInput.setText(user.phone)

        val roles = arrayOf("User", "Operator", "Admin")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter
        roleSpinner.setSelection(roles.indexOf(user.role))

        editUsersCheck.isChecked = user.permissions.editUsers
        editDisasterDataCheck.isChecked = user.permissions.editDisasterData
        adminDashboardCheck.isChecked = user.permissions.adminDashboardAccess
        activateListeningCheck.isChecked = user.permissions.activateListening

        AlertDialog.Builder(requireContext())
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedUser = user.copy(
                    name = nameInput.text.toString(),
                    surname = surnameInput.text.toString(),
                    phone = phoneInput.text.toString(),
                    role = roleSpinner.selectedItem.toString(),
                    permissions = User.Permissions(
                        editUsers = editUsersCheck.isChecked,
                        editDisasterData = editDisasterDataCheck.isChecked,
                        adminDashboardAccess = adminDashboardCheck.isChecked,
                        activateListening = activateListeningCheck.isChecked
                    )
                )
                updateUser(updatedUser)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUser(user: User) {
        user.id?.let { userId ->
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "User updated successfully", Toast.LENGTH_SHORT).show()
                    loadUsers() // Refresh the list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to update user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}