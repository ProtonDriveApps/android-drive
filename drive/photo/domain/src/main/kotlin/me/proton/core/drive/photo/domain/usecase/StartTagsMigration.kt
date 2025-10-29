/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.photo.domain.usecase

import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.drivePhotosTagsMigration
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.drivePhotosTagsMigrationDisabled
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.TagsMigrationAnchor
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatus
import me.proton.core.drive.photo.domain.manager.PhotoTagWorkManager
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetVolume
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class StartTagsMigration @Inject constructor(
    private val getFeatureFlag: GetFeatureFlag,
    private val fetchAllPhotoListings: FetchAllPhotoListings,
    private val configurationProvider: ConfigurationProvider,
    private val insertTagsMigrationFiles: InsertTagsMigrationFiles,
    private val getTagsMigrationStatus: GetTagsMigrationStatus,
    private val getTagsMigrationStatistics: GetTagsMigrationStatistics,
    private val getOrCreateClientUid: GetOrCreateClientUid,
    private val updateTagsMigrationStatus: UpdateTagsMigrationStatus,
    private val workManager: PhotoTagWorkManager,
    private val getVolume: GetVolume,
) {
    suspend operator fun invoke(userId: UserId, volumeId: VolumeId) = coRunCatching {
        if (getFeatureFlag(drivePhotosTagsMigrationDisabled(userId)).on) {
            CoreLogger.i(PHOTO, "Kill switch for migration enabled, aborting")
            return@coRunCatching
        }
        if (getFeatureFlag(drivePhotosTagsMigration(userId)).off) {
            CoreLogger.i(PHOTO, "Feature flag for migration not enabled, aborting")
            return@coRunCatching
        }
        val status = getTagsMigrationStatus(userId, volumeId).getOrThrow()
        if (status.finished) {
            CoreLogger.i(PHOTO, "Migration already finished for volume: ${volumeId.id.logId()}")
            return@coRunCatching
        }
        val statusAnchor = status.anchor
        val migrationClientUid = statusAnchor?.clientUid
        val applicationClientUid = getOrCreateClientUid().getOrThrow()
        if (migrationClientUid != null && migrationClientUid != applicationClientUid) {
            CoreLogger.i(
                PHOTO,
                "Migration started by another client: $migrationClientUid for volume: ${volumeId.id.logId()}"
            )
            return@coRunCatching
        }

        val photoListings = fetchAllPhotoListings(
            userId = userId,
            volumeId = volumeId,
            pageSize = configurationProvider.apiListingPageSize,
            linkId = statusAnchor?.lastProcessedLinkId
        ).getOrThrow().let { photoListings ->
            val lastProcessedCaptureTime = statusAnchor?.lastProcessedCaptureTime
            if (lastProcessedCaptureTime != null) {
                photoListings.filter { photoListing ->
                    photoListing.captureTime < lastProcessedCaptureTime
                }
            } else {
                photoListings
            }
        }
        if (photoListings.isEmpty()) {
            CoreLogger.i(PHOTO, "No files to migrate for volume: ${volumeId.id.logId()}")
            val statistics = getTagsMigrationStatistics(userId, volumeId).first()
            if (statistics.data.isEmpty()) {
                CoreLogger.i(
                    PHOTO,
                    "Updating migration as finished for volume: ${volumeId.id.logId()}"
                )

                updateTagsMigrationStatus(
                    userId, volumeId, TagsMigrationStatus(
                        finished = true,
                        anchor = statusAnchor.createAnchor(userId, volumeId, applicationClientUid)
                    )
                ).getOrThrow()
            } else {
                if (!statistics.isFinished && statistics.progress != null) {
                    CoreLogger.i(
                        PHOTO,
                        "Restarting migration for the remaining local files $statistics"
                    )
                    workManager.enqueue(userId, volumeId)
                }
            }
            return@coRunCatching
        }

        CoreLogger.i(PHOTO, "Starting migration for volume: ${volumeId.id.logId()}")
        insertTagsMigrationFiles(photoListings.map { photoListing ->
            TagsMigrationFile(
                volumeId = volumeId,
                fileId = photoListing.linkId,
                captureTime = photoListing.captureTime,
            )
        }).getOrThrow()
        workManager.enqueue(userId, volumeId)
    }

    private suspend fun TagsMigrationAnchor?.createAnchor(
        userId: UserId,
        volumeId: VolumeId,
        applicationClientUid: ClientUid,
        currentTimestamp:TimestampS = TimestampS(),
    ): TagsMigrationAnchor {
        return if (this == null) {
            val volume = getVolume(userId, volumeId).toResult().getOrThrow()
            TagsMigrationAnchor(
                lastProcessedLinkId = FileId(ShareId(userId, volume.shareId), volume.linkId),
                lastProcessedCaptureTime = volume.createTime,
                currentTimestamp = currentTimestamp,
                clientUid = applicationClientUid,
            )
        } else {
            copy(
                currentTimestamp = currentTimestamp,
                clientUid = applicationClientUid,
            )
        }
    }
}
