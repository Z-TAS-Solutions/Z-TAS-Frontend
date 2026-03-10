package package com.example.z_tas

enum class NotificationType {
    SECURITY,
    INFO,
    SUCCESS,
    ERROR,
    WARNING
}

data class Notification(
    val id: Int,
    val title: String,
    val message: String,
    val time: String,
    val type: NotificationType,
    var isRead: Boolean = false
)