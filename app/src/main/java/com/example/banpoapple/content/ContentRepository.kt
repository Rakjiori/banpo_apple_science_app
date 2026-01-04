package com.example.banpoapple.content

import com.example.banpoapple.BuildConfig
import com.example.banpoapple.network.BackendApi
import com.example.banpoapple.network.GroupNoticeResponse
import com.example.banpoapple.network.GroupResponse
import com.example.banpoapple.network.GroupTaskResponse
import com.example.banpoapple.network.HomeFeedResponse
import com.example.banpoapple.network.NoticeResponse
import com.example.banpoapple.network.ReviewResponse
import com.example.banpoapple.network.ScheduleSlotResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class ContentRepository(
    private val api: BackendApi,
    private val client: OkHttpClient,
    private val demoMode: Boolean
) {
    suspend fun fetchHomeFeed(): Result<HomeFeedResponse> = withContext(Dispatchers.IO) {
        runCatching { api.getHomeFeed() }
    }

    suspend fun fetchNotices(): Result<List<NoticeResponse>> = withContext(Dispatchers.IO) {
        runCatching { api.getNotices() }
    }

    suspend fun fetchReviews(): Result<List<ReviewResponse>> = withContext(Dispatchers.IO) {
        runCatching { api.getReviews() }
    }

    suspend fun fetchSchedules(): Result<List<ScheduleSlotResponse>> = withContext(Dispatchers.IO) {
        runCatching { api.getSchedules() }
    }

    suspend fun fetchGroups(): Result<List<GroupResponse>> = withContext(Dispatchers.IO) {
        runCatching { api.getGroups() }
    }

    suspend fun fetchGroupNotices(groupId: Long): Result<List<GroupNoticeResponse>> = withContext(Dispatchers.IO) {
        runCatching { api.getGroupNotices(groupId) }
    }

    suspend fun fetchGroupTasks(groupId: Long): Result<List<GroupTaskResponse>> = withContext(Dispatchers.IO) {
        runCatching { api.getGroupTasks(groupId) }
    }

    suspend fun createGroup(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        postForm("groups", mapOf("name" to name))
    }

    suspend fun deleteGroup(groupId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        postForm("groups/$groupId/delete", emptyMap())
    }

    suspend fun addGroupNotice(groupId: Long, title: String, content: String?): Result<Unit> = withContext(Dispatchers.IO) {
        postForm("groups/$groupId/notices", mapOf("title" to title, "content" to content))
    }

    suspend fun deleteGroupNotice(groupId: Long, noticeId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        postForm("groups/$groupId/notices/$noticeId/delete", emptyMap())
    }

    suspend fun addGroupTask(groupId: Long, title: String, description: String?, dueDate: String?): Result<Unit> = withContext(Dispatchers.IO) {
        postForm("groups/$groupId/tasks", mapOf("title" to title, "description" to description, "dueDate" to dueDate))
    }

    suspend fun deleteGroupTask(groupId: Long, taskId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        postForm("groups/$groupId/tasks/$taskId/delete", emptyMap())
    }

    suspend fun addGroupMembers(groupId: Long, userIds: List<Long>): Result<Unit> = withContext(Dispatchers.IO) {
        val values = userIds.map { it.toString() }
        postForm("groups/$groupId/members/invite", emptyMap(), mapOf("userIds" to values))
    }

    private fun postForm(
        path: String,
        fields: Map<String, String?>,
        listFields: Map<String, List<String>> = emptyMap()
    ): Result<Unit> {
        if (demoMode) return Result.success(Unit)
        return try {
            val csrfToken = fetchCsrfToken()
            val formBuilder = FormBody.Builder()
            fields.forEach { (key, value) ->
                if (!value.isNullOrBlank()) {
                    formBuilder.add(key, value)
                }
            }
            listFields.forEach { (key, values) ->
                values.forEach { value ->
                    if (value.isNotBlank()) {
                        formBuilder.add(key, value)
                    }
                }
            }
            if (!csrfToken.isNullOrBlank()) {
                formBuilder.add("_csrf", csrfToken)
            }
            val request = Request.Builder()
                .url("${BuildConfig.BASE_URL}$path")
                .post(formBuilder.build())
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code !in 300..399) {
                    return Result.failure(IllegalStateException("요청 실패 (code ${response.code})"))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchCsrfToken(): String? {
        return try {
            client.newCall(
                Request.Builder()
                    .url("${BuildConfig.BASE_URL}groups")
                    .get()
                    .build()
            ).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val regex = Regex("""name="_csrf" value="([^"]+)"""")
                regex.find(body)?.groupValues?.getOrNull(1)
            }
        } catch (_: IOException) {
            null
        }
    }
}
