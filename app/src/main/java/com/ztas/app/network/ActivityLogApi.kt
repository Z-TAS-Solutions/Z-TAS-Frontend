package com.ztas.app.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ActivityLogApi {

    @GET("user/activity-logs")
    suspend fun getUserActivityLogs(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ActivityLogsApiEnvelope>
}
