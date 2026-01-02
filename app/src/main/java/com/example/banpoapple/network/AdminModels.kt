package com.example.banpoapple.network

data class AdminUserResponse(
    val id: Long?,
    val username: String?,
    val fullName: String?,
    val schoolName: String?,
    val grade: String?,
    val accountType: String?,
    val role: String?
)

data class PagedResponse<T>(
    val content: List<T> = emptyList(),
    val totalPages: Int? = 0,
    val totalElements: Long? = 0,
    val number: Int? = 0
)
