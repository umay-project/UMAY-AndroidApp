package com.example.rescueapp.ui.controller

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val retrofit = Retrofit.Builder()
    .baseUrl("https://umay.develop-er.org")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(DebrisSiteApi::class.java)
