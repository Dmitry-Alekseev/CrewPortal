package com.example.crewportal.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("api/data/metar")
    suspend fun getMetar(
        @Query("ids") ids: String,
        @Query("format") format: String = "raw"
    ): String

    @GET("api/data/taf")
    suspend fun getTaf(
        @Query("ids") ids: String,
        @Query("format") format: String = "raw"
    ): String
}
