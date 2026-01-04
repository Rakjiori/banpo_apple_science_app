package com.example.banpoapple.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.content.Intent
import com.example.banpoapple.ui.login.LoginActivity
import com.example.banpoapple.R
import androidx.lifecycle.lifecycleScope
import com.example.banpoapple.di.AppContainer
import com.example.banpoapple.network.GroupResponse
import com.example.banpoapple.network.NoticeResponse
import com.example.banpoapple.network.ReviewResponse
import com.example.banpoapple.network.ScheduleSlotResponse
import com.example.banpoapple.ui.main.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.example.banpoapple.network.AdminUserResponse
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.headerBell)?.setOnClickListener { openNotificationDropdown() }
        view.findViewById<View>(R.id.headerGroup)?.setOnClickListener { navigateToGroups() }

        val announcementList = view.findViewById<LinearLayout>(R.id.adminAnnouncementList)
        val reviewList = view.findViewById<LinearLayout>(R.id.adminReviewList)
        val scheduleList = view.findViewById<LinearLayout>(R.id.adminScheduleList)
        val groupList = view.findViewById<LinearLayout>(R.id.adminGroupList)
        val userList = view.findViewById<LinearLayout>(R.id.adminUserList)
        val filterSchool = view.findViewById<TextInputEditText>(R.id.inputFilterSchool)
        val filterGrade = view.findViewById<TextInputEditText>(R.id.inputFilterGrade)

        viewLifecycleOwner.lifecycleScope.launch {
            val repo = AppContainer.contentRepository

            val announcements = repo.fetchNotices().getOrNull().orEmpty()
            renderAnnouncements(announcementList, announcements)
            renderPagination(view.findViewById(R.id.paginationAnnouncements), announcements.size)

            val reviews = repo.fetchReviews().getOrNull().orEmpty()
            renderReviews(reviewList, reviews)
            renderPagination(view.findViewById(R.id.paginationReviews), reviews.size)

            val schedules = repo.fetchSchedules().getOrNull().orEmpty()
            renderSchedules(scheduleList, schedules)

            val groups = repo.fetchGroups().getOrNull().orEmpty()
            renderGroups(groupList, groups)

            loadUsers(userList, filterSchool.text?.toString(), filterGrade.text?.toString(), 0)
        }

        view.findViewById<Button>(R.id.buttonLogout)?.setOnClickListener { performLogout() }
        view.findViewById<Button>(R.id.buttonEditProfile)?.setOnClickListener { openProfileEdit() }
        view.findViewById<View>(R.id.buttonAddGroup)?.setOnClickListener { showAdminAction("그룹 추가") }
        view.findViewById<View>(R.id.buttonAddNotice)?.setOnClickListener { showAdminAction("공지 등록") }
        view.findViewById<View>(R.id.buttonAddTask)?.setOnClickListener { showAdminAction("과제 등록") }
        view.findViewById<View>(R.id.buttonAddMember)?.setOnClickListener { showAdminAction("멤버 추가") }
        view.findViewById<View>(R.id.buttonResetPassword)?.setOnClickListener { promptPasswordReset() }
        view.findViewById<View>(R.id.buttonLoadUsers)?.setOnClickListener {
            loadUsers(userList, filterSchool.text?.toString(), filterGrade.text?.toString(), 0)
        }
    }

    private fun renderAnnouncements(container: LinearLayout, items: List<NoticeResponse>) {
        container.removeAllViews()
        val ctx = requireContext()
        if (items.isEmpty()) {
            addEmptyState(container, getString(R.string.announcement_empty))
            return
        }
        items.forEachIndexed { idx, a ->
            val tv = TextView(ctx).apply {
                val date = a.createdAt?.let { " · $it" } ?: ""
                text = "[공지 ${idx + 1}] ${a.title}${date}\n${a.content}"
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 14f
            }
            container.addView(tv)
        }
    }

    private fun renderReviews(container: LinearLayout, items: List<ReviewResponse>) {
        container.removeAllViews()
        val ctx = requireContext()
        if (items.isEmpty()) {
            addEmptyState(container, getString(R.string.review_empty))
            return
        }
        items.forEach { r ->
            val tv = TextView(ctx).apply {
                text = "⭐ ${r.rating ?: "-"} / ${r.author ?: ""}\n${r.content ?: ""}"
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 14f
            }
            container.addView(tv)
        }
    }

    private fun renderSchedules(container: LinearLayout, items: List<ScheduleSlotResponse>) {
        container.removeAllViews()
        val ctx = requireContext()
        if (items.isEmpty()) {
            addEmptyState(container, getString(R.string.schedule_empty))
            return
        }
        items.forEach { s ->
            val tv = TextView(ctx).apply {
                val title = s.subject ?: "수업"
                val time = listOfNotNull(s.startTime, s.endTime).joinToString(" - ")
                val day = s.dayOfWeek ?: "요일 미정"
                val note = s.note?.let { "\n$it" } ?: ""
                text = "$day · $title ($time)$note"
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 14f
            }
            container.addView(tv)
        }
    }

    private fun renderGroups(container: LinearLayout, items: List<GroupResponse>) {
        container.removeAllViews()
        val ctx = requireContext()
        if (items.isEmpty()) {
            addEmptyState(container, getString(R.string.group_empty))
            return
        }
        items.forEach { g ->
            val tv = TextView(ctx).apply {
                text = "[${g.name ?: "그룹"}] 멤버 ${g.memberCount ?: 0}명"
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 14f
            }
            container.addView(tv)
        }
    }

    private fun addEmptyState(container: LinearLayout, message: String) {
        val tv = TextView(requireContext()).apply {
            text = message
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 13f
        }
        container.addView(tv)
    }

    private fun renderPagination(container: LinearLayout, totalPages: Int, onSelect: ((Int) -> Unit)? = null) {
        container.removeAllViews()
        if (totalPages <= 1) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val pages = (0 until totalPages).map { it + 1 }
        pages.forEachIndexed { index, page ->
            val tv = TextView(requireContext()).apply {
                text = page.toString()
                setPadding(16, 8, 16, 8)
                setTextColor(resources.getColor(if (index == 0) R.color.purple_500 else R.color.text_primary, null))
                textSize = 14f
                setOnClickListener { onSelect?.invoke(page - 1) }
            }
            container.addView(tv)
        }
    }

    private fun openProfileEdit() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ProfileFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun performLogout() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun showAdminAction(action: String) {
        android.widget.Toast.makeText(requireContext(), "$action 준비 중", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun promptPasswordReset() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "아이디"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("비밀번호 초기화")
            .setMessage("입력한 계정의 비밀번호를 apple 로 초기화합니다.")
            .setView(input)
            .setPositiveButton("초기화") { _, _ ->
                val username = input.text?.toString().orEmpty()
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppContainer.adminRepository.resetPassword(username)
                    result.fold(
                        onSuccess = {
                            android.widget.Toast.makeText(requireContext(), "${username}의 비밀번호가 apple 로 초기화되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { err ->
                            android.widget.Toast.makeText(requireContext(), err.message ?: "초기화 실패", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun loadUsers(container: LinearLayout, school: String?, grade: String?, page: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            AppContainer.adminRepository.fetchUsers(school, grade, page).fold(
                onSuccess = { renderUsers(container, it.content, it.totalPages ?: 0) },
                onFailure = { err ->
                    container.removeAllViews()
                    addEmptyState(container, err.message ?: "계정 목록을 불러오지 못했습니다.")
                }
            )
        }
    }

    private fun renderUsers(container: LinearLayout, users: List<AdminUserResponse>, totalPages: Int) {
        container.removeAllViews()
        if (users.isEmpty()) {
            addEmptyState(container, "계정이 없습니다.")
            return
        }
        users.forEach { user ->
            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 12f
                cardElevation = 4f
                strokeWidth = 1
                strokeColor = resources.getColor(R.color.sky_stroke, null)
                setCardBackgroundColor(resources.getColor(R.color.card_tint, null))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
                layoutParams = lp

                val inner = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 12, 16, 12)
                }
                val title = TextView(requireContext()).apply {
                    text = "${user.username ?: ""} (${user.fullName ?: ""})"
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                val body = TextView(requireContext()).apply {
                    text = "${user.schoolName ?: "-"} / ${user.grade ?: "-"} / ${user.accountType ?: "-"} / ${user.role ?: "-"}"
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    textSize = 13f
                    setPadding(0, 4, 0, 0)
                }
                val resetBtn = com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = "비밀번호 apple로 초기화"
                    setTextColor(resources.getColor(R.color.purple_500, null))
                    setOnClickListener { resetUserPassword(user.username) }
                }
                inner.addView(title)
                inner.addView(body)
                inner.addView(resetBtn)
                addView(inner)
            }
            container.addView(card)
        }
        renderPagination(requireView().findViewById(R.id.paginationUsers), totalPages) { page ->
            val school = requireView().findViewById<TextInputEditText>(R.id.inputFilterSchool).text?.toString()
            val grade = requireView().findViewById<TextInputEditText>(R.id.inputFilterGrade).text?.toString()
            loadUsers(container, school, grade, page)
        }
    }

    private fun resetUserPassword(username: String?) {
        if (username.isNullOrBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = AppContainer.adminRepository.resetPassword(username)
            result.fold(
                onSuccess = {
                    android.widget.Toast.makeText(requireContext(), "${username}의 비밀번호가 apple 로 초기화되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    android.widget.Toast.makeText(requireContext(), err.message ?: "초기화 실패", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun openNotificationDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            val notifications = AppContainer.authRepository.fetchNotifications().getOrElse { emptyList() }
            if (notifications.isEmpty()) {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("알림")
                    .setMessage("새 알림이 없습니다.")
                    .setPositiveButton("닫기", null)
                    .show()
                return@launch
            }
            val titles = notifications.map { it.title }.toTypedArray()
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
}
