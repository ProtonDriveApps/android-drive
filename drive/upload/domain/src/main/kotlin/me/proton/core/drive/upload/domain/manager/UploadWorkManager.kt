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
package me.proton.core.drive.upload.domain.manager

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.volume.domain.entity.VolumeId

interface UploadWorkManager {
    suspend fun upload(
        userId: UserId,
        volumeId: VolumeId,
        folderId: FolderId,
        uriStrings: List<String>,
        shouldDeleteSource: Boolean
    )

    suspend fun upload(
        uploadBulk: UploadBulk,
        folder: Folder,
        silently: Boolean = false,
    )

    suspend fun uploadAlreadyCreated(
        userId: UserId,
        uploadFileLinkId: Long,
        uriString: String,
        shouldDeleteSource: Boolean
    )

    suspend fun cancel(uploadFileLink: UploadFileLink)

    suspend fun cancelAll(userId: UserId)

    fun getProgressFlow(uploadFileLink: UploadFileLink): Flow<Percentage>?

    fun broadcastFilesBeingUploaded(folder: Folder, uriStrings: List<String>)

    fun broadcastUploadLimitReached(userId: UserId)
}
