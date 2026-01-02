package com.example.banpoapple.auth

import com.example.banpoapple.BuildConfig
import com.example.banpoapple.network.BackendApi
import com.example.banpoapple.network.NotificationResponse
import com.example.banpoapple.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val client: OkHttpClient,
    private val api: BackendApi,
    private val demoMode: Boolean
) {

    suspend fun login(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (demoMode) {
            return@withContext if (username.isBlank() || password.isBlank()) {
                Result.failure(IllegalArgumentException("아이디와 비밀번호를 입력하세요."))
            } else {
                Result.success(Unit)
            }
        }

        try {
            // CSRF 토큰을 login 페이지에서 추출
            val csrfToken = fetchCsrfToken()

            val formBuilder = FormBody.Builder()
                .add("username", username)
                .add("password", password)
            if (csrfToken != null) {
                formBuilder.add("_csrf", csrfToken)
            }
            val form = formBuilder.build()

            val request = Request.Builder()
                .url("${BuildConfig.BASE_URL}login")
                .post(form)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code !in 300..399) {
                    return@withContext Result.failure(IllegalStateException("로그인 실패 (code ${response.code})"))
                }
            }

            // 세션 확인
            api.getNotifications()
            Result.success(Unit)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                Result.failure(IllegalArgumentException("아이디 또는 비밀번호를 확인해주세요."))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchCsrfToken(): String? {
        return try {
            client.newCall(
                Request.Builder()
                    .url("${BuildConfig.BASE_URL}login")
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

    suspend fun fetchNotifications(): Result<List<NotificationResponse>> = withContext(Dispatchers.IO) {
        if (demoMode) {
            val sample = listOf(
                NotificationResponse(1, "주간 리포트 제출", "토요일 23:00까지 업로드", "https://banpoapplescience.onrender.com/reports", read = false),
                NotificationResponse(2, "스터디 모임", "수요일 19:00, 2층 세미나실", null, read = true),
                NotificationResponse(3, "퀴즈 리마인드", "이번 주 과학 퀴즈 풀기", "https://banpoapplescience.onrender.com/quizzes", read = false)
            )
            return@withContext Result.success(sample)
        }

        try {
            Result.success(api.getNotifications())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(username: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (username.isBlank()) return@withContext Result.failure(IllegalArgumentException("아이디를 입력해주세요."))
        if (demoMode) return@withContext Result.success(Unit)
        try {
            api.resetPassword(username.trim())
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception("초기화 실패: ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
