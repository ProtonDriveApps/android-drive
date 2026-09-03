/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.upload.data.worker

import androidx.work.NetworkType
import androidx.work.Operation
import androidx.work.WorkManager
import me.proton.core.domain.entity.UserId

internal sealed class FileUploadFlow {

    abstract suspend fun enqueueWork(uploadTags: List<String>, uriString: String): Operation
    abstract val uploadFileLinkId: Long

    class Sdk(
        private val workManager: WorkManager,
        private val userId: UserId,
        override val uploadFileLinkId: Long,
        private val networkType: NetworkType,
        private val cleanupWorkers: CleanupWorkers,
    ) : FileUploadFlow() {

        override suspend fun enqueueWork(uploadTags: List<String>, uriString: String) = workManager
            .beginWith(
                UpdateFileAttributesWorker.getWorkRequest(
                    userId = userId,
                    uploadFileLinkId = uploadFileLinkId,
                    tags = uploadTags,
                )
            )
            .then(
                ExtractTagsWorker.getWorkRequest(
                    userId = userId,
                    uploadFileLinkId = uploadFileLinkId,
                    tags = uploadTags,
                )
            )
            .then(
                CreateNewFileSdkWorker.getWorkRequest(
                    userId = userId,
                    uploadFileLinkId = uploadFileLinkId,
                    uriString = uriString,
                    networkType = networkType,
                    tags = uploadTags,
                )
            ).then(
                UploadFileSdkWorker.getWorkRequest(
                    userId = userId,
                    uploadFileLinkId = uploadFileLinkId,
                    uriString = uriString,
                    networkType = networkType,
                    tags = uploadTags,
                )
            ).then(
                cleanupWorkers(userId, uploadFileLinkId, uploadTags)
            ).enqueue()
    }

    class RecreateFileSdk(
        private val workManager: WorkManager,
        private val userId: UserId,
        override val uploadFileLinkId: Long,
        private val networkType: NetworkType,
        private val cleanupWorkers: CleanupWorkers,
    ) : FileUploadFlow() {

        override suspend fun enqueueWork(uploadTags: List<String>, uriString: String) = workManager
            .beginWith(
                CreateNewFileSdkWorker.getWorkRequest(
                    userId = userId,
                    uploadFileLinkId = uploadFileLinkId,
                    uriString = uriString,
                    networkType = networkType,
                    tags = uploadTags,
                )
            ).then(
                UploadFileSdkWorker.getWorkRequest(
                    userId = userId,
                    uploadFileLinkId = uploadFileLinkId,
                    uriString = uriString,
                    networkType = networkType,
                    tags = uploadTags,
                )
            ).then(
                cleanupWorkers(userId, uploadFileLinkId, uploadTags)
            ).enqueue()
    }
}
