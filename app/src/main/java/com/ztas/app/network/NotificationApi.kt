package com.ztas.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {

    @GET("user/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("unread_only") unreadOnly: Boolean? = null,
        @Query("sort_order") sortOrder: String? = null
    ): Response<NotificationsResponse>

    @PATCH("user/notifications/{notificationId}/status")
    suspend fun updateNotificationStatus(
        @Header("Authorization") token: String,
        @Path("notificationId") notificationId: String,
        @Body request: NotificationStatusRequest
    ): Response<NotificationStatusResponse>

    @PATCH("user/notifications/read-all")
    suspend fun markAllAsRead(
        @Header("Authorization") token: String
    ): Response<MarkAllReadResponse>
}
