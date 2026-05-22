package com.example.crewportal.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class WeatherRepository {
    private val api: WeatherApi = Retrofit.Builder()
        .baseUrl("https://aviationweather.gov/")
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)

    suspend fun getReport(icao: String): Pair<String, String> {
        val clean = icao.trim().uppercase()
        val metar = api.getMetar(clean).trim().ifBlank { "No METAR found for $clean" }
        val taf = api.getTaf(clean).trim().ifBlank { "No TAF found for $clean" }
        return metar to taf
    }
}
