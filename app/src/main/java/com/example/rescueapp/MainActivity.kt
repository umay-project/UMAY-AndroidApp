package com.example.rescueapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.rescueapp.databinding.ActivityMainBinding
import com.example.rescueapp.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var navView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navView = binding.navView

        // Handle authentication state
        if (auth.currentUser == null) {
            startLoginActivity()
            return
        }

        setupNavigation()

        // Check user role after navigation is set up
        auth.currentUser?.let { user ->
            checkUserRoles(user.uid)
        }
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Update AppBarConfiguration to include all top-level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_profile,
                R.id.navigation_operator,
                R.id.navigation_admin_dashboard
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun checkUserRoles(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val role = document.getString("role")
                    val permissions = document.get("permissions") as? Map<String, Boolean>
                    val menu = navView.menu

                    // Handle Operator Role
                    if (role == "Operator") {
                        if (menu.findItem(R.id.navigation_operator) == null) {
                            menu.add(
                                Menu.NONE,
                                R.id.navigation_operator,
                                Menu.NONE,
                                "Operator"
                            ).setIcon(R.drawable.operator)
                        }
                    }

                    // Handle Admin Role
                    if (role == "Admin") {
                        val adminMenuItem = menu.findItem(R.id.navigation_admin_dashboard)
                            ?: menu.add(
                                Menu.NONE,
                                R.id.navigation_admin_dashboard,
                                Menu.NONE,
                                "Admin"
                            ).setIcon(R.drawable.ic_admin)

                        // Check admin permissions
                        val hasAdminAccess = permissions?.get("adminDashboardAccess") ?: false
                        adminMenuItem.isVisible = hasAdminAccess
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error checking user roles", e)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun logout() {
        auth.signOut()
        startLoginActivity()
    }
}