package com.example.z_tas.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface WebAuthnApi {

    // ── Login ────────────────────────────────────────────────
    @POST("webauthn/login/begin")
    suspend fun beginLogin(@Body request: BeginLoginRequest): Response<BeginLoginResponse>

    @POST("webauthn/login/finish")
    suspend fun finishLogin(@Body request: FinishLoginRequest): Response<FinishLoginResponse>

    // ── Register ─────────────────────────────────────────────
    @POST("webauthn/register/begin")
    suspend fun beginRegister(@Body request: BeginRegisterRequest): Response<BeginRegisterResponse>

    @POST("webauthn/register/finish")
    suspend fun finishRegister(
        @Header("X-Session-Token") sessionToken: String,
        @Body request: FinishRegisterRequest
    ): Response<FinishRegisterResponse>
}
