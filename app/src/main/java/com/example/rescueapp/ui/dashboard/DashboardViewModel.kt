package com.example.rescueapp.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {
    private val _bounds = MutableLiveData<Bounds>()
    val bounds: LiveData<Bounds> get() = _bounds

    fun updateBounds(minLat: Double, maxLat: Double, minLong: Double, maxLong: Double) {
        _bounds.value = Bounds(minLat, maxLat, minLong, maxLong)
    }

    data class Bounds(
        val minLat: Double,
        val maxLat: Double,
        val minLong: Double,
        val maxLong: Double
    )
}
