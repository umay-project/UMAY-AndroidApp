package com.example.rescueapp.ui.models

import com.google.firebase.firestore.DocumentId

data class Operator(
    @DocumentId val id: String? = null,
    val surname: String = "",
    val email: String = "",
    var phone: String = "",
    val role: String = "",
    val permissions: Permissions = Permissions(),
    val listeningMode: Boolean = false,
    val requestUrls: RequestUrls = RequestUrls(),
    val image: String? = null
) {
    data class Permissions(
        val editUsers: Boolean = false,
        val editDisasterData: Boolean = false,
        val adminDashboardAccess: Boolean = false,
        val activateListening: Boolean = false
    )

    data class RequestUrls(
        val startListeningUrl: String = "",
        val stopListeningUrl: String = ""
    )
}
