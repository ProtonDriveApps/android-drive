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

package me.proton.android.drive.ui.viewmodel

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.android.drive.ui.options.Option
import me.proton.android.drive.ui.options.filter
import me.proton.android.drive.usecase.GetUriForFile
import me.proton.android.drive.usecase.NotifyActivityNotFound
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.GetCacheTempFolder
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.upload.domain.usecase.UploadFiles
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.upload.domain.exception.NotEnoughSpaceException
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class ParentFolderOptionsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    getDriveLink: GetDecryptedDriveLink,
    private val uploadFiles: UploadFiles,
    private val getCacheTempFolder: GetCacheTempFolder,
    private val getUriForFile: GetUriForFile,
    private val notifyActivityNotFound: NotifyActivityNotFound,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val simpleDateFormat: SimpleDateFormat by lazy { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US) }
    private val now: String get() = simpleDateFormat.format(Date())
    private var photoUri: Uri? = savedStateHandle.get<String>(KEY_URI)?.let { uriString -> Uri.parse(uriString) }
    private val folderId = FolderId(
        shareId = ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        id = savedStateHandle.require(KEY_FOLDER_ID)
    )
    private var dismiss: (() -> Unit)? = null
    val driveLink: StateFlow<DriveLink.Folder?> = getDriveLink(userId, folderId = folderId)
        .mapSuccessValueOrNull()
        .mapWithPrevious { previous, new ->
            if (previous != null && new == null) {
                dismiss?.invoke()
            }
            new
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun entries(
        folder: DriveLink.Folder,
        runAction: RunAction,
        navigateToCreateFolder: (folderId: FolderId) -> Unit,
        showFilePicker: (() -> Unit) -> Unit,
        takeAPhoto: (Uri, () -> Unit) -> Unit,
        dismiss: () -> Unit,
    ): List<FileOptionEntry<DriveLink.Folder>> =
        options
            .filter(folder)
            .map { option ->
                when (option) {
                    is Option.CreateFolder -> option.build(runAction, navigateToCreateFolder)
                    is Option.TakeAPhoto -> option.build { onTakeAPhoto(takeAPhoto) }
                    is Option.UploadFile -> option.build {
                        showFilePicker { handleActivityNotFound(I18N.string.operation_open_document) }
                    }
                    else -> throw IllegalStateException(
                        "Option ${option.javaClass.simpleName} is not found. Did you forget to add it?"
                    )
                }
            }.also {
                this.dismiss = dismiss
            }

    fun onAddFileResult(uriStrings: List<String>, navigateToStorageFull: () -> Unit, dismiss: () -> Unit) {
        viewModelScope.launch {
            driveLink.value?.let { folder ->
                uploadFiles(
                    folder = folder,
                    uploadFileDescriptions = uriStrings.map { uri -> UploadFileDescription(uri) },
                    priority = UploadFileLink.USER_PRIORITY,
                )
                    .onFailure { error ->
                        if (error is NotEnoughSpaceException) {
                            navigateToStorageFull()
                            return@launch
                        }
                    }
            }
            dismiss()
        }
    }

    fun onCameraResult(isTaken: Boolean, navigateToStorageFull: () -> Unit, dismiss: () -> Unit) {
        viewModelScope.launch {
            if (isTaken) {
                val uri = requireNotNull(photoUri) { "Uri must not be null at this point" }
                driveLink.value?.let { folder ->
                    uploadFiles(
                        folder = folder,
                        uploadFileDescriptions = listOf(UploadFileDescription(uri.toString())),
                        shouldDeleteSource = true,
                        priority = UploadFileLink.USER_PRIORITY,
                    )
                        .onFailure { error ->
                            if (error is NotEnoughSpaceException) {
                                navigateToStorageFull()
                                updatePhotoUri(null)
                                return@launch
                            }
                        }
                }
            } else {
                withContext(Job() + Dispatchers.IO) {
                    photoUri?.path?.let { path -> File(path).delete() }
                }
            }
            updatePhotoUri(null)
            dismiss()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun onTakeAPhoto(takeAPhoto: (Uri, () -> Unit) -> Unit) {
        viewModelScope.launch(Job() + Dispatchers.IO) {
            val photoFile = File.createTempFile("IMG_${now}_", ".jpg", getCacheTempFolder(userId))
            updatePhotoUri(photoFile)
            getUriForFile(photoFile)
                .onFailure { throwable ->
                    CoreLogger.d(VIEW_MODEL, throwable, "Failed to get Uri for a file")
                }
                .onSuccess { uri ->
                    viewModelScope.launch {
                        takeAPhoto(uri) { handleActivityNotFound(I18N.string.operation_take_picture) }
                    }
                }
        }
    }

    private fun updatePhotoUri(photoFile: File?) {
        photoUri = photoFile?.let { file -> Uri.fromFile(file) }
        savedStateHandle[KEY_URI] = photoUri?.toString()
    }

    private fun handleActivityNotFound(@StringRes operation: Int) {
        this.dismiss?.invoke()
        notifyActivityNotFound(folderId.userId, operation)
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_FOLDER_ID = "folderId"
        const val KEY_URI = "key.uri"

        private val options = setOf(
            Option.UploadFile,
            Option.TakeAPhoto,
            Option.CreateFolder,
        )
    }
}
