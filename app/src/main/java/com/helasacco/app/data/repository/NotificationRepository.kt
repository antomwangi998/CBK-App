package com.helasacco.app.data.repository

import com.helasacco.app.data.local.dao.NotificationDao
import com.helasacco.app.data.local.entities.toDomain
import com.helasacco.app.domain.model.Notification
import com.helasacco.app.ui.admin.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
) : NotificationRepository {

    override fun getForUser(userId: String, memberId: String?): Flow<List<Notification>> =
        notificationDao.getForUser(userId, memberId).map { list -> list.map { it.toDomain() } }

    override fun getUnreadCount(userId: String): Flow<Int> =
        notificationDao.getUnreadCount(userId)

    override suspend fun markRead(id: String) {
        notificationDao.markRead(id, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    override suspend fun markAllRead(userId: String) {
        notificationDao.markAllRead(userId)
    }
}
