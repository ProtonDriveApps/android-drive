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

package me.proton.core.drive.trash.data.api.datasource

import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.trash.data.api.DriveTrashApi
import me.proton.core.drive.trash.data.api.request.LinkIDsRequest
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException
import javax.inject.Inject

class TrashApiDataSource @Inject constructor(
    private val apiProvider: ApiProvider,
) {

    @Throws(ApiException::class)
    suspend fun sendToTrash(
        parentId: ParentId,
        linkIds: List<String>,
    ) = apiProvider.get<DriveTrashApi>(parentId.userId).invoke {
        sendToTrash(parentId.shareId.id, parentId.id, LinkIDsRequest(linkIds))
    }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun restoreFromTrash(shareId: ShareId, linkIds: List<String>) =
        apiProvider.get<DriveTrashApi>(shareId.userId).invoke {
            restoreFromTrash(shareId.id, LinkIDsRequest(linkIds))
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun deleteItemsFromTrash(shareId: ShareId, linkIds: List<String>) =
        apiProvider.get<DriveTrashApi>(shareId.userId).invoke {
            deleteItemsFromTrash(shareId.id, LinkIDsRequest(linkIds))
        }.valueOrThrow
}
