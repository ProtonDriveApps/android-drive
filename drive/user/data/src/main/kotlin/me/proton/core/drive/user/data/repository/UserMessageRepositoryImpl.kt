/*
 * Copyright (c) 2023-2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.user.data.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.user.data.db.UserMessageDatabase
import me.proton.core.drive.user.data.db.entity.DismissedUserMessageEntity
import me.proton.core.drive.user.domain.entity.UserMessage
import me.proton.core.drive.user.domain.repository.UserMessageRepository
import javax.inject.Inject

class UserMessageRepositoryImpl @Inject constructor(
    private val database: UserMessageDatabase,
) : UserMessageRepository {

    override fun exists(userId: UserId, message: UserMessage): Flow<Boolean> =
        database.userMessageDao.exists(userId, message)

    override suspend fun insertOrUpdate(userId: UserId, message: UserMessage) {
        database.userMessageDao.insertOrUpdate(
            DismissedUserMessageEntity(
                userId = userId,
                userMessage = message,
                timestampS = TimestampS().value
            )
        )
    }
}
