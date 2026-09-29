/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.backup.data.handler

import kotlinx.coroutines.flow.first
import me.proton.android.drive.verifier.domain.exception.VerifierException
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.drive.backup.data.extension.isMissingMediaLocationPermission
import me.proton.core.drive.backup.data.extension.toBackupError
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.usecase.DeleteFile
import me.proton.core.drive.backup.domain.usecase.HasFolders
import me.proton.core.drive.backup.domain.usecase.MarkAsFailed
import me.proton.core.drive.backup.domain.usecase.StopBackup
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.firstErrorDomainOrNull
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.extension.parentLinkId
import me.proton.core.drive.upload.domain.handler.UploadErrorHandler
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.network.domain.ApiException
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.OperationAbortedException
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.ProtonSdkError
import me.proton.drive.sdk.UploadAbortedException
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

class BackupUploadErrorHandler @Inject constructor(
    private val backupManager: BackupManager,
    private val deleteFile: DeleteFile,
    private val hasFolders: HasFolders,
    private val markAsFailed: MarkAsFailed,
    private val backupPermissionsManager: BackupPermissionsManager,
    private val stopBackup: StopBackup,
) : UploadErrorHandler {
    override suspend fun onError(uploadError: UploadErrorManager.Error) {
        coRunCatching {
            if (uploadError.throwable.hasEffectOnBackup()) {
                if (hasFolders(uploadError.uploadFileLink.parentLinkId).first()) {
                    handleError(uploadError)
                }
            }
        }.onFailure { error ->
            error.log(BACKUP, "Cannot handle error for: ${uploadError.uploadFileLink.id}")
        }
    }

    private suspend fun handleError(
        uploadError: UploadErrorManager.Error,
    ) {
        when (val throwable = uploadError.throwable) {
            is FileNotFoundException -> onFileNotFoundException(uploadError.uploadFileLink)

            else -> {
                val backupError = throwable.toBackupError(uploadError.uploadFileLink.parentLinkId)
                when (backupError.type) {
                    BackupErrorType.PERMISSION -> onFilePermissionError(uploadError, backupError)
                    BackupErrorType.FOLDER_NOT_FOUND -> onFolderNotFoundError(uploadError, backupError)
                    BackupErrorType.LOCAL_STORAGE,
                    BackupErrorType.DRIVE_STORAGE,
                    BackupErrorType.PHOTOS_UPLOAD_NOT_ALLOWED,
                    BackupErrorType.OTHER -> onFileOtherError(uploadError)
                    BackupErrorType.CONNECTIVITY,
                    BackupErrorType.WIFI_CONNECTIVITY,
                    BackupErrorType.BACKGROUND_RESTRICTIONS,
                    -> Unit // Will be stopped by work manager
                }
            }
        }
    }

    private suspend fun onFilePermissionError(
        uploadError: UploadErrorManager.Error,
        backupError: BackupError,
    ) {
        val permissions = backupPermissionsManager.getBackupPermissions(refresh = true)
        // ACCESS_MEDIA_LOCATION is app wide, dropping the item would empty the queue one by one
        val shouldStop = permissions is BackupPermissions.Denied ||
                uploadError.throwable.isMissingMediaLocationPermission
        if (shouldStop) {
            stopBackup(
                folderId = uploadError.uploadFileLink.parentLinkId,
                error = backupError,
            ).onFailure { error ->
                error.log(BACKUP, "Cannot stop backup after losing permissions")
            }
        } else {
            onFileNotFoundException(uploadError.uploadFileLink)
        }
    }

    // The folder is gone, every remaining file would fail the same way
    private suspend fun onFolderNotFoundError(
        uploadError: UploadErrorManager.Error,
        backupError: BackupError,
    ) {
        stopBackup(
            folderId = uploadError.uploadFileLink.parentLinkId,
            error = backupError,
        ).onFailure { error ->
            error.log(BACKUP, "Cannot stop backup after the backup folder was not found")
        }
    }

    private suspend fun onFileNotFoundException(uploadFileLink: UploadFileLink) {
        uploadFileLink.uriString?.let { uriString ->
            CoreLogger.i(BACKUP, "Deleting file not found: $uriString")
            val folderId = uploadFileLink.parentLinkId
            deleteFile(folderId, uriString).onSuccess {
                backupManager.updateNotification(folderId)
            }.onFailure { error ->
                error.log(BACKUP, "Cannot delete file: $uriString")
            }
        }
    }

    private suspend fun onFileOtherError(uploadError: UploadErrorManager.Error) {
        uploadError.uploadFileLink.uriString?.let { uriString ->
            val folderId = uploadError.uploadFileLink.parentLinkId
            markAsFailed(
                folderId = folderId,
                uriString = uriString,
            ).onSuccess {
                backupManager.updateNotification(folderId)
            }.onFailure { error ->
                error.log(BACKUP, "Cannot mark as failed: $uriString")
            }
        }
    }
}

private fun Throwable.hasEffectOnBackup(): Boolean {
    return when (this) {
        is ApiException,
        is IOException,
        is CryptoException,
        is NoSuchElementException,
        is SecurityException,
        is UploadAbortedException,
        is VerifierException,
        -> true

        is UnsupportedOperationException -> isMissingMediaLocationPermission

        is OperationAbortedException -> {
            val errorCause = cause
            if (errorCause is ProtonDriveSdkException) {
                errorCause.hasEffectOnBackup()
            } else {
                true
            }
        }
        is ProtonDriveSdkException -> {
            error?.firstErrorDomainOrNull(ProtonSdkError.ErrorDomain.SuccessfulCancellation) == null
        }

        else -> false
    }
}
