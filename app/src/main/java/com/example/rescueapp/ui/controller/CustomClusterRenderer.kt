package com.example.rescueapp.ui.controller
import android.content.Context
import com.example.rescueapp.ui.models.DebrisClusterItem
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

import android.os.Handler
import android.os.Looper

class CustomClusterRenderer(
    context: Context,
    private val googleMap: GoogleMap,
    clusterManager: ClusterManager<DebrisClusterItem>
) : DefaultClusterRenderer<DebrisClusterItem>(context, googleMap, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<DebrisClusterItem>): Boolean {
        var zoomLevel = 0f

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            zoomLevel = googleMap.cameraPosition.zoom
        }

        return cluster.size > 1 && zoomLevel < 15
    }
}

