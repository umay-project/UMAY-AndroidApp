package com.example.rescueapp.ui.controller

import com.example.rescueapp.ui.models.DebrisSite
import retrofit2.Call
import retrofit2.http.GET

interface DebrisSiteApi {
    @GET("get-data")
    fun getDebrisSites(): Call<List<DebrisSite>>
}
