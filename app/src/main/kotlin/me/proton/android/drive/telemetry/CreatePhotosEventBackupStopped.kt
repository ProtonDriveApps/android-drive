/*
 * Copyright (c) 2023-2024 Proton AG.
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

import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.stats.domain.entity.UploadStats
import me.proton.core.drive.stats.domain.usecase.GetUploadStats
import me.proton.core.drive.stats.domain.usecase.IsInitialBackup
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.drive.telemetry.domain.event.PhotosEvent
import javax.inject.Inject

class CreatePhotosEventBackupStopped(
    private val getUploadStats: GetUploadStats,
    private val isInitialBackup: IsInitialBackup,
    private val clock: () -> TimestampS,
) {
    @Inject
    constructor(
        getUploadStats: GetUploadStats,
        isInitialBackup: IsInitialBackup,
    ) : this(getUploadStats, isInitialBackup, ::TimestampS)

    suspend operator fun invoke(
        folderId: FolderId,
        reason: PhotosEvent.Reason,
    ): DriveTelemetryEvent = when (reason) {
        PhotosEvent.Reason.COMPLETED,
        PhotosEvent.Reason.PAUSED_DISABLED,
        -> getUploadStats(folderId).getOrNull()
            ?.backupStop(reason, folderId)
            ?: backupStopNoBackup(reason, folderId)

        else -> getUploadStats(folderId).getOrThrow().backupStop(reason, folderId)
    }


    private suspend fun UploadStats.backupStop(
        reason: PhotosEvent.Reason,
        folderId: FolderId,
    ) = PhotosEvent.BackupStopped(
        duration = clock().value - minimumUploadCreationDateTime.value,
        files = count,
        size = size.value,
        reason = reason,
        isInitialBackup = isInitialBackup(folderId).getOrThrow()
    )

    private suspend fun backupStopNoBackup(
        reason: PhotosEvent.Reason,
        folderId: FolderId,
    ) = PhotosEvent.BackupStopped(
        duration = 0,
        files = 0,
        size = 0,
        reason = reason,
        isInitialBackup = isInitialBackup(folderId).getOrThrow()
    )
}
