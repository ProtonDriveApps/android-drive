/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.drivelink.download.domain.manager

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.domain.entity.NetworkType

interface DownloadWorkManager {
    suspend fun download(
        driveLink: DriveLink,
        retryable: Boolean,
        networkType: NetworkType = NetworkType.ANY,
    )
    fun cancel(driveLink: DriveLink)
    suspend fun cancelAll(userId: UserId)
    fun getProgressFlow(driveLink: DriveLink.File): Flow<Percentage>?
}
