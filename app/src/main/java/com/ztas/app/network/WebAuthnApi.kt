package com.ztas.app.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface WebAuthnApi {

    // ── Login ────────────────────────────────────────────────
    @POST("webauthn/login/begin")
    suspend fun beginLogin(@Body request: BeginLoginRequest): Response<ResponseBody>

    @POST("webauthn/login/finish")
    suspend fun finishLogin(
        @Header("X-Session-Token") sessionToken: String,
        @Body request: FinishLoginRequest
    ): Response<ResponseBody>

    // ── Register ─────────────────────────────────────────────
    @POST("webauthn/register/begin")
    suspend fun beginRegister(@Body request: BeginRegisterRequest): Response<BeginRegisterResponse>

    @POST("webauthn/register/finish")
    suspend fun finishRegister(
        @Header("X-Session-Token") sessionToken: String,
        @Body request: FinishRegisterRequest
    ): Response<FinishRegisterResponse>
}
