/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.stats

import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.stats.domain.entity.UploadStats
import me.proton.core.drive.stats.domain.usecase.UpdateUploadStats
import javax.inject.Inject

class UploadSideEffect @Inject constructor(
    private val getUploadFileLink: GetUploadFileLink,
    private val updateUploadStats: UpdateUploadStats,
) {

    suspend operator fun invoke(event: Event.Upload) {
        if (event.state in finalState) {
            val uploadFileLink = getUploadFileLink(event.uploadFileLinkId).toResult().getOrThrow()
            val uploadCreationDateTime = requireNotNull(uploadFileLink.uploadCreationDateTime)
            val size = requireNotNull(uploadFileLink.size)
            updateUploadStats(
                UploadStats(
                    folderId = uploadFileLink.parentLinkId,
                    count = 1,
                    size = size,
                    minimumUploadCreationDateTime = uploadCreationDateTime,
                    minimumFileCreationDateTime = uploadFileLink.fileCreationDateTime,
                )
            )
        }
    }

    private companion object {
        val finalState = listOf(
            Event.Upload.UploadState.UPLOAD_COMPLETE,
            Event.Upload.UploadState.UPLOAD_FAILED,
        )
    }
}
