package com.example.banpoapple.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.InputType
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.banpoapple.R
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.banpoapple.di.AppContainer
import com.example.banpoapple.auth.GroupMemberInfo
import com.example.banpoapple.network.AdminUserResponse
import com.example.banpoapple.network.GroupNoticeResponse
import com.example.banpoapple.network.GroupResponse
import com.example.banpoapple.network.GroupTaskResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LearningFragment : Fragment() {

    private val groupsPerPage = 5
    private val tasksPerPage = 3
    private val noticesPerPage = 3

    private var groupItems: List<GroupResponse> = emptyList()
    private var taskItems: List<GroupTaskResponse> = emptyList()
    private var noticeItems: List<GroupNoticeResponse> = emptyList()

    private var currentGroupPage = 0
    private var currentTaskPage = 0
    private var currentNoticePage = 0

    private var currentGroupId: Long? = null

    private lateinit var groupList: LinearLayout
    private lateinit var taskList: LinearLayout
    private lateinit var noticeList: LinearLayout
    private lateinit var paginationGroup: LinearLayout
    private lateinit var paginationTask: LinearLayout
    private lateinit var paginationNotice: LinearLayout
    private lateinit var memberList: LinearLayout
    private lateinit var memberGroupSpinner: Spinner
    private var memberGroups: List<GroupResponse> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_learning, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.headerBell)?.setOnClickListener { showNotificationsDropdown() }
        view.findViewById<View>(R.id.headerGroup)?.setOnClickListener { navigateToGroups() }
        groupList = view.findViewById(R.id.learningGroupList)
        taskList = view.findViewById(R.id.learningTaskList)
        noticeList = view.findViewById(R.id.learningNoticeList)
        paginationGroup = view.findViewById(R.id.paginationLearningGroup)
        paginationTask = view.findViewById(R.id.paginationLearningTask)
        paginationNotice = view.findViewById(R.id.paginationLearningNotice)
        memberList = view.findViewById(R.id.learningMemberList)
        memberGroupSpinner = view.findViewById(R.id.learningMemberGroupSpinner)

        val isAdmin = true // TODO wire with actual role when backend exposes it
        val groupAdminActions = view.findViewById<LinearLayout>(R.id.learningGroupAdminActions)
        val taskAdminActions = view.findViewById<LinearLayout>(R.id.learningTaskAdminActions)
        val noticeAdminActions = view.findViewById<LinearLayout>(R.id.learningNoticeAdminActions)
        groupAdminActions.visibility = if (isAdmin) View.VISIBLE else View.GONE
        taskAdminActions.visibility = if (isAdmin) View.VISIBLE else View.GONE
        noticeAdminActions.visibility = if (isAdmin) View.VISIBLE else View.GONE
        if (isAdmin) {
            view.findViewById<View>(R.id.buttonAddGroup).setOnClickListener { showAddGroupDialog() }
            view.findViewById<View>(R.id.buttonDeleteGroup).setOnClickListener { showDeleteGroupDialog() }
            view.findViewById<View>(R.id.buttonAddMember).setOnClickListener { showAddMemberDialog() }
            view.findViewById<View>(R.id.buttonAddTask).setOnClickListener { showAddTaskDialog() }
            view.findViewById<View>(R.id.buttonDeleteTask).setOnClickListener { showDeleteTaskDialog() }
            view.findViewById<View>(R.id.buttonAddNotice).setOnClickListener { showAddNoticeDialog() }
            view.findViewById<View>(R.id.buttonDeleteNotice).setOnClickListener { showDeleteNoticeDialog() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val groups = AppContainer.contentRepository.fetchGroups().getOrNull().orEmpty()
            renderGroups(groupList, groups)
            setupMemberSection(groups)

            val firstGroup = groups.firstOrNull()
            if (firstGroup != null) {
                selectGroup(firstGroup)
            } else {
                currentGroupId = null
                renderTasks(taskList, emptyList())
                renderNotices(noticeList, emptyList())
            }
        }
    }

    private fun renderGroups(container: LinearLayout, items: List<GroupResponse>) {
        container.removeAllViews()
        if (items.isEmpty()) {
            addSimpleText(container, getString(R.string.group_empty))
            paginationGroup.visibility = View.GONE
            groupItems = emptyList()
            return
        }
        groupItems = items
        val totalPages = (items.size + groupsPerPage - 1) / groupsPerPage
        currentGroupPage = currentGroupPage.coerceIn(0, totalPages - 1)
        renderGroupPage(container, currentGroupPage)
        updateGroupPagination(totalPages)
    }

    private fun renderGroupPage(container: LinearLayout, pageIndex: Int) {
        container.removeAllViews()
        val start = pageIndex * groupsPerPage
        val end = minOf(start + groupsPerPage, groupItems.size)
        groupItems.subList(start, end).forEach { group ->
            container.addView(buildCard("${group.name ?: "그룹"}", "멤버 ${group.memberCount ?: 0}명") {
                selectGroup(group)
                showGroupDetail(group)
            })
        }
    }

    private fun renderTasks(container: LinearLayout, tasks: List<GroupTaskResponse>) {
        container.removeAllViews()
        if (tasks.isEmpty()) {
            addSimpleText(container, "그룹 과제가 없습니다.")
            paginationTask.visibility = View.GONE
            taskItems = emptyList()
            return
        }
        taskItems = tasks
        val totalPages = (tasks.size + tasksPerPage - 1) / tasksPerPage
        currentTaskPage = currentTaskPage.coerceIn(0, totalPages - 1)
        renderTaskPage(container, currentTaskPage)
        updateTaskPagination(totalPages)
    }

    private fun renderTaskPage(container: LinearLayout, pageIndex: Int) {
        container.removeAllViews()
        val start = pageIndex * tasksPerPage
        val end = minOf(start + tasksPerPage, taskItems.size)
        taskItems.subList(start, end).forEach { task ->
            val dueText = task.dueDate?.let { " · $it" } ?: ""
            val body = "${task.title ?: ""}$dueText" + (task.description?.let { "\n${it}" } ?: "")
            container.addView(buildCard("그룹 과제", body))
        }
    }

    private fun renderNotices(container: LinearLayout, notices: List<GroupNoticeResponse>) {
        container.removeAllViews()
        if (notices.isEmpty()) {
            addSimpleText(container, "그룹 공지가 없습니다.")
            paginationNotice.visibility = View.GONE
            noticeItems = emptyList()
            return
        }
        noticeItems = notices
        val totalPages = (notices.size + noticesPerPage - 1) / noticesPerPage
        currentNoticePage = currentNoticePage.coerceIn(0, totalPages - 1)
        renderNoticePage(container, currentNoticePage)
        updateNoticePagination(totalPages)
    }

    private fun renderNoticePage(container: LinearLayout, pageIndex: Int) {
        container.removeAllViews()
        val start = pageIndex * noticesPerPage
        val end = minOf(start + noticesPerPage, noticeItems.size)
        noticeItems.subList(start, end).forEach { notice ->
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

    private fun selectGroup(group: GroupResponse) {
        val groupId = group.id ?: return
        currentGroupId = groupId
        currentTaskPage = 0
        currentNoticePage = 0
        syncMemberGroupSelection(groupId)
        viewLifecycleOwner.lifecycleScope.launch {
            AppContainer.contentRepository.fetchGroupTasks(groupId).fold(
                onSuccess = { renderTasks(taskList, it) },
                onFailure = { renderTasks(taskList, emptyList()) }
            )
            AppContainer.contentRepository.fetchGroupNotices(groupId).fold(
                onSuccess = { renderNotices(noticeList, it) },
                onFailure = { renderNotices(noticeList, emptyList()) }
            )
        }
    }

    private fun setupMemberSection(groups: List<GroupResponse>) {
        memberGroups = groups
        if (groups.isEmpty()) {
            memberGroupSpinner.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listOf("그룹 없음")
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            memberGroupSpinner.isEnabled = false
            memberList.removeAllViews()
            addSimpleText(memberList, "그룹이 없습니다.")
            return
        }
        memberGroupSpinner.isEnabled = true
        val labels = groups.map { it.name ?: "그룹 ${it.id ?: ""}" }
        memberGroupSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val selectedIndex = groups.indexOfFirst { it.id == currentGroupId }.let { if (it >= 0) it else 0 }
        memberGroupSpinner.setSelection(selectedIndex)
        memberGroupSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val groupId = groups.getOrNull(position)?.id ?: return
                loadMembersForGroup(groupId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // no-op
            }
        }
        val initialGroupId = groups.getOrNull(selectedIndex)?.id
        if (initialGroupId != null) {
            loadMembersForGroup(initialGroupId)
        }
    }

    private fun syncMemberGroupSelection(groupId: Long) {
        val index = memberGroups.indexOfFirst { it.id == groupId }
        if (index >= 0 && memberGroupSpinner.selectedItemPosition != index) {
            memberGroupSpinner.setSelection(index)
        }
    }

    private fun loadMembersForGroup(groupId: Long) {
        memberList.removeAllViews()
        addSimpleText(memberList, "멤버를 불러오는 중입니다.")
        viewLifecycleOwner.lifecycleScope.launch {
            AppContainer.adminRepository.fetchGroupMembers(groupId).fold(
                onSuccess = { renderMembers(memberList, it) },
                onFailure = { err ->
                    memberList.removeAllViews()
                    addSimpleText(memberList, err.message ?: "멤버를 불러오지 못했습니다.")
                }
            )
        }
    }

    private fun renderMembers(container: LinearLayout, members: List<GroupMemberInfo>) {
        container.removeAllViews()
        if (members.isEmpty()) {
            addSimpleText(container, "멤버가 없습니다.")
            return
        }
        members.forEach { member ->
            val tv = TextView(requireContext()).apply {
                text = buildString {
                    append(member.username)
                    if (member.fullName.isNotBlank()) {
                        append(" (")
                        append(member.fullName)
                        append(")")
                    }
                    if (member.schoolGrade.isNotBlank()) {
                        append("\n")
                        append(member.schoolGrade)
                    }
                }
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 13f
            }
            container.addView(tv)
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

    private fun updatePagination(
        container: LinearLayout,
        totalPages: Int,
        currentIndex: Int,
        onSelect: (Int) -> Unit
    ) {
        container.removeAllViews()
        if (totalPages <= 1) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        for (i in 0 until totalPages) {
            val pageNumber = i + 1
            val tv = TextView(requireContext()).apply {
                text = pageNumber.toString()
                setPadding(12, 6, 12, 6)
                setTextColor(resources.getColor(R.color.purple_500, null))
                alpha = if (i == currentIndex) 1f else 0.5f
                textSize = 13f
                setOnClickListener { onSelect(i) }
            }
            container.addView(tv)
        }
    }

    private fun updateGroupPagination(totalPages: Int) {
        updatePagination(paginationGroup, totalPages, currentGroupPage) { page ->
            currentGroupPage = page
            renderGroupPage(groupList, page)
            updateGroupPagination(totalPages)
        }
    }

    private fun updateTaskPagination(totalPages: Int) {
        updatePagination(paginationTask, totalPages, currentTaskPage) { page ->
            currentTaskPage = page
            renderTaskPage(taskList, page)
            updateTaskPagination(totalPages)
        }
    }

    private fun updateNoticePagination(totalPages: Int) {
        updatePagination(paginationNotice, totalPages, currentNoticePage) { page ->
            currentNoticePage = page
            renderNoticePage(noticeList, page)
            updateNoticePagination(totalPages)
        }
    }

    private fun showAddGroupDialog() {
        val input = EditText(requireContext()).apply {
            hint = "그룹 이름"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("그룹 추가")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                if (name.isBlank()) {
                    Toast.makeText(requireContext(), "그룹 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppContainer.contentRepository.createGroup(name)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(requireContext(), "그룹을 추가했습니다.", Toast.LENGTH_SHORT).show()
                            reloadGroups()
                        },
                        onFailure = { err ->
                            Toast.makeText(requireContext(), err.message ?: "그룹 추가 실패", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAddMemberDialog() {
        if (groupItems.isEmpty()) {
            Toast.makeText(requireContext(), "그룹이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val usersResult = loadAllUsers()
            usersResult.fold(
                onSuccess = { users ->
                    if (users.isEmpty()) {
                        Toast.makeText(requireContext(), "추가할 계정이 없습니다.", Toast.LENGTH_SHORT).show()
                        return@fold
                    }
                    val groupLabels = groupItems.map { it.name ?: "그룹 ${it.id ?: ""}" }
                    val userLabels = users.map {
                        val name = it.fullName?.let { n -> " ($n)" } ?: ""
                        val school = it.schoolName?.let { s -> " · $s" } ?: ""
                        val grade = it.grade?.let { g -> " ${g}학년" } ?: ""
                        "${it.username ?: ""}$name$school$grade"
                    }
                    val groupSpinner = Spinner(requireContext()).apply {
                        adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            groupLabels
                        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    }
                    val currentIndex = groupItems.indexOfFirst { it.id == currentGroupId }
                    if (currentIndex >= 0) {
                        groupSpinner.setSelection(currentIndex)
                    }
                    val userSpinner = Spinner(requireContext()).apply {
                        adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            userLabels
                        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                    }
                    val container = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(32, 8, 32, 0)
                        addView(groupSpinner)
                        addView(userSpinner)
                    }
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("멤버 추가")
                        .setView(container)
                        .setPositiveButton("추가") { _, _ ->
                            val group = groupItems.getOrNull(groupSpinner.selectedItemPosition)
                            val user = users.getOrNull(userSpinner.selectedItemPosition)
                            val groupId = group?.id
                            val userId = user?.id
                            if (groupId == null || userId == null) {
                                Toast.makeText(requireContext(), "선택을 확인해주세요.", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            viewLifecycleOwner.lifecycleScope.launch {
                                val result = AppContainer.contentRepository.addGroupMembers(groupId, listOf(userId))
                                result.fold(
                                    onSuccess = {
                                        Toast.makeText(requireContext(), "멤버를 추가했습니다.", Toast.LENGTH_SHORT).show()
                                        if (currentGroupId == groupId) {
                                            refreshGroupContent(groupId)
                                        }
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(requireContext(), err.message ?: "멤버 추가 실패", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                        .setNegativeButton("취소", null)
                        .show()
                },
                onFailure = { err ->
                    Toast.makeText(requireContext(), err.message ?: "계정 목록 불러오기 실패", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showDeleteGroupDialog() {
        if (groupItems.isEmpty()) {
            Toast.makeText(requireContext(), "삭제할 그룹이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = groupItems.map { it.name ?: "그룹 ${it.id ?: ""}" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("그룹 삭제")
            .setItems(labels) { _, which ->
                val group = groupItems.getOrNull(which) ?: return@setItems
                val groupId = group.id ?: return@setItems
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppContainer.contentRepository.deleteGroup(groupId)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(requireContext(), "그룹을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                            reloadGroups()
                        },
                        onFailure = { err ->
                            Toast.makeText(requireContext(), err.message ?: "그룹 삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAddNoticeDialog() {
        if (groupItems.isEmpty()) {
            Toast.makeText(requireContext(), "그룹이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val groupLabels = groupItems.map { it.name ?: "그룹 ${it.id ?: ""}" }
        val groupSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                groupLabels
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        val currentIndex = groupItems.indexOfFirst { it.id == currentGroupId }
        if (currentIndex >= 0) {
            groupSpinner.setSelection(currentIndex)
        }
        val titleInput = EditText(requireContext()).apply { hint = "제목" }
        val contentInput = EditText(requireContext()).apply {
            hint = "내용"
            minLines = 3
            setSingleLine(false)
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 0)
            addView(groupSpinner)
            addView(titleInput)
            addView(contentInput)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("공지 추가")
            .setView(container)
            .setPositiveButton("추가") { _, _ ->
                val groupId = groupItems.getOrNull(groupSpinner.selectedItemPosition)?.id
                if (groupId == null) {
                    Toast.makeText(requireContext(), "그룹을 선택해주세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val title = titleInput.text?.toString().orEmpty().trim()
                val content = contentInput.text?.toString().orEmpty().trim().ifBlank { null }
                if (title.isBlank()) {
                    Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppContainer.contentRepository.addGroupNotice(groupId, title, content)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(requireContext(), "공지를 추가했습니다.", Toast.LENGTH_SHORT).show()
                            if (currentGroupId == groupId) {
                                refreshGroupContent(groupId)
                            }
                        },
                        onFailure = { err ->
                            Toast.makeText(requireContext(), err.message ?: "공지 추가 실패", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteNoticeDialog() {
        val groupId = currentGroupId
        if (groupId == null) {
            Toast.makeText(requireContext(), "그룹을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (noticeItems.isEmpty()) {
            Toast.makeText(requireContext(), "삭제할 공지가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = noticeItems.map { it.title ?: "공지 ${it.id ?: ""}" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("공지 삭제")
            .setItems(labels) { _, which ->
                val notice = noticeItems.getOrNull(which) ?: return@setItems
                val noticeId = notice.id ?: return@setItems
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppContainer.contentRepository.deleteGroupNotice(groupId, noticeId)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(requireContext(), "공지를 삭제했습니다.", Toast.LENGTH_SHORT).show()
                            refreshGroupContent(groupId)
                        },
                        onFailure = { err ->
                            Toast.makeText(requireContext(), err.message ?: "공지 삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAddTaskDialog() {
        if (groupItems.isEmpty()) {
            Toast.makeText(requireContext(), "그룹이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val groupLabels = groupItems.map { it.name ?: "그룹 ${it.id ?: ""}" }
        val groupSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                groupLabels
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        val currentIndex = groupItems.indexOfFirst { it.id == currentGroupId }
        if (currentIndex >= 0) {
            groupSpinner.setSelection(currentIndex)
        }
        val titleInput = EditText(requireContext()).apply { hint = "제목" }
        val descInput = EditText(requireContext()).apply {
            hint = "설명"
            minLines = 2
            setSingleLine(false)
        }
        val dueInput = EditText(requireContext()).apply {
            hint = "마감일 (yyyy-MM-dd)"
            inputType = InputType.TYPE_CLASS_DATETIME
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 0)
            addView(groupSpinner)
            addView(titleInput)
            addView(descInput)
            addView(dueInput)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("과제 추가")
            .setView(container)
            .setPositiveButton("추가") { _, _ ->
                val groupId = groupItems.getOrNull(groupSpinner.selectedItemPosition)?.id
                if (groupId == null) {
                    Toast.makeText(requireContext(), "그룹을 선택해주세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val title = titleInput.text?.toString().orEmpty().trim()
                val desc = descInput.text?.toString().orEmpty().trim().ifBlank { null }
                val due = dueInput.text?.toString().orEmpty().trim().ifBlank { null }
                if (title.isBlank()) {
                    Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!due.isNullOrBlank() && !Regex("""\d{4}-\d{2}-\d{2}""").matches(due)) {
                    Toast.makeText(requireContext(), "마감일 형식을 확인해주세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppContainer.contentRepository.addGroupTask(groupId, title, desc, due)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(requireContext(), "과제를 추가했습니다.", Toast.LENGTH_SHORT).show()
                            if (currentGroupId == groupId) {
                                refreshGroupContent(groupId)
                            }
                        },
                        onFailure = { err ->
                            Toast.makeText(requireContext(), err.message ?: "과제 추가 실패", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteTaskDialog() {
        val groupId = currentGroupId
        if (groupId == null) {
            Toast.makeText(requireContext(), "그룹을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (taskItems.isEmpty()) {
            Toast.makeText(requireContext(), "삭제할 과제가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = taskItems.map { it.title ?: "과제 ${it.id ?: ""}" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("과제 삭제")
            .setItems(labels) { _, which ->
                val task = taskItems.getOrNull(which) ?: return@setItems
                val taskId = task.id ?: return@setItems
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = AppContainer.contentRepository.deleteGroupTask(groupId, taskId)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(requireContext(), "과제를 삭제했습니다.", Toast.LENGTH_SHORT).show()
                            refreshGroupContent(groupId)
                        },
                        onFailure = { err ->
                            Toast.makeText(requireContext(), err.message ?: "과제 삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun reloadGroups() {
        viewLifecycleOwner.lifecycleScope.launch {
            val groups = AppContainer.contentRepository.fetchGroups().getOrNull().orEmpty()
            renderGroups(groupList, groups)
            setupMemberSection(groups)
            val nextGroup = groups.firstOrNull()
            if (nextGroup != null) {
                selectGroup(nextGroup)
            } else {
                currentGroupId = null
                renderTasks(taskList, emptyList())
                renderNotices(noticeList, emptyList())
            }
        }
    }

    private fun refreshGroupContent(groupId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            AppContainer.contentRepository.fetchGroupTasks(groupId).fold(
                onSuccess = { renderTasks(taskList, it) },
                onFailure = { renderTasks(taskList, emptyList()) }
            )
            AppContainer.contentRepository.fetchGroupNotices(groupId).fold(
                onSuccess = { renderNotices(noticeList, it) },
                onFailure = { renderNotices(noticeList, emptyList()) }
            )
        }
    }

    private suspend fun loadAllUsers(): Result<List<AdminUserResponse>> = withContext(Dispatchers.IO) {
        val collected = mutableListOf<AdminUserResponse>()
        var page = 0
        var totalPages = 1
        while (page < totalPages) {
            val result = AppContainer.adminRepository.fetchUsers(null, null, page, 50)
            val response = result.getOrElse { return@withContext Result.failure(it) }
            collected.addAll(response.content)
            totalPages = response.totalPages ?: 0
            if (totalPages == 0) break
            page += 1
        }
        Result.success(collected)
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
