package com.example.banpoapple.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

data class NotificationResponse(
    val id: Long?,
    val title: String,
    val body: String,
    val url: String?,
    val read: Boolean? = null
)

interface BackendApi {
    @GET("api/mobile/home")
    suspend fun getHomeFeed(): HomeFeedResponse

    @GET("api/notifications/due")
    suspend fun getNotifications(): List<NotificationResponse>

    @GET("api/notices")
    suspend fun getNotices(): List<NoticeResponse>

    @GET("api/notices/{id}")
    suspend fun getNotice(@Path("id") id: Long): NoticeResponse

    @GET("api/reviews")
    suspend fun getReviews(): List<ReviewResponse>

    @GET("api/schedules")
    suspend fun getSchedules(): List<ScheduleSlotResponse>

    @GET("api/groups")
    suspend fun getGroups(): List<GroupResponse>

    @GET("api/groups/{groupId}/notices")
    suspend fun getGroupNotices(@Path("groupId") groupId: Long): List<GroupNoticeResponse>

    @GET("api/groups/{groupId}/tasks")
    suspend fun getGroupTasks(@Path("groupId") groupId: Long): List<GroupTaskResponse>

    @POST("api/admin/users/{username}/reset-password")
    suspend fun resetPassword(@Path("username") username: String)

    @GET("api/admin/users")
    suspend fun getAdminUsers(
        @Query("school") school: String? = null,
        @Query("grade") grade: String? = null,
        @Query("accountType") accountType: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<AdminUserResponse>
}
