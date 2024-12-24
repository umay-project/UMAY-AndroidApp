package com.example.rescueapp.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rescueapp.R
import com.example.rescueapp.ui.controller.api
import com.example.rescueapp.ui.dashboard.DashboardViewModel
import com.example.rescueapp.ui.models.DebrisClusterItem
import com.example.rescueapp.ui.models.DebrisSite
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.maps.android.clustering.ClusterManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal
import java.math.RoundingMode

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var clusterManager: ClusterManager<DebrisClusterItem>
    private lateinit var dashboardViewModel: DashboardViewModel
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var API_KEY = "AIzaSyBwwAKgOZxVsEHn6fMVAbkObDpopTdxWXY"
    private var lastQueryTime: Long = 0
    private val QUERY_DELAY = 1000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), API_KEY)
        }

        dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        val autocompleteFragment = AutocompleteSupportFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.autocomplete_fragment_container, autocompleteFragment)
            .commit()

        autocompleteFragment.setPlaceFields(
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng
                if (latLng != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                    map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(place.name)
                    )
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(requireContext(), "Error: $status", Toast.LENGTH_SHORT).show()
            }
        })

        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        return rootView
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableMyLocation()

        clusterManager = ClusterManager(requireContext(), map)
        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)

        map.setOnCameraIdleListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastQueryTime > QUERY_DELAY) {
                val bounds = map.projection.visibleRegion.latLngBounds

                dashboardViewModel.updateBounds(
                    bounds.southwest.latitude,
                    bounds.northeast.latitude,
                    bounds.southwest.longitude,
                    bounds.northeast.longitude
                )

                fetchAndClusterMarkers(
                    bounds.southwest.latitude,
                    bounds.northeast.latitude,
                    bounds.southwest.longitude,
                    bounds.northeast.longitude
                )

                lastQueryTime = currentTime
            }
        }
    }
    private fun fetchAndClusterMarkers(minLat: Double, maxLat: Double, minLong: Double, maxLong: Double) {

        val formattedMinLat = convertToDDDMM(minLat)
        val formattedMaxLat = convertToDDDMM(maxLat)
        val formattedMinLong = convertToDDDMM(minLong)
        val formattedMaxLong = convertToDDDMM(maxLong)

        val fullUrl = "http://umay.develop-er.org/get-records?minLat=$formattedMinLat&maxLat=$formattedMaxLat&minLong=$formattedMinLong&maxLong=$formattedMaxLong"
        Log.d("API_URL", "Requesting URL: $fullUrl")

        api.getDebrisSites(formattedMinLat, formattedMaxLat, formattedMinLong, formattedMaxLong).enqueue(object : Callback<List<DebrisSite>> {
            override fun onResponse(call: Call<List<DebrisSite>>, response: Response<List<DebrisSite>>) {
                if (response.isSuccessful) {
                    val debrisSites = response.body() ?: emptyList()
                    Log.d("API Response", "Number of debris sites: ${debrisSites.size}")


                    clusterManager.clearItems()

                    if (debrisSites.isNotEmpty()) {
                        val firstSite = LatLng(
                            convertToDecimalDegrees(debrisSites[0].latitude),
                            convertToDecimalDegrees(debrisSites[0].longitude)
                        )
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(firstSite, 12f))

                        debrisSites.forEach { site ->
                            val convertedLatitude = convertToDecimalDegrees(site.latitude)
                            val convertedLongitude = convertToDecimalDegrees(site.longitude)
                            val position = LatLng(convertedLatitude, convertedLongitude)

                            Log.d("DebrisMarker", "Position: $convertedLatitude, $convertedLongitude")

                            val icon = getResizedBitmapIcon(R.drawable.ic_debris_marker, 100, 100)
                            if (icon != null) {
                                val clusterItem = DebrisClusterItem(
                                    id = site._id,
                                    latLng = position,
                                    clusterTitle = "Debris Site: ${site.audioFileName}",
                                    clusterSnippet = "Timestamp: ${site.timestamp}"
                                )
                                clusterManager.addItem(clusterItem)
                            } else {
                                Log.e("DebrisMarker", "Failed to load marker icon")
                            }
                        }

                        clusterManager.cluster()
                    } else {
                        Log.w("FetchDebrisSites", "No debris sites found")
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load debris sites", Toast.LENGTH_SHORT).show()
                    Log.e("FetchDebrisSites", "Response failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<DebrisSite>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("FetchDebrisSites", "Error fetching debris sites", t)
            }
        })
    }

    private fun convertToDDDMM(coordinate: Double): Int {
        val degrees = coordinate.toInt()
        val minutes = ((coordinate - degrees) * 60).toInt()
        return degrees * 100 + minutes
    }


    private fun convertToDecimalDegrees(coordinate: Double): Double {
        val degrees = (coordinate / 100).toInt()
        val minutes = coordinate % 100
        val decimalDegrees = degrees + (minutes / 60.0)


        return BigDecimal(decimalDegrees).setScale(8, RoundingMode.HALF_UP).toDouble()
    }


    private fun getResizedBitmapIcon(resourceId: Int, width: Int, height: Int): BitmapDescriptor? {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
