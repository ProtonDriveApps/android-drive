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

package me.proton.core.drive.files.data.operation

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.files.data.operation.move.worker.MoveFileWorker
import me.proton.core.drive.files.domain.operation.FileOperationManager
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Singleton
class FileOperationManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val selectLinks: SelectLinks,
) : FileOperationManager {

    private val String.uniqueMoveWorkName: String get() = "moving=$this"

    override suspend fun changeParent(userId: UserId, linkIds: List<LinkId>, folderId: FolderId, allowUndo: Boolean) {
        val selectionId = selectLinks(linkIds).getOrThrow()
        workManager.enqueueUniqueWork(
            selectionId.id.uniqueMoveWorkName,
            ExistingWorkPolicy.KEEP,
            MoveFileWorker.getWorkRequest(
                userId = userId,
                selectionId = selectionId,
                folderId = folderId,
                allowUndo = allowUndo,
            )
        )
    }
}
