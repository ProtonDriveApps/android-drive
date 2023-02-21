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
package me.proton.core.drive.folder.data.api

import me.proton.core.drive.folder.data.api.request.DeleteFolderChildrenRequest
import me.proton.core.drive.folder.data.extension.toCreateFolderRequest
import me.proton.core.drive.folder.data.extension.toDtoDesc
import me.proton.core.drive.folder.data.extension.toDtoSort
import me.proton.core.drive.folder.domain.entity.FolderInfo
import me.proton.core.drive.folder.domain.entity.Sorting
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.api.response.LinkResponses
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException

class FolderApiDataSource(
    private val apiProvider: ApiProvider,
) {
    @Throws(ApiException::class)
    suspend fun getFolderChildren(
        folderId: FolderId,
        pageIndex: Int,
        pageSize: Int,
        sorting: Sorting,
    ): List<LinkDto> =
        apiProvider.get<FolderApi>(folderId.userId).invoke {
            getFolderChildren(
                folderId.shareId.id,
                folderId.id,
                pageIndex,
                pageSize,
                sorting.by.toDtoSort(),
                sorting.direction.toDtoDesc()
            )
        }.valueOrThrow.linkDtos

    @Throws(ApiException::class)
    suspend fun createFolder(
        shareId: ShareId,
        folderInfo: FolderInfo,
    ) =
        apiProvider.get<FolderApi>(shareId.userId).invoke {
            createFolder(
                shareId = shareId.id,
                request = folderInfo.toCreateFolderRequest()
            )
        }.valueOrThrow.folder.id

    @Throws(ApiException::class)
    suspend fun deleteFolderChildren(
        folderId: FolderId,
        linkIds: List<LinkId>,
    ): LinkResponses =
        apiProvider.get<FolderApi>(folderId.userId).invoke {
            deleteFolderChildren(
                shareId = folderId.shareId.id,
                folderLinkId = folderId.id,
                request = DeleteFolderChildrenRequest(linkIds.map { linkId -> linkId.id }),
            )
        }.valueOrThrow
}
