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

package me.proton.core.drive.drivelink.domain.extension

import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.file.base.domain.entity.ThumbnailId
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.file.base.domain.extension.getThumbnailId
import me.proton.core.drive.file.base.domain.extension.getThumbnailIds
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.hasShareLink
import me.proton.core.drive.link.domain.extension.isPhoto
import me.proton.core.drive.link.domain.extension.isSharedByLinkOrWithUsers
import me.proton.core.drive.photo.domain.entity.PhotoListing

fun DriveLink.updateLastModified(lastModified: TimestampS) = link.let { link ->
    when (link) {
        is Link.File -> link.copy(lastModified = lastModified)
        is Link.Folder -> link.copy(lastModified = lastModified)
        is Link.Album -> link.copy(lastModified = lastModified)
    }
}

val DriveLink?.isNameEncrypted: Boolean get() = this?.let { cryptoName is CryptoProperty.Encrypted } ?: false

val DriveLink.File.thumbnailIds: Set<ThumbnailId> get() = link.getThumbnailIds(volumeId)

fun DriveLink.File.getThumbnailId(type: ThumbnailType): ThumbnailId? = link.getThumbnailId(volumeId, type)

val DriveLink.File.isPhoto: Boolean get() = link.isPhoto

val DriveLink.isEditor: Boolean get() = link.permissions.has(Permissions.Permission.WRITE)

val DriveLink.hasShareLink: Boolean
    get() = link.hasShareLink

val DriveLink.isSharedByLinkOrWithUsers: Boolean
    get() = link.isSharedByLinkOrWithUsers

val DriveLink.isSharedWithUsers: Boolean
    get() = (shareInvitationCount ?: 0) > 0 || (shareMemberCount ?: 0) > 0

val DriveLink.isShareMember: Boolean
    get() = shareUser?.permissions?.isAdmin == false

val DriveLink.isShareReadOnly: Boolean get() = sharePermissions?.let { sharePermissions ->
    sharePermissions.canRead && !sharePermissions.canWrite
} ?: false

fun DriveLink.File.toVolumePhotoListing(): PhotoListing.Volume =
    requireNotNull(photoCaptureTime) { "Photo drive link is required" }
        .let { captureTime ->
            PhotoListing.Volume(
                linkId = id,
                captureTime = captureTime,
                nameHash = nameHash,
                contentHash = link.photoContentHash,
            )
        }

val List<DriveLink>.lowestCommonPermissions: Permissions get() =
    minOfOrNull { driveLink -> driveLink.sharePermissions?.value ?: Permissions.owner.value }
        ?.let { value ->
            Permissions(value)
        } ?: Permissions.owner
