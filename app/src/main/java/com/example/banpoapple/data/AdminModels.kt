package com.example.banpoapple.data

data class AnnouncementAdmin(
    val id: Long,
    val title: String,
    val date: String,
    val content: String
)

data class ReviewAdmin(
    val id: Long,
    val author: String,
    val rating: Int,
    val content: String
)

data class ScheduleSlotAdmin(
    val id: Long,
    val day: String,
    val time: String,
    val note: String?
)

data class GroupAdmin(
    val id: Long,
    val name: String,
    val members: Int,
    val notices: Int,
    val tasks: Int
)
