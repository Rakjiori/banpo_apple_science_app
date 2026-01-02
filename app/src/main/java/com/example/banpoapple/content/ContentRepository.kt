package com.example.banpoapple.content

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

class ContentRepository(
    private val api: BackendApi
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
}
