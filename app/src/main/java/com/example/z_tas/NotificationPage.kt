package com.example.z_tas

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.z_tas.network.NotificationData
import com.example.z_tas.network.NotificationStatusRequest
import com.example.z_tas.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var emptyState: View
    private lateinit var tabAll: TextView
    private lateinit var tabUnread: TextView

    private val notificationApi = RetrofitClient.notificationApi
    private var allNotifications = mutableListOf<Notification>()
    private var currentFilter = FilterType.ALL

    companion object {
        private const val TAG = "NotificationPage"
    }

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
        val backArrow = findViewById<ImageView>(android.widget.ImageView::class.java.cast(findViewById(R.id.backArrow))?.id ?: R.id.backArrow)
        // Wait, I can just do:
        findViewById<android.widget.ImageView>(R.id.backArrow)?.setOnClickListener {
            finish()
        }

        // Setup Bottom Nav Bar
        val composeView = findViewById<ComposeView>(R.id.compose_bottom_nav)
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ZtasNavBar(selectedIndex = 2)
            }
        }

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(allNotifications) { notification ->
            // Handle notification click — toggle read/unread via API
            toggleNotificationStatus(notification)
        }
        recyclerView.adapter = adapter

        // Load notifications from API
        loadNotifications()

        // Tab click listeners
        tabAll.setOnClickListener {
            selectTab(FilterType.ALL)
        }

        tabUnread.setOnClickListener {
            selectTab(FilterType.UNREAD)
        }

        // Mark all as read via API
        btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }

        // Initial state
        selectTab(FilterType.ALL)
    }

    /**
     * Fetches notifications from GET /user/notifications
     */
    private fun loadNotifications() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // TODO: Replace placeholder token with real token
                    notificationApi.getNotifications(
                        token = "Bearer PLACEHOLDER_TOKEN",
                        limit = 20
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val apiNotifications = response.body()!!.notifications
                    allNotifications.clear()
                    allNotifications.addAll(apiNotifications.map { it.toLocalNotification() })
                    selectTab(currentFilter)
                    updateUnreadCount()
                } else {
                    Log.e(TAG, "Load notifications failed: ${response.code()}")
                    Toast.makeText(this@NotificationPage, "Failed to load notifications", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Load notifications error", e)
                Toast.makeText(this@NotificationPage, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

    /**
     * Toggles a notification's status via PATCH /user/notifications/{id}/status.
     * If unread → marks as read. If read → marks as unread.
     */
    private fun toggleNotificationStatus(notification: Notification) {
        val newStatus = if (notification.isRead) "unread" else "read"

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    notificationApi.updateNotificationStatus(
                        token = "Bearer PLACEHOLDER_TOKEN",
                        notificationId = notification.id,
                        request = NotificationStatusRequest(newStatus)
                    )
                }

                if (response.isSuccessful) {
                    val index = allNotifications.indexOfFirst { it.id == notification.id }
                    if (index != -1) {
                        allNotifications[index].isRead = newStatus == "read"
                        selectTab(currentFilter)
                        updateUnreadCount()
                    }
                    Log.d(TAG, "Notification ${notification.id} marked as $newStatus")
                } else {
                    Log.e(TAG, "Mark notification failed: ${response.code()}")
                    Toast.makeText(this@NotificationPage, "Failed to update", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Mark notification error", e)
                Toast.makeText(this@NotificationPage, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Marks all notifications as read via PATCH /user/notifications/read-all.
     */
    private fun markAllAsRead() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    notificationApi.markAllAsRead("Bearer PLACEHOLDER_TOKEN")
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d(TAG, "Marked all as read: ${body?.data?.updatedCount} updated")

                    allNotifications.forEach { it.isRead = true }
                    selectTab(currentFilter)
                    updateUnreadCount()

                    Toast.makeText(this@NotificationPage, "All marked as read", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Mark all read failed: ${response.code()}")
                    Toast.makeText(this@NotificationPage, "Failed to mark all as read", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Mark all read error", e)
                Toast.makeText(this@NotificationPage, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    /**
     * Maps API [NotificationData] → local [Notification] model.
     */
    private fun NotificationData.toLocalNotification(): Notification {
        val titleUpper = title.uppercase()
        val type = when {
            "FAIL" in titleUpper || "ERROR" in titleUpper -> NotificationType.ERROR
            "BLOCK" in titleUpper || "DENIED" in titleUpper -> NotificationType.WARNING
            "SUCCESS" in titleUpper || "VERIFIED" in titleUpper -> NotificationType.SUCCESS
            "SECURITY" in titleUpper -> NotificationType.SECURITY
            else -> NotificationType.INFO
        }

        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        val diffMin = diffMs / (1000 * 60)
        val diffHour = diffMs / (1000 * 60 * 60)
        val diffDay = diffMs / (1000 * 60 * 60 * 24)
        val timeStr = when {
            diffMin < 1 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHour < 24 -> "${diffHour} hrs ago"
            else -> "${diffDay} day${if (diffDay > 1) "s" else ""} ago"
        }

        return Notification(
            id = notificationId ?: "",
            title = title.uppercase(),
            message = details,
            time = timeStr,
            type = type,
            isRead = status == "read"
        )
    }
}