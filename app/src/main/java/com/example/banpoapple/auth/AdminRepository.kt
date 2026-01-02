package com.example.banpoapple.auth

import com.example.banpoapple.network.AdminUserResponse
import com.example.banpoapple.network.BackendApi
import com.example.banpoapple.network.PagedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminRepository(
    private val api: BackendApi
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
}
