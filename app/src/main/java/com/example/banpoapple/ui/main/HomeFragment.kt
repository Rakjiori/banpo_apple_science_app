package com.example.banpoapple.ui.main

import android.os.Bundle
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.core.view.isVisible
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.banpoapple.di.AppContainer
import com.example.banpoapple.R
import com.example.banpoapple.network.GroupResponse
import com.example.banpoapple.network.NoticeResponse
import com.example.banpoapple.network.ReviewResponse
import com.example.banpoapple.network.ScheduleSlotResponse
import com.example.banpoapple.network.NotificationResponse
import com.example.banpoapple.network.GroupNoticeResponse
import com.example.banpoapple.network.GroupTaskResponse
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val scheduleList = view.findViewById<LinearLayout>(R.id.scheduleList)
        val announcementList = view.findViewById<LinearLayout>(R.id.announcementList)
        val reviewList = view.findViewById<LinearLayout>(R.id.reviewList)
        val groupList = view.findViewById<LinearLayout>(R.id.groupList)
        val headerBell = view.findViewById<View>(R.id.headerBell)
        val headerGroup = view.findViewById<View>(R.id.headerGroup)
        headerBell?.setOnClickListener { showNotificationDropdown() }
        headerGroup?.setOnClickListener { navigateToGroups() }

        fetchAndRenderData(
            scheduleList,
            announcementList,
            reviewList,
            groupList
        )
    }

    private fun loadNotifications(
        progress: ProgressBar,
        status: TextView,
        list: LinearLayout,
        inflater: LayoutInflater
    ) {
        progress.isVisible = true
        status.text = getString(R.string.loading_notifications)
        list.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = AppContainer.authRepository.fetchNotifications()
            progress.isVisible = false

            result.fold(
                onSuccess = { items -> renderNotifications(items, status, list, inflater) },
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
        list: LinearLayout,
        inflater: LayoutInflater
    ) {
        list.removeAllViews()
        if (items.isEmpty()) {
            status.isVisible = true
            status.text = getString(R.string.notification_empty)
            return
        }

        status.isVisible = false
        items.forEach { notification ->
            val textView = inflater.inflate(
                R.layout.item_notification,
                list,
                false
            ) as TextView

            val body = buildString {
                append(notification.title)
                if (!notification.body.isNullOrBlank()) {
                    append("\n")
                    append(notification.body.trim())
                }
                if (!notification.url.isNullOrBlank()) {
                    append("\n")
                    append(notification.url)
                }
            }
            textView.text = body
            list.addView(textView)
        }
    }

    private fun fetchAndRenderData(
        scheduleContainer: LinearLayout,
        announcementContainer: LinearLayout,
        reviewContainer: LinearLayout,
        groupContainer: LinearLayout
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            AppContainer.contentRepository.fetchHomeFeed().fold(
                onSuccess = { feed ->
                    renderSchedules(scheduleContainer, feed.schedules)
                    renderAnnouncements(announcementContainer, feed.notices)
                    renderReviews(reviewContainer, feed.reviews)
                    renderGroups(groupContainer, feed.groups)
                },
                onFailure = {
                    // graceful fallback to individual calls
                    loadLegacyData(
                        scheduleContainer,
                        announcementContainer,
                        reviewContainer,
                        groupContainer
                    )
                }
            )
        }
    }

    private suspend fun loadLegacyData(
        scheduleContainer: LinearLayout,
        announcementContainer: LinearLayout,
        reviewContainer: LinearLayout,
        groupContainer: LinearLayout
    ) {
        // Schedules
        AppContainer.contentRepository.fetchSchedules().fold(
            onSuccess = { renderSchedules(scheduleContainer, it) },
            onFailure = { renderSchedules(scheduleContainer, emptyList()) }
        )

        // Announcements
        AppContainer.contentRepository.fetchNotices().fold(
            onSuccess = {
                renderAnnouncements(announcementContainer, it)
            },
            onFailure = {
                renderAnnouncements(announcementContainer, emptyList())
            }
        )

        // Reviews
        AppContainer.contentRepository.fetchReviews().fold(
            onSuccess = { renderReviews(reviewContainer, it) },
            onFailure = { renderReviews(reviewContainer, emptyList()) }
        )

        // Groups
        val groups = AppContainer.contentRepository.fetchGroups().getOrNull().orEmpty()
        renderGroups(groupContainer, groups)
    }

    private fun renderSchedules(container: LinearLayout, schedules: List<ScheduleSlotResponse>) {
        container.removeAllViews()
        val ctx = requireContext()
        val items = if (schedules.isNotEmpty()) schedules else emptyList()
        if (items.isEmpty()) {
            addSimpleText(container, getString(R.string.schedule_empty))
            return
        }
        items.forEach { schedule ->
            val tv = TextView(ctx).apply {
                val day = schedule.dayOfWeek ?: "요일 미정"
                val title = schedule.subject ?: "수업"
                val time = listOfNotNull(schedule.startTime, schedule.endTime).joinToString(" - ")
                text = "$day · $title ${time}".trim() +
                        (schedule.note?.let { "\n$it" } ?: "")
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 14f
            }
            container.addView(tv)
        }
    }

    private fun renderAnnouncements(container: LinearLayout, notices: List<NoticeResponse>) {
        container.removeAllViews()
        if (notices.isEmpty()) {
            addSimpleText(container, getString(R.string.announcement_empty))
            return
        }
        notices.forEach { announcement ->
            val created = announcement.createdAt ?: ""
            addCard(
                container,
                announcement.title ?: "",
                "${announcement.content ?: ""}\n$created"
            ) {
                showDialog("공지사항", "${announcement.title ?: ""}\n\n${announcement.content ?: ""}")
            }
        }
    }

    private fun renderReviews(container: LinearLayout, reviews: List<ReviewResponse>) {
        container.removeAllViews()
        val ctx = requireContext()
        val items = if (reviews.isNotEmpty()) reviews else emptyList()
        if (items.isEmpty()) {
            addSimpleText(container, getString(R.string.review_empty))
            return
        }
        items.forEach { review ->
            val rating = review.rating?.let { "⭐ $it" } ?: "⭐"
            val author = review.author ?: ""
            addCard(container, "$rating · $author", review.content ?: "") {
                showDialog("수강 후기", "${review.content ?: ""}\n\n- $author")
            }
        }
    }

    private fun renderGroups(container: LinearLayout, groups: List<GroupResponse>) {
        container.removeAllViews()
        val items = if (groups.isNotEmpty()) groups else emptyList()
        if (items.isEmpty()) {
            addSimpleText(container, getString(R.string.group_empty))
            return
        }
        items.forEach { group ->
            addCard(
                container,
                group.name ?: "그룹",
                "멤버 ${group.memberCount ?: 0}명"
            ) { showGroupDetail(group) }
        }
    }

    private fun addSimpleText(container: LinearLayout, message: String) {
        val tv = TextView(requireContext()).apply {
            text = message
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 14f
        }
        container.addView(tv)
    }

    private fun addCard(container: LinearLayout, title: String, body: String, onClick: (() -> Unit)? = null) {
        val ctx = requireContext()
        val card = com.google.android.material.card.MaterialCardView(ctx).apply {
            radius = 12f
            cardElevation = 6f
            useCompatPadding = true
            strokeWidth = 1
            strokeColor = resources.getColor(R.color.purple_200, null)
            setCardBackgroundColor(resources.getColor(R.color.card_tint, null))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            layoutParams = lp

            val inner = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }
            val titleView = TextView(ctx).apply {
                text = title
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val bodyView = TextView(ctx).apply {
                text = body
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 14f
                setPadding(0, 6, 0, 0)
            }
            inner.addView(titleView)
            inner.addView(bodyView)
            addView(inner)
            setOnClickListener { onClick?.invoke() }
        }
        container.addView(card)
    }

    private fun showGroupDetail(group: GroupResponse) {
        viewLifecycleOwner.lifecycleScope.launch {
            val groupId = group.id ?: return@launch
            val tasks = AppContainer.contentRepository.fetchGroupTasks(groupId).getOrNull().orEmpty()
            val notices = AppContainer.contentRepository.fetchGroupNotices(groupId).getOrNull().orEmpty()
            val message = buildString {
                append("시간표: 준비 중\n\n")
                append("과제\n")
                if (tasks.isEmpty()) append("- 과제가 없습니다.\n") else {
                    tasks.forEach { append("- ${it.title ?: ""} ${it.dueDate?.let { d -> "($d)" } ?: ""}\n") }
                }
                append("\n공지\n")
                if (notices.isEmpty()) append("- 공지가 없습니다.\n") else {
                    notices.forEach { append("- ${it.title ?: ""} ${it.createdAt?.let { d -> "($d)" } ?: ""}\n") }
                }
            }
            AlertDialog.Builder(requireContext())
                .setTitle(group.name ?: "그룹 상세")
                .setMessage(message)
                .setPositiveButton("닫기", null)
                .show()
        }
    }

    private fun showDialog(title: String, body: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton("확인", null)
            .show()
    }

    private fun showNotificationDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            val notifications = AppContainer.authRepository.fetchNotifications().getOrElse { emptyList() }
            val groups = AppContainer.contentRepository.fetchGroups().getOrNull().orEmpty()
            if (notifications.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("알림")
                    .setMessage("새 알림이 없습니다.")
                    .setPositiveButton("닫기", null)
                    .show()
                return@launch
            }
            fun resolveGroupName(notification: NotificationResponse): String? {
                val url = notification.url ?: return null
                val id = url.substringAfterLast("/groups/", missingDelimiterValue = "").takeWhile { it.isDigit() }
                val groupId = id.toLongOrNull() ?: return null
                return groups.firstOrNull { it.id == groupId }?.name
            }
            val titles = notifications.map {
                resolveGroupName(it)?.let { name -> "${it.title} · $name" } ?: it.title
            }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("알림")
                .setItems(titles) { _, index ->
                    val item = notifications[index]
                    val groupName = resolveGroupName(item)?.let { "\n\n그룹: $it" } ?: ""
                    showDialog(item.title, item.body + groupName)
                }
                .setNegativeButton("닫기", null)
                .show()
        }
    }

    private fun navigateToGroups() {
        (activity?.findViewById<BottomNavigationView>(R.id.bottomNav))?.selectedItemId = R.id.menu_groups
    }
}
