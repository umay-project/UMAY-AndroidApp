package com.example.rescueapp.ui.models

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class DebrisClusterItem(
    val id: String,
    private val latLng: LatLng,
    private val clusterTitle: String,
    private val clusterSnippet: String
) : ClusterItem {

    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = clusterTitle
    override fun getSnippet(): String = clusterSnippet

    override fun getZIndex(): Float? = 0f
}
