/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

/**
 * Emits each backup folder of the user once it has a [BackupErrorType.FOLDER_NOT_FOUND] error,
 * and again if the error comes back after being cleared.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveFolderNotFound @Inject constructor(
    private val getFoldersFlow: GetFoldersFlow,
    private val getErrors: GetErrors,
) {
    operator fun invoke(userId: UserId): Flow<FolderId> = getFoldersFlow(userId)
        .map { backupFolders -> backupFolders.map { backupFolder -> backupFolder.folderId }.toSet() }
        .distinctUntilChanged()
        .flatMapLatest { folderIds ->
            if (folderIds.isEmpty()) {
                emptyFlow()
            } else {
                folderIds.map { folderId ->
                    getErrors(folderId)
                        .map { errors -> errors.any { error -> error.type == BackupErrorType.FOLDER_NOT_FOUND } }
                        .distinctUntilChanged()
                        .filter { folderNotFound -> folderNotFound }
                        .map { folderId }
                }.merge()
            }
        }
}
