package com.example.rescueapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

class HomeViewModel : ViewModel() {
    private val _cameraPosition = MutableLiveData<CameraPosition>()
    val cameraPosition: LiveData<CameraPosition> get() = _cameraPosition

    private val _bounds = MutableLiveData<MapBounds>()
    val bounds: LiveData<MapBounds> get() = _bounds

    fun updateCameraPosition(position: CameraPosition) {
        _cameraPosition.value = position
    }

    fun updateBounds(minLat: Double, maxLat: Double, minLong: Double, maxLong: Double) {
        _bounds.value = MapBounds(minLat, maxLat, minLong, maxLong)
    }

    data class MapBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLong: Double,
        val maxLong: Double
    )
}