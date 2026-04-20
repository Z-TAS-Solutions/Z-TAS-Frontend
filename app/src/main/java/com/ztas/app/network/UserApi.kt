package com.ztas.app.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HTTP
import retrofit2.http.POST

interface UserApi {

    @POST("admin/users/register/new")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("admin/users/register/verifyOTP")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("admin/users/register/resendOTP")
    suspend fun resendRegistrationOtp(@Body request: ResendOtpRequest): Response<ResponseBody>

    @GET("user/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @POST("user/account/delete/begin")
    suspend fun beginDeleteAccount(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @HTTP(method = "DELETE", path = "user/account", hasBody = true)
    suspend fun confirmDeleteAccount(
        @Header("Authorization") token: String,
        @Header("X-Session-Token") sessionToken: String,
        @Body request: FinishLoginRequest
    ): Response<DeleteAccountResponse>
}
