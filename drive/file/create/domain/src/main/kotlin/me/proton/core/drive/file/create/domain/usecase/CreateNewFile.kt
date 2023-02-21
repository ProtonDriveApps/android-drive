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
package me.proton.core.drive.file.create.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.CreateNewFileInfo
import me.proton.core.drive.file.base.domain.entity.FileInfo
import me.proton.core.drive.file.base.domain.entity.NewFileInfo
import me.proton.core.drive.file.base.domain.repository.FileRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class CreateNewFile @Inject constructor(
    private val createNewFileInfo: CreateNewFileInfo,
    private val getLink: GetLink,
    private val fileRepository: FileRepository,
) {
    suspend operator fun invoke(
        parentFolder: Link.Folder,
        name: String,
        mimeType: String
    ): Result<Pair<NewFileInfo, FileInfo>> = coRunCatching {
        val newFileInfo = createNewFileInfo(
            folder = parentFolder,
            name = name,
            mimeType = mimeType,
        ).getOrThrow()
        newFileInfo to fileRepository.createNewFile(
            shareId = parentFolder.id.shareId,
            newFileInfo = newFileInfo
        ).getOrThrow()
    }

    suspend operator fun invoke(
        parentFolderId: FolderId,
        name: String,
        mimeType: String
    ) = coRunCatching {
        invoke(
            parentFolder = getLink(parentFolderId).toResult().getOrThrow(),
            name = name,
            mimeType = mimeType,
        ).getOrThrow()
    }
}
