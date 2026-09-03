/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.trash.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.provider.ProtonSdkClientProvider
import me.proton.core.drive.trash.domain.repository.DriveTrashRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.drive.sdk.entity.NodeResultPair
import me.proton.drive.sdk.entity.NodeUid
import javax.inject.Inject

class DriveTrashRepositorySdk @Inject constructor(
    private val protonSdkClientProvider: ProtonSdkClientProvider,
) : DriveTrashRepository {
    override suspend fun fetchTrashContent(
        userId: UserId,
        volumeId: VolumeId,
        pageIndex: Int,
        pageSize: Int
    ): Result<Pair<List<Link>, SaveAction>> {
        TODO("fetchTrashContent not implemented in sdk")
    }

    override suspend fun sendToTrash(
        userId: UserId,
        volumeId: VolumeId,
        links: List<LinkId>,
    ): Map<LinkId, DataResult<Unit>> {
        val nodeUids = links.associateBy { it.nodeUid(volumeId) }
        return protonSdkClientProvider.getOrCreate(userId, volumeId).getOrThrow()
            .trashNodes(nodeUids.keys.toList())
            .toDataResultMap(nodeUids)
    }

    override suspend fun restoreFromTrash(
        userId: UserId,
        volumeId: VolumeId,
        links: List<LinkId>,
    ): Map<LinkId, DataResult<Unit>> {
        val nodeUids = links.associateBy { it.nodeUid(volumeId) }
        return protonSdkClientProvider.getOrCreate(userId, volumeId).getOrThrow()
            .restoreNodes(nodeUids.keys.toList())
            .toDataResultMap(nodeUids)
    }

    override suspend fun emptyTrash(userId: UserId, volumeId: VolumeId) {
        return protonSdkClientProvider.getOrCreate(userId, volumeId).getOrThrow().emptyTrash()
    }

    override suspend fun deleteItemsFromTrash(
        userId: UserId,
        volumeId: VolumeId,
        links: List<LinkId>,
    ): Map<LinkId, DataResult<Unit>> {
        val nodeUids = links.associateBy { it.nodeUid(volumeId) }
        return protonSdkClientProvider.getOrCreate(userId, volumeId).getOrThrow()
            .deleteNodes(nodeUids.keys.toList())
            .toDataResultMap(nodeUids)
    }

    private suspend fun Flow<NodeResultPair>.toDataResultMap(
        nodeUids: Map<NodeUid, LinkId>,
    ): Map<LinkId, DataResult<Unit>> = toList()
        .mapNotNull { pair ->
            nodeUids[pair.nodeUid]?.let { linkId ->
                linkId to when (pair) {
                    is NodeResultPair.Success ->
                        DataResult.Success(ResponseSource.Remote, Unit)

                    is NodeResultPair.Failure ->
                        DataResult.Error.Remote(pair.error.message, pair.error)
                }
            }
        }.toMap()
}
