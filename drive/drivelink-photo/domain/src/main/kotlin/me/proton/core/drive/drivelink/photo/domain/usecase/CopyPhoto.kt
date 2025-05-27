/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.photo.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.onProtonHttpException
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.CreateCopyInfo
import me.proton.core.drive.documentsprovider.domain.usecase.GetContentDigest
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.files.domain.usecase.CopyFile
import me.proton.core.drive.link.domain.entity.CopyInfo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult
import me.proton.core.drive.photo.domain.usecase.GetRelatedPhotoIds
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class CopyPhoto @Inject constructor(
    private val copyFile: CopyFile,
    private val getContentDigest: GetContentDigest,
    private val updateEventAction: UpdateEventAction,
    private val getShare: GetShare,
    private val getPhotoShare: GetPhotoShare,
    private val getRelatedPhotoIds: GetRelatedPhotoIds,
    private val createCopyInfo: CreateCopyInfo,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(
        newParentId: ParentId,
        fileId: FileId,
        shouldUpdateEvent: Boolean = true,
    ) = coRunCatching {
        val userId = fileId.userId
        val fileShare = getShare(fileId.shareId).toResult().getOrThrow()
        val parentShare = getShare(newParentId.shareId).toResult().getOrThrow()
        require(fileShare.volumeId != parentShare.volumeId) {
            "Cannot copy photo to the same volume"
        }
        val photoShare = getPhotoShare(userId).toResult().getOrThrow()
        val relatedPhotoIds = getRelatedPhotoIds(fileShare.volumeId, fileId).getOrThrow()
        optionalUpdateEventAction(userId, photoShare.volumeId, shouldUpdateEvent) {
            copyFile(
                volumeId = fileShare.volumeId,
                fileId = fileId,
                relatedPhotosIds = relatedPhotoIds,
                newVolumeId = parentShare.volumeId,
                newParentId = newParentId,
                contentDigestMap = (listOf(fileId) + relatedPhotoIds).associateWith { linkId ->
                    getContentDigest(linkId).getOrNull()
                },
            ).getOrThrow()
        }
    }

    suspend operator fun invoke(
        newParentId: ParentId,
        fileId: FileId,
        copyInfo: CopyInfo,
        shouldUpdateEvent: Boolean = true,
    ) = coRunCatching {
        val userId = fileId.userId
        val fileShare = getShare(fileId.shareId).toResult().getOrThrow()
        val parentShare = getShare(newParentId.shareId).toResult().getOrThrow()
        require(fileShare.volumeId != parentShare.volumeId) {
            "Cannot copy photo to the same volume"
        }
        val photoShare = getPhotoShare(userId).toResult().getOrThrow()
        optionalUpdateEventAction(userId, photoShare.volumeId, shouldUpdateEvent) {
            copyFile(
                volumeId = fileShare.volumeId,
                fileId = fileId,
                newParentId = newParentId,
                copyInfo = copyInfo,
            ).getOrThrow()
        }
        fileId
    }

    suspend operator fun invoke(
        newParentId: ParentId,
        fileIds: List<FileId>,
        shouldUpdateEvent: Boolean = true,
    ): List<Result<FileId>> = withContext(Dispatchers.IO) {
        coRunCatching {
            fileIds.chunked(configurationProvider.contentDigestsInParallel)
                .flatMap { chunk ->
                    chunk.map { fileId ->
                        async {
                            val fileShare = getShare(fileId.shareId).toResult().getOrThrow()
                            val parentShare = getShare(newParentId.shareId).toResult().getOrThrow()
                            require(fileShare.volumeId != parentShare.volumeId) {
                                "Cannot copy photo to the same volume"
                            }
                            Triple(parentShare.volumeId, fileId, getRelatedPhotoIds(fileShare.volumeId, fileId).getOrThrow())
                        }
                    }.awaitAll()
                }
                .groupBy({it.first}) {
                    it.second to it.third
                }
                .map { (volumeId, relatedPhotoIds) ->
                    createCopyInfo(
                        newVolumeId = volumeId,
                        newParentId = newParentId,
                        fileIds = fileIds,
                        relatedPhotoIds = relatedPhotoIds.toMap(),
                    ).getOrThrow()
                }
                .flatMap { copyInfos ->
                    fileIds.chunked(configurationProvider.contentDigestsInParallel)
                        .flatMap { chunk ->
                            chunk.map { fileId ->
                                async {
                                    invoke(
                                        newParentId = newParentId,
                                        fileId = fileId,
                                        copyInfo = copyInfos[fileId]!!,
                                        shouldUpdateEvent = shouldUpdateEvent,
                                    )
                                }
                            }.awaitAll()
                        }
                }
        }.getOrThrow()
    }

    private suspend fun optionalUpdateEventAction(
        userId: UserId,
        volumeId: VolumeId,
        shouldUpdateEvent: Boolean,
        block: suspend () -> Unit
    ) {
        if (shouldUpdateEvent) {
            updateEventAction(
                userId = userId,
                volumeId = volumeId,
            ) {
                block()
            }
        } else {
            block()
        }
    }
}
