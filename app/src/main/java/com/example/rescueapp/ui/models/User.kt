package com.example.rescueapp.ui.models

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String? = null,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    var phone: String = "",
    val role: String = "",
    val permissions: Permissions = Permissions(),
    var teamName: String? = null,
    var image: String? = null
) {
    data class Permissions(
        var editUsers: Boolean = false,
        var editDisasterData: Boolean = false,
        var adminDashboardAccess: Boolean = false,
        var activateListening: Boolean = false
    )
}
