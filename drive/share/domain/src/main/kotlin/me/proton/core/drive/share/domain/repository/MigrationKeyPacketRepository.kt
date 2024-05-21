/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.share.domain.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.share.domain.entity.ShareAccessWithNode
import me.proton.core.drive.share.domain.entity.ShareId

interface MigrationKeyPacketRepository {
    suspend fun getAll(userId: UserId): Result<List<ShareId>>

    suspend fun update(
        userId: UserId,
        shareAccessWithNode: ShareAccessWithNode,
    ): Result<List<String>>

    suspend fun getLastUpdate(userId: UserId): TimestampS?

    suspend fun setLastUpdate(userId: UserId, date: TimestampS)

}
