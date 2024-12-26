package com.example.rescueapp.ui.controller
import android.content.Context
import com.example.rescueapp.ui.models.DebrisClusterItem
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions

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

    override fun onBeforeClusterItemRendered(item: DebrisClusterItem, markerOptions: MarkerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        markerOptions
            .title(item.clusterTitle)
            .snippet(item.clusterSnippet)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
    }

}

