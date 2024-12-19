package com.example.rescueapp.ui.controller

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("geocode/json")
    fun getBounds(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): Call<GeocodingResponse>
}

data class GeocodingResponse(
    val results: List<Result>
)

data class Result(
    val geometry: Geometry
)

data class Geometry(
    val bounds: Bounds?
)

data class Bounds(
    val northeast: Location,
    val southwest: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)
