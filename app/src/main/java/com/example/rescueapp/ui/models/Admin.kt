package com.example.rescueapp.ui.models

import com.google.firebase.firestore.DocumentId

data class Admin(
    @DocumentId val id: String? = null,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    var phone: String = "",
    val role: String = "",
    val permissions: Permissions = Permissions(),
    var image: String? = null
) {
    data class Permissions(
        val editUsers: Boolean = false,
        val editDisasterData: Boolean = false,
        val adminDashboardAccess: Boolean = false,
        val activateListening: Boolean = false
    )
}
