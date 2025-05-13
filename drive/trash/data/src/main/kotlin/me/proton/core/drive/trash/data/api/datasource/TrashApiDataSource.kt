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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.trash.data.api.DriveTrashApi
import me.proton.core.drive.trash.data.api.request.LinkIDsRequest
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException
import javax.inject.Inject

class TrashApiDataSource @Inject constructor(
    private val apiProvider: ApiProvider,
) {

    @Throws(ApiException::class)
    suspend fun sendToTrash(
        userId: UserId,
        volumeId: VolumeId,
        linkIds: List<String>,
    ) = apiProvider.get<DriveTrashApi>(userId).invoke {
        sendToTrash(volumeId.id, LinkIDsRequest(linkIds))
    }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun restoreFromTrash(
        userId: UserId,
        volumeId: VolumeId,
        linkIds: List<String>,
    ) = apiProvider.get<DriveTrashApi>(userId).invoke {
        restoreFromTrash(volumeId.id, LinkIDsRequest(linkIds))
    }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun deleteItemsFromTrash(
        userId: UserId,
        volumeId: VolumeId,
        linkIds: List<String>,
    ) = apiProvider.get<DriveTrashApi>(userId).invoke {
        deleteItemsFromTrash(volumeId.id, LinkIDsRequest(linkIds))
    }.valueOrThrow
}
