package com.example.z_tas.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // TODO: Replace with your actual backend URL
    private const val BASE_URL = "https://z-tas.com/api/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val webAuthnApi: WebAuthnApi = retrofit.create(WebAuthnApi::class.java)
    val sessionApi: SessionApi = retrofit.create(SessionApi::class.java)
    val notificationApi: NotificationApi = retrofit.create(NotificationApi::class.java)
}
