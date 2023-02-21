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
package me.proton.core.drive.folder.create.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.folder.CreateFolderInfo
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.folder.domain.repository.FolderRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class CreateFolder @Inject constructor(
    private val folderRepository: FolderRepository,
    private val getLink: GetLink,
    private val createFolderInfo: CreateFolderInfo,
    private val updateEventAction: UpdateEventAction,
) {
    suspend operator fun invoke(
        parentFolder: Link.Folder,
        folderName: String,
    ): Result<Pair<String, FolderId>> = coRunCatching {
        updateEventAction(
            shareId = parentFolder.id.shareId,
        ) {
            val (name, folderInfo) = createFolderInfo(parentFolder, folderName).getOrThrow()
            name to folderRepository.createFolder(
                shareId = parentFolder.id.shareId,
                folderInfo = folderInfo
            ).getOrThrow()
        }
    }

    suspend operator fun invoke(
        parentFolderId: FolderId,
        folderName: String,
    ): Result<Pair<String, FolderId>> = coRunCatching {
        invoke(
            parentFolder = getLink(parentFolderId).toResult().getOrThrow(),
            folderName = folderName,
        ).getOrThrow()
    }
}
