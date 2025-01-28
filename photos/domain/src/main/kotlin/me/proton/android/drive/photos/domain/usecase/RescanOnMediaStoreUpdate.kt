/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.photos.domain.usecase

import me.proton.android.drive.photos.domain.repository.MediaStoreVersionRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.usecase.GetAllFolders
import me.proton.core.drive.backup.domain.usecase.RescanAllFolders
import me.proton.core.drive.base.domain.extension.throwOnFailure
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class RescanOnMediaStoreUpdate @Inject constructor(
    private val mediaStoreVersionRepository: MediaStoreVersionRepository,
    private val getAllFolders: GetAllFolders,
    private val rescanAllFolders: RescanAllFolders,
) {
    suspend operator fun invoke(userId: UserId) = coRunCatching {
        val last = mediaStoreVersionRepository.getLastVersion(userId)
        val current = mediaStoreVersionRepository.getCurrentVersion()
        if (last != null && current != null && last != current) {
            CoreLogger.i(BACKUP, "Rescan all folders after media store updates: $last, $current")
            getAllFolders(userId)
                .getOrThrow()
                .map { backupFolder -> backupFolder.folderId }
                .distinct().map { folderId ->
                    rescanAllFolders(folderId)
                }.throwOnFailure { count ->
                    "Failed to rescan, for $count folders"
                }
        } else {
            CoreLogger.d(BACKUP, "No update from media store: $last, $current")
        }
        mediaStoreVersionRepository.setLastVersion(userId, current)
    }
}
