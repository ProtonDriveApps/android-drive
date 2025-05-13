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

import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.folder.domain.usecase.DeleteLocalContent
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.DeleteShare
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetVolume
import javax.inject.Inject

class PhotoShareCleanup @Inject constructor(
    private val getShares: GetShares,
    private val getVolume: GetVolume,
    private val deleteShare: DeleteShare,
    private val deleteLocalContent: DeleteLocalContent,
    private val getLink: GetLink,
) {

    suspend operator fun invoke(userId: UserId) = coRunCatching {
        getShares(
            userId = userId,
            shareType = Share.Type.PHOTO,
            refresh = flowOf(false),
        ).toResult().getOrNull()?.firstOrNull()?.let { photoShare ->
            getVolume(
                userId = userId,
                volumeId = photoShare.volumeId,
                refresh = flowOf(false),
            ).toResult().getOrNull()?.let { volume ->
                if (volume.type == Volume.Type.REGULAR) {
                    deleteStandardShares(userId, photoShare)
                    photoShare.deleteShareAndLocalContent(volume.id)
                }
            }
        }
    }

    private suspend fun Share.deleteShareAndLocalContent(volumeId: VolumeId) {
        deleteLocalContent(
            volumeId = volumeId,
            folderId = rootFolderId,
        )
            .onFailure { error ->
                error.log(
                    tag = LogTag.PHOTO,
                    message = "Failed deleting local content for folder ${rootFolderId.id.logId()}",
                )
            }
        deleteShare(
            shareId = id,
            locallyOnly = true,
        )
            .onFailure { error ->
                error.log(
                    tag = LogTag.PHOTO,
                    message = "Failed deleting share ${id.id.logId()}",
                )
            }
    }

    private suspend fun deleteStandardShares(userId: UserId, photoShare: Share) {
        getShares(
            userId = userId,
            shareType = Share.Type.STANDARD,
            refresh = flowOf(false),
        ).toResult().getOrNull()?.forEach { standardShare ->
            getLink(FileId(photoShare.id, standardShare.rootLinkId)).toResult().getOrNull()?.let {
                deleteShare(
                    shareId = standardShare.id,
                    locallyOnly = true,
                ).onFailure { error ->
                    error.log(
                        tag = LogTag.PHOTO,
                        message = "Failed deleting standard share ${standardShare.id.id.logId()}",
                    )
                }
            }
        }
    }
}
