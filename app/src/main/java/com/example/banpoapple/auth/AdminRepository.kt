package com.example.banpoapple.auth

import com.example.banpoapple.BuildConfig
import com.example.banpoapple.network.AdminUserResponse
import com.example.banpoapple.network.BackendApi
import com.example.banpoapple.network.PagedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class GroupMemberInfo(
    val username: String,
    val fullName: String,
    val schoolGrade: String
)

class AdminRepository(
    private val api: BackendApi,
    private val client: OkHttpClient,
    private val demoMode: Boolean
) {
    suspend fun fetchUsers(
        school: String?,
        grade: String?,
        page: Int = 0,
        size: Int = 20
    ): Result<PagedResponse<AdminUserResponse>> = withContext(Dispatchers.IO) {
        runCatching { api.getAdminUsers(school, grade, null, page, size) }
    }

    suspend fun resetPassword(username: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { api.resetPassword(username) }
    }

    suspend fun fetchGroupMembers(groupId: Long): Result<List<GroupMemberInfo>> = withContext(Dispatchers.IO) {
        if (demoMode) {
            return@withContext Result.success(
                listOf(
                    GroupMemberInfo("demo_user", "데모", "데모학교 / 1학년")
                )
            )
        }
        try {
            val request = Request.Builder()
                .url("${BuildConfig.BASE_URL}admin/members?groupId=$groupId")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext Result.failure(IllegalStateException("멤버 불러오기 실패 (code ${response.code})"))
            }
            val body = response.body?.string().orEmpty()
            response.close()
            Result.success(parseMemberList(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseMemberList(html: String): List<GroupMemberInfo> {
        val pattern = Regex(
            """class="table-row"[^>]*>.*?class="title-md"[^>]*>(.*?)</div>.*?class="muted"[^>]*>(.*?)</div>.*?class="muted"[^>]*>(.*?)</div>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return pattern.findAll(html).mapNotNull { match ->
            val username = cleanText(match.groupValues.getOrNull(1))
            val fullName = cleanText(match.groupValues.getOrNull(2))
            val schoolGrade = cleanText(match.groupValues.getOrNull(3))
            if (username.isBlank()) null else GroupMemberInfo(username, fullName, schoolGrade)
        }.toList()
    }

    private fun cleanText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val noTags = raw.replace(Regex("<[^>]+>"), " ")
        return decodeHtml(noTags).replace(Regex("\\s+"), " ").trim()
    }

    private fun decodeHtml(value: String): String {
        return value
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }
}
