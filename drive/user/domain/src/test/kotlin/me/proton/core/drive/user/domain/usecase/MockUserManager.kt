/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.user.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.proton.android.drive.db.DriveDatabase
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.db.test.userId
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User

@Suppress("TestFunctionName")
fun MockUserManager(database: DriveDatabase): UserManager {
    return mockk {
        coEvery { observeUser(userId) } coAnswers {
            val firstArg = firstArg<UserId>()
            database.userDao().observeByUserId(firstArg).filterNotNull().map { entity ->
                entity.toUser()
            }
        }
        coEvery { getUser(userId) } coAnswers {
            val firstArg = firstArg<UserId>()
            requireNotNull(database.userDao().getByUserId(firstArg)?.toUser())
        }
    }
}

private fun UserEntity.toUser() = User(
    userId = userId,
    email = null,
    name = name,
    displayName = displayName,
    currency = currency,
    credit = credit,
    usedSpace = usedSpace,
    maxSpace = maxSpace,
    maxUpload = maxUpload,
    role = null,
    private = isPrivate,
    services = services,
    subscribed = subscribed,
    delinquent = null,
    keys = emptyList(),
    recovery = null,
    createdAtUtc = 0,
    type = null,
)
