package com.ztas.app.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SessionApi {

    @GET("user/sessions")
    suspend fun getSessions(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<SessionsResponse>

    @POST("user/sessions/logout-others")
    suspend fun logoutOtherDevices(
        @Header("Authorization") token: String
    ): Response<LogoutOthersResponse>

    @POST("user/force-logout-devices")
    suspend fun forceLogoutAllDevices(
        @Header("Authorization") token: String
    ): Response<ForceLogoutResponse>
}
