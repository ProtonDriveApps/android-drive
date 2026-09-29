/*
 * Copyright (c) 2024-2026 Proton AG.
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

import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.log.LogTag.UPLOAD
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetInternalStorageInfo
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.extension.sizeOrZero
import me.proton.core.drive.linkupload.domain.extension.volumeId
import me.proton.core.drive.linkupload.domain.usecase.GetPendingUploadFileLinksSize
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinksCount
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinksWithUriByPriority
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class GetNextUploadFileLinks @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val getUploadFileLinksCount: GetUploadFileLinksCount,
    private val getPendingUploadFileLinksSize: GetPendingUploadFileLinksSize,
    private val getUploadFileLinksWithUriByPriority: GetUploadFileLinksWithUriByPriority,
    private val getInternalStorageInfo: GetInternalStorageInfo,
) {
    suspend operator fun invoke(
        userId: UserId,
    ): Result<List<UploadFileLink>> = coRunCatching {
        val photo = invoke(userId, Volume.Type.PHOTO)
        val drive = invoke(userId, Volume.Type.REGULAR)
        val total = photo.size + drive.size
        if (total == 0) {
            CoreLogger.d(UPLOAD, "Nothing to upload")
        } else {
            CoreLogger.d(UPLOAD, "Uploading $total files: photo=${photo.size}, drive=${drive.size}")
        }
        photo + drive
    }

    private suspend fun invoke(userId: UserId, volumeType: Volume.Type): List<UploadFileLink> {
        val uploadCount = getUploadFileLinksCount(userId, volumeType).first()
        if (uploadCount.total == 0) {
            return emptyList()
        }
        val running = uploadCount.totalWithUri - uploadCount.totalUnprocessedWithUri
        val runningNonUserUploads =
            uploadCount.totalWithUriNonUserPriority - uploadCount.totalUnprocessedWithUriNonUserPriority
        val availableNonUserUploadSlots =
            configurationProvider.nonUserUploadsInParallel - runningNonUserUploads
        val availableInternalStorage =
            getInternalStorageInfo().getOrThrow().available - getPendingUploadFileLinksSize(userId)

        val uploadFileLinks = getUploadFileLinksWithUriByPriority(
            userId = userId,
            volumeType = volumeType,
            states = setOf(UploadState.UNPROCESSED, UploadState.CREATING_NEW_FILE),
            count = uploadCount.totalWithUri,
        ).first()

        if (uploadFileLinks.isEmpty()) {
            CoreLogger.d(UPLOAD, "Nothing to upload for volumeType=$volumeType (${uploadCount.totalWithUri})")
            return emptyList()
        }

        val selected = uploadFileLinks
            .groupBy { it.volumeId }
            .values
            .flatMap { volumeLinks -> volumeLinks.selectNextForVolume(running) }
            .sortedBy { it.priority }

        if (selected.isEmpty()) {
            return emptyList()
        }

        return selected
            .takeNonUserPriorityLimited(availableNonUserUploadSlots)
            .takeOnlyInSizeOrFirst(availableInternalStorage, running)
    }

    private fun List<UploadFileLink>.selectNextForVolume(running: Int): List<UploadFileLink> {
        val availableSlots = configurationProvider.uploadsInParallelPerVolume - running

        if (any { it.state == UploadState.CREATING_NEW_FILE } || availableSlots <= 0) {
            return emptyList()
        }

        val unprocessed = filter { it.state == UploadState.UNPROCESSED }
        return unprocessed.take(minOf(2, availableSlots))
    }

    private fun List<UploadFileLink>.takeOnlyInSizeOrFirst(
        availableInternalStorage: Bytes,
        running: Int,
    ): List<UploadFileLink> {
        val underInternalStorageLinks = filter { uploadFileLink ->
            uploadFileLink.sizeOrZero < availableInternalStorage
        }
        return if (underInternalStorageLinks.isEmpty() && running == 0) {
            val uploadFileLink = first()
            CoreLogger.w(
                UPLOAD,
                "Next to upload will be too large: ${uploadFileLink.size} > $availableInternalStorage"
            )
            listOf(uploadFileLink)
        } else {
            underInternalStorageLinks.takeOnlyInSize(availableInternalStorage)
        }
    }

    private fun List<UploadFileLink>.takeNonUserPriorityLimited(count: Int): List<UploadFileLink> {
        val (users, others) = partition { uploadFileLink ->
            uploadFileLink.priority <= UploadFileLink.USER_PRIORITY
        }
        return listOf(users, others.take(count)).flatten()
    }

    private fun List<UploadFileLink>.takeOnlyInSize(size: Bytes): List<UploadFileLink> {
        var count = 0.bytes
        var index = 0

        for (uploadFileLink in this) {
            count += uploadFileLink.sizeOrZero
            if (count >= size) {
                break
            }
            index++
        }

        return subList(0, index)
    }

}
