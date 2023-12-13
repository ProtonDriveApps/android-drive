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
package me.proton.core.drive.linktrash.data.test.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.test.NullableFile
import me.proton.core.drive.link.data.test.NullableFolder
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class StubbedLinkTrashRepository @Inject constructor() : LinkTrashRepository {

    private val workIds = MutableStateFlow(emptyMap<String, List<LinkId>>())
    private val stateFlow = MutableStateFlow(emptyMap<List<LinkId>, TrashState>())
    private var trashContent = emptyList<VolumeId>()

    val state: Map<List<LinkId>, TrashState>
        get() = stateFlow.value

    override suspend fun insertOrUpdateTrashState(linkIds: List<LinkId>, trashState: TrashState) {
        stateFlow.value = stateFlow.value + (linkIds to trashState)
    }

    override suspend fun removeTrashState(linkIds: List<LinkId>) {
        stateFlow.value = stateFlow.value.filterKeys { it != linkIds }
    }

    override suspend fun markTrashedLinkAsDeleted(shareId: ShareId) {
        stateFlow.value = stateFlow.value.map { (linkIds, trashState) ->
            if (linkIds.any { linkId -> linkId.shareId == shareId }) {
                linkIds to TrashState.DELETED
            } else {
                linkIds to trashState
            }
        }.toMap()
    }

    override fun hasTrashContent(userId: UserId, volumeId: VolumeId): Flow<Boolean> {
        return stateFlow.transform { state ->
            emit(
                state.filterKeys { it.any { linkId -> linkId.userId == userId } }
                    .filterValues { it in listOf(TrashState.TRASHING, TrashState.DELETED) }
                    .isNotEmpty()
            )
        }
    }

    override suspend fun hasWorkWithId(workId: String): Boolean {
        return workIds.value.containsKey(workId)
    }

    override suspend fun insertOrIgnoreWorkId(linkIds: List<LinkId>, workId: String) {
        workIds.value = workIds.value + (workId to linkIds)
    }

    override suspend fun insertWork(linkIds: List<LinkId>, retries: Int): DataResult<String> {
        val workId = "work-id-${linkIds.first().shareId.id}"
        workIds.value = workIds.value + (workId to linkIds)
        return DataResult.Success(ResponseSource.Local, workId)
    }

    override suspend fun getLinksAndRemoveWorkFromCache(workId: String): List<Link> {
        val work = workIds.value.filterKeys { id -> id == workId }
        workIds.value = workIds.value.filterNot { (id, _) -> id == workId }
        return work.values.map { linkIds ->
            linkIds.map { linkId ->
                val parentId = FolderId(linkId.shareId, "folder-id")
                when (linkId) {
                    is FolderId -> NullableFolder(id = linkId, parentId = parentId)
                    is FileId -> NullableFile(id = linkId, parentId = parentId)
                } as Link
            }
        }.flatten()
    }

    override suspend fun markTrashContentAsFetched(userId: UserId, volumeId: VolumeId) {
        trashContent = trashContent + volumeId
    }

    override suspend fun shouldInitiallyFetchTrashContent(userId: UserId, volumeId: VolumeId): Boolean {
        return trashContent.contains(volumeId).not()
    }

    override suspend fun isTrashed(linkId: LinkId): Boolean {
        return stateFlow.value
            .filterKeys { linkIds -> linkId in linkIds }
            .filterValues { trashState -> trashState.isTrashed() }
            .isNotEmpty()
    }

    override suspend fun isAnyTrashed(linkIds: Set<LinkId>): Boolean {
        return linkIds.fold(false) { acc, linkId ->
            acc || isTrashed(linkId)
        }
    }

    private fun TrashState.isTrashed(): Boolean {
        return this !in listOf(TrashState.TRASHING, TrashState.DELETED)
    }
}

val LinkTrashRepository.state
    get() = (this as StubbedLinkTrashRepository).state

fun LinkTrashRepository.stateForLinks(vararg links: BaseLink): TrashState =
    (this as StubbedLinkTrashRepository).state.getValue(links.map { it.id })