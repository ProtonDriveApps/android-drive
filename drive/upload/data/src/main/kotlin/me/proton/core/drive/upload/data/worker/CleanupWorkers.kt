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

package me.proton.core.drive.upload.data.worker

import androidx.work.OneTimeWorkRequest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink

interface CleanupWorkers {
    suspend fun additionalCleanupWorkers(
        uploadFileLink: UploadFileLink,
    ): List<OneTimeWorkRequest>

    suspend fun additionalCleanupWorkers(
        uploadFileLinkId: Long,
    ): List<OneTimeWorkRequest>

    suspend operator fun invoke(
        userId: UserId,
        uploadFileLink: UploadFileLink,
        uploadTag: List<String>,
    ): List<OneTimeWorkRequest> =
        additionalCleanupWorkers(uploadFileLink) + UploadSuccessCleanupWorker.getWorkRequest(
            userId = userId,
            uploadFileLinkId = uploadFileLink.id,
            tags = uploadTag
        )

    suspend operator fun invoke(
        userId: UserId,
        uploadFileLinkId: Long,
        uploadTag: List<String>,
    ): List<OneTimeWorkRequest> =
        additionalCleanupWorkers(uploadFileLinkId) + UploadSuccessCleanupWorker.getWorkRequest(
            userId = userId,
            uploadFileLinkId = uploadFileLinkId,
            tags = uploadTag
        )
}
