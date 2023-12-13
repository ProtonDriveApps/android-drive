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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.function.pagedList
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

class GetUploadFileLinksPaged @Inject constructor(
    private val linkUploadRepository: LinkUploadRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(userId: UserId): List<UploadFileLink> =
        pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
            linkUploadRepository.getUploadFileLinks(
                userId = userId,
                fromIndex = fromIndex,
                count = count,
            )
        }

    suspend operator fun invoke(userId: UserId, shareId: ShareId): List<UploadFileLink> =
        pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
            linkUploadRepository.getUploadFileLinks(
                userId = userId,
                shareId = shareId,
                fromIndex = fromIndex,
                count = count,
            )
        }

    suspend operator fun invoke(userId: UserId, parentId: FolderId): List<UploadFileLink> =
        pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
            linkUploadRepository.getUploadFileLinks(
                userId = userId,
                parentId = parentId,
                fromIndex = fromIndex,
                count = count,
            )
        }
}
