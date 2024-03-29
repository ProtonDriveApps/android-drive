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

package me.proton.core.drive.linkupload.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

class RemoveAllUploadFileLinks @Inject constructor(
    private val linkUploadRepository: LinkUploadRepository,
) {
    suspend operator fun invoke(userId: UserId, uploadState: UploadState) = coRunCatching {
        linkUploadRepository.removeAllUploadFileLinks(userId, uploadState)
    }

    suspend operator fun invoke(
        userId: UserId,
        shareId: ShareId,
        uploadState: UploadState,
    ) = coRunCatching {
        linkUploadRepository.removeAllUploadFileLinks(userId, shareId, uploadState)
    }

    suspend operator fun invoke(
        userId: UserId,
        folderId: FolderId,
        uploadState: UploadState,
    ) = coRunCatching {
        linkUploadRepository.removeAllUploadFileLinks(userId, folderId, uploadState)
    }

    suspend operator fun invoke(
        folderId: FolderId,
        uriStrings: List<String>,
        uploadState: UploadState,
    ) = coRunCatching {
        linkUploadRepository.removeAllUploadFileLinks(folderId, uriStrings, uploadState)
    }
}
