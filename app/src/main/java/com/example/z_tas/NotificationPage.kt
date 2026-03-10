package com.example.z_tas

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var emptyState: View
    private lateinit var tabAll: TextView
    private lateinit var tabUnread: TextView

    private var allNotifications = mutableListOf<Notification>()
    private var currentFilter = FilterType.ALL

    enum class FilterType {
        ALL, UNREAD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_page)

        // Initialize views
        recyclerView = findViewById(R.id.notificationsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        tabAll = findViewById(R.id.tabAll)
        tabUnread = findViewById(R.id.tabUnread)
        val btnMarkAllRead = findViewById<TextView>(R.id.btnMarkAllRead)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(allNotifications) { notification ->
            // Handle notification click
            markAsRead(notification)
        }
        recyclerView.adapter = adapter

        // Load sample notifications
        loadNotifications()

        // Tab click listeners
        tabAll.setOnClickListener {
            selectTab(FilterType.ALL)
        }

        tabUnread.setOnClickListener {
            selectTab(FilterType.UNREAD)
        }

        // Mark all as read
        btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }

        // Initial state
        selectTab(FilterType.ALL)
    }

    private fun loadNotifications() {
        allNotifications.clear()
        allNotifications.addAll(
            listOf(
                Notification(
                    id = 1,
                    title = "MFA REQUIRED",
                    message = "Please complete MFA to proceed with payment.",
                    time = "5 mins ago",
                    type = NotificationType.SECURITY,
                    isRead = false
                ),
                Notification(
                    id = 2,
                    title = "MFA CODE SENT",
                    message = "A verification code was sent to your number ending 765.",
                    time = "5 mins ago",
                    type = NotificationType.INFO,
                    isRead = false
                ),
                Notification(
                    id = 3,
                    title = "IDENTITY VERIFIED",
                    message = "Your identity was verified and approved.",
                    time = "4 hrs ago",
                    type = NotificationType.SUCCESS,
                    isRead = true
                ),
                Notification(
                    id = 4,
                    title = "MFA FAILED",
                    message = "Incorrect code entered. Access blocked.",
                    time = "21 hrs ago",
                    type = NotificationType.ERROR,
                    isRead = true
                ),
                Notification(
                    id = 5,
                    title = "ACCESS DENIED",
                    message = "Your authentication did not meet security requirements.",
                    time = "1 day ago",
                    type = NotificationType.WARNING,
                    isRead = true
                )
            )
        )
    }

    private fun selectTab(filter: FilterType) {
        currentFilter = filter

        when (filter) {
            FilterType.ALL -> {
                tabAll.setBackgroundResource(R.drawable.tab_selected)
                tabAll.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                tabUnread.setBackgroundResource(R.drawable.tab_unselected)
                tabUnread.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

                adapter.updateNotifications(allNotifications)
            }
            FilterType.UNREAD -> {
                tabUnread.setBackgroundResource(R.drawable.tab_selected)
                tabUnread.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                tabAll.setBackgroundResource(R.drawable.tab_unselected)
                tabAll.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

                val unreadNotifications = allNotifications.filter { !it.isRead }
                adapter.updateNotifications(unreadNotifications)
            }
        }

        updateEmptyState()
    }

    private fun markAsRead(notification: Notification) {
        val index = allNotifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            allNotifications[index].isRead = true
            adapter.notifyItemChanged(index)
            updateUnreadCount()
        }
    }

    private fun markAllAsRead() {
        allNotifications.forEach { it.isRead = true }
        adapter.notifyDataSetChanged()
        updateUnreadCount()

        if (currentFilter == FilterType.UNREAD) {
            selectTab(FilterType.UNREAD)
        }
    }

    private fun updateUnreadCount() {
        val unreadCount = allNotifications.count { !it.isRead }
        val notifCount = findViewById<TextView>(R.id.notifCount)
        notifCount.text = "$unreadCount unread"
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}