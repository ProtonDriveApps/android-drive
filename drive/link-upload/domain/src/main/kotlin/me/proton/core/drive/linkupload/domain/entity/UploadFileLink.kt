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
package me.proton.core.drive.linkupload.domain.entity

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CameraExifTags
import me.proton.core.drive.base.domain.entity.Location
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import kotlin.time.Duration

data class UploadFileLink(
    val id: Long = 0,
    val userId: UserId,
    val volumeId: VolumeId,
    val shareId: ShareId,
    val parentLinkId: FolderId,
    val linkId: String? = null,
    val draftRevisionId: String = "",
    val name: String,
    val mimeType: String,
    val nodeKey: String = "",
    val nodePassphrase: String = "",
    val nodePassphraseSignature: String = "",
    val contentKeyPacket: String = "",
    val contentKeyPacketSignature: String = "",
    val manifestSignature: String = "",
    val state: UploadState = UploadState.UNPROCESSED,
    val size: Bytes? = null,
    val lastModified: TimestampMs? = null,
    val uriString: String? = null,
    val shouldDeleteSourceUri: Boolean = false,
    val mediaResolution: MediaResolution? = null,
    val digests : UploadDigests = UploadDigests(),
    val networkTypeProviderType: NetworkTypeProviderType,
    val mediaDuration: Duration? = null,
    val fileCreationDateTime: TimestampS? = null,
    val location: Location? = null,
    val cameraExifTags: CameraExifTags? = null,
    val shouldAnnounceEvent: Boolean = true,
    val cacheOption: CacheOption = CacheOption.ALL,
    val priority: Long,
    val uploadCreationDateTime: TimestampS? = null,
    val shouldBroadcastErrorMessage: Boolean = true,
) {
    companion object {
        const val USER_PRIORITY = 1_000L
        const val BACKUP_PRIORITY = 10_000L
        const val RECENT_BACKUP_PRIORITY = BACKUP_PRIORITY - 1_000L
    }
}
