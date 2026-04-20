package com.ztas.app.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TAG = "RetrofitClient"

    private const val BASE_URL = "http://172.188.97.210:8000/api/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // Fail faster on unreachable servers; keep read/write reasonable.
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .also { Log.i(TAG, "Using BASE_URL=$BASE_URL") }

    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val webAuthnApi: WebAuthnApi = retrofit.create(WebAuthnApi::class.java)
    val sessionApi: SessionApi = retrofit.create(SessionApi::class.java)
    val notificationApi: NotificationApi = retrofit.create(NotificationApi::class.java)
    val activityLogApi: ActivityLogApi = retrofit.create(ActivityLogApi::class.java)
}
