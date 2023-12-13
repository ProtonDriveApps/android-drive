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

package me.proton.core.drive.upload.domain.usecase

import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import javax.inject.Inject

class RemoveUploadFileAndAnnounceCancelled @Inject constructor(
    private val removeUploadFile: RemoveUploadFile,
    private val announceUploadEvent: AnnounceUploadEvent,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink) = coRunCatching {
        removeUploadFile(uploadFileLink).getOrThrow()
        announceUploadEvent(
            uploadFileLink = uploadFileLink,
            uploadEvent = Event.Upload(
                state = Event.Upload.UploadState.UPLOAD_CANCELLED,
                uploadFileLinkId = uploadFileLink.id,
                percentage = Percentage(0),
                shouldShow = uploadFileLink.shouldAnnounceEvent,
            )
        )
    }
}
