package com.example.banpoapple.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.banpoapple.R
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.banpoapple.di.AppContainer
import com.example.banpoapple.network.GroupNoticeResponse
import com.example.banpoapple.network.GroupResponse
import com.example.banpoapple.network.GroupTaskResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class LearningFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_learning, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.headerBell)?.setOnClickListener { showNotificationsDropdown() }
        view.findViewById<View>(R.id.headerGroup)?.setOnClickListener { navigateToGroups() }
        val groupList = view.findViewById<LinearLayout>(R.id.learningGroupList)
        val taskList = view.findViewById<LinearLayout>(R.id.learningTaskList)
        val noticeList = view.findViewById<LinearLayout>(R.id.learningNoticeList)
        setPagination(view.findViewById(R.id.paginationLearningGroup))
        setPagination(view.findViewById(R.id.paginationLearningTask))
        setPagination(view.findViewById(R.id.paginationLearningNotice))

        viewLifecycleOwner.lifecycleScope.launch {
            val groups = AppContainer.contentRepository.fetchGroups().getOrNull().orEmpty()
            renderGroups(groupList, groups)

            val firstGroup = groups.firstOrNull()?.id
            if (firstGroup != null) {
                AppContainer.contentRepository.fetchGroupTasks(firstGroup).fold(
                    onSuccess = { renderTasks(taskList, it) },
                    onFailure = { renderTasks(taskList, emptyList()) }
                )
                AppContainer.contentRepository.fetchGroupNotices(firstGroup).fold(
                    onSuccess = { renderNotices(noticeList, it) },
                    onFailure = { renderNotices(noticeList, emptyList()) }
                )
            } else {
                renderTasks(taskList, emptyList())
                renderNotices(noticeList, emptyList())
            }
        }
    }

    private fun renderGroups(container: LinearLayout, items: List<GroupResponse>) {
        container.removeAllViews()
        if (items.isEmpty()) {
            addSimpleText(container, getString(R.string.group_empty))
            return
        }
        items.forEach { group ->
            container.addView(buildCard("${group.name ?: "그룹"}", "멤버 ${group.memberCount ?: 0}명") {
                showGroupDetail(group)
            })
        }
    }

    private fun renderTasks(container: LinearLayout, tasks: List<GroupTaskResponse>) {
        container.removeAllViews()
        if (tasks.isEmpty()) {
            addSimpleText(container, "그룹 과제가 없습니다.")
            return
        }
        tasks.forEach { task ->
            val dueText = task.dueDate?.let { " · $it" } ?: ""
            val body = "${task.title ?: ""}$dueText" + (task.description?.let { "\n${it}" } ?: "")
            container.addView(buildCard("그룹 과제", body))
        }
    }

    private fun renderNotices(container: LinearLayout, notices: List<GroupNoticeResponse>) {
        container.removeAllViews()
        if (notices.isEmpty()) {
            addSimpleText(container, "그룹 공지가 없습니다.")
            return
        }
        notices.forEach { notice ->
            val date = notice.createdAt?.let { " ($it)" } ?: ""
            val body = "${notice.title ?: ""}$date\n${notice.content ?: ""}"
            container.addView(buildCard("그룹 공지", body))
        }
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
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(group.name ?: "그룹 상세")
                .setMessage(message)
                .setPositiveButton("닫기", null)
                .show()
        }
    }

    private fun addSimpleText(container: LinearLayout, message: String) {
        val tv = TextView(requireContext()).apply {
            text = message
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 13f
        }
        container.addView(tv)
    }

    private fun showNotificationsDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            val notifications = AppContainer.authRepository.fetchNotifications().getOrElse { emptyList() }
            val groups = AppContainer.contentRepository.fetchGroups().getOrNull().orEmpty()
            if (notifications.isEmpty()) {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("알림")
                    .setMessage("새 알림이 없습니다.")
                    .setPositiveButton("닫기", null)
                    .show()
                return@launch
            }
            fun resolveGroupName(notification: com.example.banpoapple.network.NotificationResponse): String? {
                val url = notification.url ?: return null
                val id = url.substringAfterLast("/groups/", missingDelimiterValue = "").takeWhile { it.isDigit() }
                val groupId = id.toLongOrNull() ?: return null
                return groups.firstOrNull { it.id == groupId }?.name
            }
            val titles = notifications.map {
                resolveGroupName(it)?.let { name -> "${it.title} · $name" } ?: it.title
            }.toTypedArray()
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("알림")
                .setItems(titles, null)
                .setNegativeButton("닫기", null)
                .show()
        }
    }

    private fun navigateToGroups() {
        (activity?.findViewById<BottomNavigationView>(R.id.bottomNav))?.selectedItemId = R.id.menu_groups
    }

    private fun setPagination(container: LinearLayout) {
        container.removeAllViews()
        val tv = TextView(requireContext()).apply {
            text = "1  2  3  4  5"
            setTextColor(resources.getColor(R.color.purple_500, null))
            textSize = 13f
        }
        container.addView(tv)
    }

    private fun buildCard(title: String, body: String, onClick: (() -> Unit)? = null): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            radius = 12f
            cardElevation = 4f
            strokeWidth = 1
            strokeColor = resources.getColor(R.color.sky_stroke, null)
            setCardBackgroundColor(resources.getColor(R.color.card_tint, null))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            layoutParams = lp
            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
            }
            val titleView = TextView(requireContext()).apply {
                text = title
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val bodyView = TextView(requireContext()).apply {
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
        return card
    }
}
