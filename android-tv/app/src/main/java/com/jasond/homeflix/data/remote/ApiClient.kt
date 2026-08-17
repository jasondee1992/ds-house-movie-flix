package com.jasond.homeflix.data.remote

import com.jasond.homeflix.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    @Volatile
    var baseUrl: String = BuildConfig.API_BASE_URL
        private set

    @Volatile
    private var configuredService: ApiService? = null

    val service: ApiService
        get() = configuredService ?: createService(baseUrl)

    fun createService(url: String): ApiService =
        Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

    @Synchronized
    fun configure(url: String) {
        baseUrl = url
        configuredService = createService(url)
    }
}
