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

package me.proton.core.drive.drivelink.data.extension

import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntityWithBlock
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.data.extension.toLinkWithProperties
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.linkdownload.data.extension.toDownloadState
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.share.user.data.extension.toShareUserMember
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.volume.domain.entity.VolumeId

fun List<DriveLinkEntityWithBlock>.toDriveLinks(): List<DriveLink> =
    groupBy { entity -> entity.linkWithPropertiesEntity.toLinkWithProperties().toLink() }
        .map { (link, entities) ->
            val firstEntity = entities.first()
            link.toEncryptedDriveLink(
                volumeId = VolumeId(firstEntity.volumeId),
                isMarkedAsOffline = firstEntity.linkOfflineEntity != null,
                downloadState = entities.mapNotNull { entity -> entity.downloadStateWithBlock }.toDownloadState(),
                trashState = firstEntity.trashState,
                shareInvitationCount = firstEntity.shareInvitationCount,
                shareMemberCount = firstEntity.shareMemberCount,
                shareUser = firstEntity.shareMemberEntity?.toShareUserMember(),
            )
        }

fun Link.toEncryptedDriveLink(
    volumeId: VolumeId,
    isMarkedAsOffline: Boolean,
    downloadState: DownloadState?,
    trashState: TrashState?,
    shareInvitationCount: Int?,
    shareMemberCount: Int?,
    shareUser: ShareUser?,
) = when (this) {
    is Link.File -> DriveLink.File(
        link = this,
        volumeId = volumeId,
        isMarkedAsOffline = isMarkedAsOffline,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = downloadState,
        trashState = trashState,
        shareInvitationCount = shareInvitationCount,
        shareMemberCount = shareMemberCount,
        shareUser = shareUser,
    )
    is Link.Folder -> DriveLink.Folder(
        link = this,
        volumeId = volumeId,
        isMarkedAsOffline = isMarkedAsOffline,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = downloadState,
        trashState = trashState,
        shareInvitationCount = shareInvitationCount,
        shareMemberCount = shareMemberCount,
        shareUser = shareUser,
    )
}
