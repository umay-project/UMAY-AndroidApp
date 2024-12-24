package com.example.rescueapp.ui.controller

import com.example.rescueapp.ui.models.DebrisSite
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DebrisSiteApi {
    @GET("get-records")
    fun getDebrisSites(
        @Query("minLat") minLat: Int,
        @Query("maxLat") maxLat: Int,
        @Query("minLong") minLong: Int,
        @Query("maxLong") maxLong: Int
    ): Call<List<DebrisSite>>
}

