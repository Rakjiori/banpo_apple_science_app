package com.example.banpoapple.network

data class NoticeResponse(
    val id: Long?,
    val title: String?,
    val content: String?,
    val createdAt: String?
)

data class ReviewResponse(
    val id: Long?,
    val author: String?,
    val rating: Int?,
    val content: String?,
    val createdAt: String?
)

data class ScheduleSlotResponse(
    val id: Long?,
    val dayOfWeek: String?,
    val startTime: String?,
    val endTime: String?,
    val subject: String?,
    val courseType: String?,
    val school: String?,
    val note: String?
)

data class GroupResponse(
    val id: Long?,
    val name: String?,
    val joinCode: String?,
    val memberCount: Int?
)

data class GroupNoticeResponse(
    val id: Long?,
    val title: String?,
    val content: String?,
    val createdAt: String?,
    val author: String?
)

data class GroupTaskResponse(
    val id: Long?,
    val title: String?,
    val description: String?,
    val dueDate: String?,
    val createdAt: String?,
    val author: String?
)

data class HomeFeedResponse(
    val notices: List<NoticeResponse> = emptyList(),
    val reviews: List<ReviewResponse> = emptyList(),
    val schedules: List<ScheduleSlotResponse> = emptyList(),
    val groups: List<GroupResponse> = emptyList()
)
