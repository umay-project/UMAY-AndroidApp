package com.example.rescueapp.ui.home

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescueapp.R
import com.example.rescueapp.ui.controller.ClusterDetailsAdapter
import com.example.rescueapp.ui.controller.CustomClusterRenderer
import com.example.rescueapp.ui.controller.api
import com.example.rescueapp.ui.dashboard.DashboardViewModel
import com.example.rescueapp.ui.models.DebrisClusterItem
import com.example.rescueapp.ui.models.DebrisSite
import com.example.rescueapp.ui.models.FakeDebrisSite
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var clusterManager: ClusterManager<DebrisClusterItem>
    private lateinit var dashboardViewModel: DashboardViewModel
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var lastBounds: LatLngBounds? = null
    private var API_KEY = "AIzaSyBwwAKgOZxVsEHn6fMVAbkObDpopTdxWXY"
    private var lastQueryTime: Long = 0
    private val QUERY_DELAY = 1000L
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var startTimestamp: Long? = null
    private var endTimestamp: Long? = null
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var btnClearTime: ImageButton


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        btnStartTime = rootView.findViewById(R.id.btnStartTime)
        btnEndTime = rootView.findViewById(R.id.btnEndTime)
        btnClearTime = rootView.findViewById(R.id.btnClearTime)

        btnStartTime.setOnClickListener {
            showTimePickerDialog(true)
        }

        btnEndTime.setOnClickListener {
            showTimePickerDialog(false)
        }

        btnClearTime.setOnClickListener {
            clearTimeFilters()
        }

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

    private fun clearTimeFilters() {
        startTimestamp = null
        endTimestamp = null
        btnStartTime.text = "Start Time"
        btnEndTime.text = "End Time"

        val bounds = map.projection.visibleRegion.latLngBounds
        fetchAndClusterMarkers(
            bounds.southwest.latitude,
            bounds.northeast.latitude,
            bounds.southwest.longitude,
            bounds.northeast.longitude
        )
    }

    private fun showTimePickerDialog(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        calendar.set(year, month, day, hour, minute)
                        val timestamp = calendar.timeInMillis

                        if (isStartTime) {
                            startTimestamp = timestamp
                            btnStartTime.text = formatDateTime(timestamp)
                        } else {
                            endTimestamp = timestamp
                            btnEndTime.text = formatDateTime(timestamp)
                        }

                        val bounds = map.projection.visibleRegion.latLngBounds
                        fetchAndClusterMarkers(
                            bounds.southwest.latitude,
                            bounds.northeast.latitude,
                            bounds.southwest.longitude,
                            bounds.northeast.longitude
                        )
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
    private var currentZoomLevel: Float = 0f

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableMyLocation()

        clusterManager = ClusterManager(requireContext(), map)

        val customRenderer = CustomClusterRenderer(requireContext(), map, clusterManager)
        clusterManager.renderer = customRenderer

        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)

        setupClusterClickListener()


        map.setOnCameraIdleListener {

            currentZoomLevel = map.cameraPosition.zoom
            clusterManager.onCameraIdle()

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastQueryTime > QUERY_DELAY) {
                val bounds = map.projection.visibleRegion.latLngBounds

                if (lastBounds == null || !lastBounds!!.contains(bounds.northeast) || !lastBounds!!.contains(bounds.southwest)) {
                    lastBounds = bounds
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
                }
                lastQueryTime = currentTime
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment_container) as? AutocompleteSupportFragment
        if (autocompleteFragment != null) {
            childFragmentManager.beginTransaction().remove(autocompleteFragment).commitAllowingStateLoss()
            Log.d("PlacesAPI", "AutocompleteSupportFragment removed.")
        }
        Places.deinitialize()

        mediaPlayer?.release()
        mediaPlayer = null
    }

     fun onResponse(call: Call<List<DebrisSite>>, response: Response<List<DebrisSite>>) {
        if (response.isSuccessful) {
            val debrisSites = response.body() ?: emptyList()
            Log.d("API Response", "Debris Sites: $debrisSites")

            debrisSites.forEach { site ->
                Log.d("DebrisSite", "ID: ${site._id}, FileName: ${site.audioFileName}, Latitude: ${site.latitude}, Longitude: ${site.longitude}")
            }
        } else {
            Log.e("API Error", "Error: ${response.errorBody()?.string()}")
        }
    }


    private fun fetchAndClusterMarkers(minLat: Double, maxLat: Double, minLong: Double, maxLong: Double) {
        val formattedMinLat = convertToDDDMM(minLat)
        val formattedMaxLat = convertToDDDMM(maxLat)
        val formattedMinLong = convertToDDDMM(minLong)
        val formattedMaxLong = convertToDDDMM(maxLong)

        val formattedStartTime = startTimestamp
        val formattedEndTime = endTimestamp

        var fullUrl = "http://umay.develop-er.org/get-records?minLat=$formattedMinLat&maxLat=$formattedMaxLat&minLong=$formattedMinLong&maxLong=$formattedMaxLong"
        if (formattedStartTime != null) {
            fullUrl += "&minTime:$formattedStartTime"
        }
        if (formattedEndTime != null) {
            fullUrl += "&maxTime:$formattedEndTime"
        }
        Log.d("API_URL", "Requesting URL: $fullUrl")


        val apiCall = if (startTimestamp != null || endTimestamp != null) {
            api.getDebrisSitesWithTime(
                formattedMinLat,
                formattedMaxLat,
                formattedMinLong,
                formattedMaxLong,
                formattedStartTime,
                formattedEndTime
            )
        } else {
            api.getDebrisSites(
                formattedMinLat,
                formattedMaxLat,
                formattedMinLong,
                formattedMaxLong
            )
        }

        apiCall.enqueue(object : Callback<List<DebrisSite>> {
            override fun onResponse(call: Call<List<DebrisSite>>, response: Response<List<DebrisSite>>) {
                if (response.isSuccessful) {
                    val debrisSites = response.body() ?: emptyList()
                    Log.d("API Response", "Number of debris sites: ${debrisSites.size}")

                    clusterManager.clearItems()

                    if (debrisSites.isNotEmpty()) {
                        debrisSites.forEach { site ->
                            val convertedLatitude = convertToDecimalDegrees(site.latitude)
                            val convertedLongitude = convertToDecimalDegrees(site.longitude)
                            val position = LatLng(convertedLatitude, convertedLongitude)

                            Log.d("DebrisMarker", "Position: $convertedLatitude, $convertedLongitude")

                            val readableTimestamp = convertTimestampToDateTime(site.timestamp)

                            val icon = getResizedBitmapIcon(R.drawable.ic_debris_marker, 100, 100)
                            if (icon != null) {
                                val clusterItem = DebrisClusterItem(
                                    id = site._id,
                                    latLng = position,
                                    clusterTitle = "Debris Site",
                                    clusterSnippet = readableTimestamp,
                                    audioFileName = site.audioFileName
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

    private fun setupClusterClickListener() {
        clusterManager.setOnClusterClickListener { cluster ->
            if (map.cameraPosition.zoom >= 18) {
                showClusterDetails(cluster)
            } else {
                val builder = LatLngBounds.Builder()
                for (item in cluster.items) {
                    builder.include(item.position)
                }
                val bounds = builder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
            }
            true
        }

        clusterManager.setOnClusterItemClickListener { clusterItem ->
            showMarkerDetails(clusterItem)
            true
        }
    }

    private fun showClusterDetails(cluster: Cluster<DebrisClusterItem>) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_cluster_details, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ClusterDetailsAdapter(cluster.items.toList()) { audioSnippet ->
            playAudioFromURL(audioSnippet)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Cluster Details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()
            .show()
    }




    private fun showMarkerDetails(item: DebrisClusterItem) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(item.clusterTitle)
            .setMessage("Date: ${item.clusterSnippet}")
            .setPositiveButton("Play Audio") { _, _ ->
                playAudioFromURL(item.audioFileName)
            }
            .setNegativeButton("Close", null)
            .create()
        dialog.show()
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun playAudioFromURL(fileName: String) {
        val sanitizedFileName = if (fileName.endsWith(".wav")) {
            fileName.substringBeforeLast(".wav")
        } else {
            fileName
        }

        val audioUrl = "http://umay.develop-er.org/get-audio?fileName=$sanitizedFileName"
        Log.d("AudioURL", "Requesting audio from URL: $audioUrl")

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
                    Toast.makeText(requireContext(), "Audio playback completed.", Toast.LENGTH_SHORT).show()
                    release()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayer", "Error occurred: what=$what, extra=$extra")
                    Toast.makeText(requireContext(), "Error playing audio. What=$what, Extra=$extra", Toast.LENGTH_SHORT).show()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error initializing audio.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f))
                    map.addMarker(
                        MarkerOptions()
                            .position(userLocation)
                            .title("Mevcut Konum")
                    )
                } else {
                    showDefaultLocation()
                }
            }.addOnFailureListener {
                showDefaultLocation()
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showDefaultLocation() {
        val defaultLocation = LatLng(41.015137, 28.979530)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 16f))
        map.addMarker(
            MarkerOptions()
                .position(defaultLocation)
                .title("Varsayılan Konum")
        )
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

    fun convertTimestampToDateTime(timestamp: String): String {
        return try {
            Log.d("TimestampDebug", "Converting timestamp: $timestamp")
            val milliseconds = timestamp.toLong()
            val date = Date(milliseconds)
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC") // Ensure the time zone is UTC
            format.format(date)
        } catch (e: Exception) {
            "Invalid Timestamp"
        }
    }



}
