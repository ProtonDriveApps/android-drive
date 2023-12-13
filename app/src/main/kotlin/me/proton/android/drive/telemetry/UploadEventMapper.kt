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

package me.proton.android.drive.telemetry

import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.entity.Event.Upload.Reason
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.telemetry.domain.event.PhotosEvent
import javax.inject.Inject

class UploadEventMapper(
    private val getUploadFileLink: GetUploadFileLink,
    private val getShare: GetShare,
    private val clock: () -> TimestampS,
) {
    @Inject
    constructor(
        getUploadFileLink: GetUploadFileLink,
        getShare: GetShare,
    ) : this(getUploadFileLink, getShare, clock = { TimestampS() })

    suspend operator fun invoke(event: Event.Upload) =
        if (event.state in finalState) {
            getUploadFileLink(event.uploadFileLinkId).toResult().getOrThrow()
                .takeIf { uploadFileLink -> uploadFileLink.isPhoto() }
                ?.let { uploadFileLink ->
                    val uploadCreationDateTime =
                        requireNotNull(uploadFileLink.uploadCreationDateTime)
                    val size = requireNotNull(uploadFileLink.size)
                    PhotosEvent.UploadDone(
                        duration = clock().value - uploadCreationDateTime.value,
                        sizeKB = size.toKiB(),
                        reason = when (event.reason) {
                            null -> PhotosEvent.Reason.COMPLETED
                            Reason.ERROR_OTHER -> PhotosEvent.Reason.FAILED_OTHER
                            Reason.ERROR_PERMISSIONS -> PhotosEvent.Reason.FAILED_PERMISSIONS
                            Reason.ERROR_DRIVE_STORAGE -> PhotosEvent.Reason.FAILED_DRIVE_STORAGE
                            Reason.ERROR_LOCAL_STORAGE -> PhotosEvent.Reason.FAILED_LOCAL_STORAGE
                            Reason.ERROR_NOT_ALLOWED -> PhotosEvent.Reason.FAILED_NOT_ALLOWED
                        }
                    )
                }
        } else {
            null
        }

    @Suppress("MagicNumber")
    private fun Bytes.toKiB() = value / 1024

    private suspend fun UploadFileLink.isPhoto() = getShare(shareId, flowOf(false))
        .filterSuccessOrError()
        .toResult()
        .getOrThrow()
        .let { share ->
            share.type == Share.Type.PHOTO
        }


    companion object {
        val finalState = listOf(
            Event.Upload.UploadState.UPLOAD_COMPLETE,
            Event.Upload.UploadState.UPLOAD_FAILED,
        )
    }
}
