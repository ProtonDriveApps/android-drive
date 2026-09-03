/*
 * Copyright (c) 2025 Proton AG.
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
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.documentsprovider.domain.usecase.NotifyDocumentChanged
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import javax.inject.Inject

class UploadDocumentSideEffect @Inject constructor(
    private val getUploadFileLink: GetUploadFileLink,
    private val notifyDocumentChanged: NotifyDocumentChanged,
) {

    suspend operator fun invoke(event: Event.Upload) {
        if (event.state == Event.Upload.UploadState.UPLOAD_COMPLETE) {
            val uploadFileLink = getUploadFileLink(event.uploadFileLinkId).toResult().getOrThrow()
            val documentId = DocumentId(uploadFileLink.userId, uploadId = event.uploadFileLinkId.toString())
            notifyDocumentChanged(documentId)
        }
    }
}
