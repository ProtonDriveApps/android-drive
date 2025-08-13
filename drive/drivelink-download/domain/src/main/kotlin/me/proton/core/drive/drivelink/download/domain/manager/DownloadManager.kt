/*
 * Copyright (c) 2025 Proton AG.
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
import kotlin.coroutines.CoroutineContext

interface DownloadManager {
    suspend fun download(driveLink: DriveLink, priority: Long, retryable: Boolean)
    suspend fun cancel(driveLink: DriveLink)
    suspend fun cancelAll(userId: UserId)
    fun getProgressFlow(driveLink: DriveLink.File): Flow<Percentage>?

    interface FileDownloader {
        suspend fun start(userId: UserId, coroutineContext: CoroutineContext): Result<Unit>
        suspend fun stop(userId: UserId): Result<Unit>
    }
}
