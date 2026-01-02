package com.example.banpoapple.ui.main

import android.os.Bundle
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.banpoapple.R
import com.example.banpoapple.di.AppContainer
import com.example.banpoapple.network.GroupResponse
import com.example.banpoapple.network.NotificationResponse
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {

    private var currentNotifications: List<NotificationResponse> = emptyList()
    private var cachedGroups: List<GroupResponse> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.headerBell)?.setOnClickListener { openNotificationDropdown() }
        view.findViewById<View>(R.id.headerGroup)?.setOnClickListener { navigateToGroups() }
        val buttonRefresh = view.findViewById<View>(R.id.buttonRefreshNotifications)
        val progress = view.findViewById<ProgressBar>(R.id.notificationProgress)
        val status = view.findViewById<TextView>(R.id.notificationStatus)
        val unreadList = view.findViewById<LinearLayout>(R.id.notificationListUnread)
        val readList = view.findViewById<LinearLayout>(R.id.notificationListRead)
        buttonRefresh.setOnClickListener { loadNotifications(progress, status, unreadList, readList, layoutInflater) }
        loadNotifications(progress, status, unreadList, readList, layoutInflater)
    }

    private fun loadNotifications(
        progress: ProgressBar,
        status: TextView,
        unreadList: LinearLayout,
        readList: LinearLayout,
        inflater: LayoutInflater
    ) {
        progress.isVisible = true
        status.text = getString(R.string.loading_notifications)
        unreadList.removeAllViews()
        readList.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = AppContainer.authRepository.fetchNotifications()
            cachedGroups = AppContainer.contentRepository.fetchGroups().getOrNull().orEmpty()
            progress.isVisible = false

            result.fold(
                onSuccess = { items ->
                    currentNotifications = items
                    renderNotifications(items, status, unreadList, readList, inflater)
                },
                onFailure = { err ->
                    status.text = getString(
                        R.string.notification_error,
                        err.message ?: ""
                    )
                    status.isVisible = true
                }
            )
        }
    }

    private fun renderNotifications(
        items: List<NotificationResponse>,
        status: TextView,
        unreadList: LinearLayout,
        readList: LinearLayout,
        inflater: LayoutInflater
    ) {
        unreadList.removeAllViews()
        readList.removeAllViews()
        val unreadItems = items.filter { it.read != true }

        if (unreadItems.isEmpty()) {
            status.isVisible = true
            status.text = "모든 알림을 확인했어요."
        } else {
            status.isVisible = false
        }

        unreadItems.forEach { notification ->
            val card = MaterialCardView(requireContext()).apply {
                radius = 12f
                cardElevation = 4f
                setCardBackgroundColor(resources.getColor(R.color.card_tint, null))
                strokeWidth = 1
                strokeColor = resources.getColor(R.color.sky_stroke, null)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
                layoutParams = lp

                val bodyView = inflater.inflate(
                    R.layout.item_notification,
                    this,
                    false
                ) as TextView

                val body = buildString {
                    append(notification.title)
                    if (!notification.body.isNullOrBlank()) {
                        append("\n")
                        append(notification.body.trim())
                    }
                    resolveGroupName(notification)?.let {
                        append("\n")
                        append(it)
                    } ?: run {
                        if (!notification.url.isNullOrBlank()) {
                            append("\n")
                            append(notification.url)
                        }
                    }
                }
                bodyView.text = body
                addView(bodyView)
                setOnClickListener { showNotificationDetail(notification) }
            }
            unreadList.addView(card)
        }

        // 읽은 알림은 표시하지 않음
        readList.isVisible = false
        updateBadge(unreadItems.size)
    }

    private fun showNotificationDetail(notification: NotificationResponse) {
        val message = buildString {
            append(notification.body.ifBlank { notification.title })
            resolveGroupName(notification)?.let { append("\n\n그룹: $it") }
            notification.url?.let { append("\n\n링크: $it") }
        }
        AlertDialog.Builder(requireContext())
            .setTitle(notification.title)
            .setMessage(message)
            .setPositiveButton("닫기", null)
            .show()

        markAsRead(notification)
    }

    private fun markAsRead(notification: NotificationResponse) {
        if (notification.read == true) return
        val updated = currentNotifications.map {
            if (it.id == notification.id) it.copy(read = true) else it
        }
        currentNotifications = updated
        view?.let {
            val unreadList = it.findViewById<LinearLayout>(R.id.notificationListUnread)
            val readList = it.findViewById<LinearLayout>(R.id.notificationListRead)
            val status = it.findViewById<TextView>(R.id.notificationStatus)
            renderNotifications(updated, status, unreadList, readList, layoutInflater)
        }
    }

    private fun resolveGroupName(notification: NotificationResponse): String? {
        val url = notification.url ?: return null
        val id = url.substringAfterLast("/groups/", missingDelimiterValue = "").takeWhile { it.isDigit() }
        val groupId = id.toLongOrNull() ?: return null
        return cachedGroups.firstOrNull { it.id == groupId }?.name
    }

    private fun updateBadge(unreadCount: Int) {
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottomNav) ?: return
        val badge = bottomNav.getOrCreateBadge(R.id.menu_groups)
        if (unreadCount > 0) {
            badge.isVisible = true
            badge.number = unreadCount
            badge.badgeTextColor = resources.getColor(android.R.color.white, null)
            badge.backgroundColor = resources.getColor(android.R.color.holo_red_dark, null)
        } else {
            badge.isVisible = false
        }
    }

    private fun navigateToGroups() {
        (activity?.findViewById<BottomNavigationView>(R.id.bottomNav))?.selectedItemId = R.id.menu_groups
    }

    private fun openNotificationDropdown() {
        val unread = currentNotifications.filter { it.read != true }
        if (unread.isEmpty()) {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("알림")
                .setMessage("새 알림이 없습니다.")
                .setPositiveButton("닫기", null)
                .show()
            return
        }
        val labels = unread.map { notif ->
            resolveGroupName(notif)?.let { "${notif.title} · $it" } ?: notif.title
        }.toTypedArray()
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("알림")
            .setItems(labels) { _, index ->
                unread.getOrNull(index)?.let { showNotificationDetail(it) }
            }
            .setNegativeButton("닫기", null)
            .show()
    }
}
