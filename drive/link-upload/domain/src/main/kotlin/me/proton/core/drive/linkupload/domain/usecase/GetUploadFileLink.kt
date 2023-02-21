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
package me.proton.core.drive.linkupload.domain.usecase

import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.extension.toDataResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import javax.inject.Inject

class GetUploadFileLink @Inject constructor(
    private val linkUploadRepository: LinkUploadRepository,
) {
    suspend operator fun invoke(uploadFileLinkId: Long): DataResult<UploadFileLink> = coRunCatching {
        linkUploadRepository.getUploadFileLink(uploadFileLinkId)
            ?: throw NoSuchElementException("UploadFileLink with id $uploadFileLinkId is not found")
    }.toDataResult()

    suspend operator fun invoke(fileId: FileId): DataResult<UploadFileLink> = coRunCatching {
        linkUploadRepository.getUploadFileLink(fileId)
            ?: throw NoSuchElementException("UploadFileLink with file id $fileId is not found")
    }.toDataResult()
}
