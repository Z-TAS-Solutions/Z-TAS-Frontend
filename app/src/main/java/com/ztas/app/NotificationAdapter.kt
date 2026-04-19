package com.ztas.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private var notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.notificationIcon)
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val message: TextView = view.findViewById(R.id.notificationMessage)
        val time: TextView = view.findViewById(R.id.notificationTime)
        val unreadIndicator: View = view.findViewById(R.id.unreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.title.text = notification.title
        holder.message.text = notification.message
        holder.time.text = notification.time

        // Set icon based on notification type
        val iconRes = when (notification.type) {
            NotificationType.SECURITY -> R.drawable.ic_security
            NotificationType.INFO -> R.drawable.ic_info
            NotificationType.SUCCESS -> R.drawable.ic_check
            NotificationType.ERROR -> R.drawable.ic_error
            NotificationType.WARNING -> R.drawable.ic_warning
        }
        holder.icon.setImageResource(iconRes)

        // Show/hide unread indicator
        holder.unreadIndicator.visibility = if (notification.isRead) View.INVISIBLE else View.VISIBLE

        // Click listener
        holder.itemView.setOnClickListener {
            onItemClick(notification)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateNotifications(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}