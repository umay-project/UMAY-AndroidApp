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

    @GET("get-records")
    fun getDebrisSitesWithTime(
        @Query("minLat") minLat: Int,
        @Query("maxLat") maxLat: Int,
        @Query("minLong") minLong: Int,
        @Query("maxLong") maxLong: Int,
        @Query("minTime") minTime: Long?,
        @Query("maxTime") maxTime: Long?
    ): Call<List<DebrisSite>>

    @GET("tag-entry")
    fun tagEntry(
        @Query("fileName") fileName: String,
        @Query("tag") tag: Boolean
    ): Call<Void>

    @GET("get-false-taggeds")
    fun getFalseTagged(): Call<List<DebrisSite>>

    @GET("delete-entry")
    fun deleteEntry(@Query("fileName") fileName: String): Call<Unit>
}

