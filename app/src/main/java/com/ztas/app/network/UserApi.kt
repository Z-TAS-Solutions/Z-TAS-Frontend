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

    // Some backends implement account deletion as DELETE-with-body, others as POST.
    // Keep both and let the caller fallback based on HTTP status.
    @HTTP(method = "DELETE", path = "user/account/delete", hasBody = true)
    suspend fun deleteAccountDelete(
        @Header("Authorization") token: String,
        @Body request: DeleteAccountRequest
    ): Response<DeleteAccountResponse>

    @POST("user/account/delete")
    suspend fun deleteAccountPost(
        @Header("Authorization") token: String,
        @Body request: DeleteAccountRequest
    ): Response<DeleteAccountResponse>
}
