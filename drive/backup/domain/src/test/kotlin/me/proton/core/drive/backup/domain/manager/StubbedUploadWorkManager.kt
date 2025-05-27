/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.backup.domain.manager

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubbedUploadWorkManager @Inject constructor() : UploadWorkManager {

    val bulks = mutableMapOf<FolderId, UploadBulk>()
    override suspend fun upload(
        userId: UserId,
        volumeId: VolumeId,
        folderId: FolderId,
        uriStrings: List<String>,
        cacheOption: CacheOption,
        shouldDeleteSource: Boolean,
        networkTypeProviderType: NetworkTypeProviderType,
        shouldAnnounceEvent: Boolean,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean
    ): List<UploadFileLink> {
        TODO("Not yet implemented")
    }

    override suspend fun upload(
        uploadBulk: UploadBulk,
        folder: Folder,
        showPreparingUpload: Boolean,
        showFilesBeingUploaded: Boolean,
    ) {
        bulks[folder.id] = uploadBulk
    }

    override suspend fun uploadAlreadyCreated(
        userId: UserId,
        uploadFileLinkId: Long,
        uriString: String,
        shouldDeleteSource: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun cancel(uploadFileLink: UploadFileLink) {
        TODO("Not yet implemented")
    }

    override suspend fun cancelAll(userId: UserId) {
        TODO("Not yet implemented")
    }

    override suspend fun cancelAllByShare(userId: UserId, shareId: ShareId) {
        TODO("Not yet implemented")
    }

    override suspend fun cancelAllByFolder(userId: UserId, folderId: FolderId) {
        TODO("Not yet implemented")
    }

    override suspend fun cancelAllByFolderAndUris(folderId: FolderId, uriStrings: List<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun waitUploadEventWorkerToFinish(userId: UserId) {
        TODO("Not yet implemented")
    }

    override fun getProgressFlow(uploadFileLink: UploadFileLink): Flow<Percentage>? {
        TODO("Not yet implemented")
    }

    override fun broadcastFilesBeingUploaded(folder: Folder, uriStrings: List<String>) {
        TODO("Not yet implemented")
    }

    override fun broadcastUploadLimitReached(userId: UserId) {
        TODO("Not yet implemented")
    }

    override fun broadcastFilesBeingUploaded(
        folder: Folder,
        uriStrings: List<String>,
        uploadFileLinks: List<UploadFileLink>
    ) {
        TODO("Not yet implemented")
    }
}
