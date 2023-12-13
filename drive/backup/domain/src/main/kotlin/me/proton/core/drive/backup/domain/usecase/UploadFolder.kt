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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.availableSpace
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.upload.domain.entity.Notifications
import me.proton.core.drive.drivelink.upload.domain.usecase.UploadFiles
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinksPaged
import me.proton.core.user.domain.usecase.GetUser
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

@Suppress("LongParameterList")
class UploadFolder @Inject constructor(
    private val getFilesToBackup: GetFilesToBackup,
    private val markAllFailedAsReady: MarkAllFailedAsReady,
    private val stopBackup: StopBackup,
    private val configurationProvider: ConfigurationProvider,
    private val uploadFiles: UploadFiles,
    private val cleanUpCompleteBackup: CleanUpCompleteBackup,
    private val getDriveLink: GetDriveLink,
    private val getUploadFileLinks: GetUploadFileLinksPaged,
    private val getUser: GetUser,
    private val markAsEnqueued: MarkAsEnqueued,
) {
    suspend operator fun invoke(
        userId: UserId,
        folderId: FolderId,
        bucketId: Int,
        tags: List<String> = emptyList(),
    ) = coRunCatching {
        val uploading = getUploadFileLinks(userId, folderId).size
        val count = configurationProvider.uploadLimitThreshold - uploading
        val user = getUser(userId, false)
        val availableSpace = user.availableSpace
        CoreLogger.d(BACKUP, "Available space: $availableSpace")

        val retryCount = markAllFailedAsReady(
            userId = userId,
            bucketId = bucketId,
            maxAttempts = configurationProvider.backupMaxAttempts,
        ).getOrThrow()
        if (retryCount > 0) {
            CoreLogger.d(BACKUP, "Retrying $retryCount files")
        }

        val usableSpace = availableSpace - configurationProvider.backupLeftSpace
        val filesToBackup = getFilesToBackup(
            userId = userId,
            bucketId = bucketId,
            maxAttempts = configurationProvider.backupMaxAttempts,
            fromIndex = 0,
            count = count,
        ).getOrThrow()
        val files = filesToBackup.takeToSize(usableSpace)

        if (files.isNotEmpty()) {
            val min = filesToBackup.minBy { file -> file.date }.date
            val max = filesToBackup.maxBy { file -> file.date }.date
            CoreLogger.d(
                BACKUP,
                "Uploading ${files.size}/${filesToBackup.size} files (min:$min, max: $max)"
            )
            if (files.size != filesToBackup.size) {
                val uploadSize = files.sumOf { file -> file.size.value }.bytes
                CoreLogger.d(BACKUP, "Upload size: $uploadSize")
                val excludeFiles = filesToBackup - files.toSet()
                val excludeSize = excludeFiles.sumOf { file -> file.size.value }.bytes
                CoreLogger.d(BACKUP, "Exclude size: $excludeSize")
                CoreLogger.d(BACKUP, "First excluded file size: ${excludeFiles.first().size}")
            }
            val uris =
                files.groupBy({ backupFile -> backupFile.uploadPriority }) { backupFile -> backupFile.uriString }
            val driveLinkFolder: DriveLink.Folder =
                getDriveLink(userId, folderId).mapSuccessValueOrNull().filterNotNull().first()
            uris.forEach { (priority, backupUris) ->
                uploadFiles(
                    folder = driveLinkFolder,
                    uriStrings = backupUris,
                    shouldDeleteSource = false,
                    notifications = Notifications.TurnedOff,
                    cacheOption = CacheOption.NONE,
                    background = true,
                    networkTypeProviderType = NetworkTypeProviderType.BACKUP,
                    shouldBroadcastErrorMessage = false,
                    priority = priority,
                    tags = tags,
                ).getOrThrow()
            }
            markAsEnqueued(userId, files.map { backupFile -> backupFile.uriString }).getOrThrow()
        } else if (filesToBackup.isNotEmpty()) {
            CoreLogger.d(BACKUP, "Cannot continue upload, ${filesToBackup.size} left")
            CoreLogger.d(BACKUP, "First excluded file size: ${filesToBackup.first().size}")
            stopBackup(userId, BackupError.DriveStorage()).getOrThrow()
        } else {
            CoreLogger.d(BACKUP, "Nothing to upload")
            cleanUpCompleteBackup(userId, folderId, bucketId).getOrThrow()
        }
    }
}

private fun List<BackupFile>.takeToSize(availableSpace: Bytes): List<BackupFile> {
    var space = 0.bytes
    return this.filter { backupFile ->
        val size = backupFile.size
        if (space + size <= availableSpace) {
            space += size
            true
        } else {
            false
        }
    }
}
