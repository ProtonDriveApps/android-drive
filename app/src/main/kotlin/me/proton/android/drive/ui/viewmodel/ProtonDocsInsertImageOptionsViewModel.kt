/*
 * Copyright (c) 2024 Proton AG.
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.android.drive.usecase.GetUriForFile
import me.proton.android.drive.usecase.NotifyActivityNotFound
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.usecase.GetCacheTempFolder
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class ProtonDocsInsertImageOptionsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCacheTempFolder: GetCacheTempFolder,
    private val getUriForFile: GetUriForFile,
    private val notifyActivityNotFound: NotifyActivityNotFound,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val simpleDateFormat: SimpleDateFormat by lazy { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US) }
    private val now: String get() = simpleDateFormat.format(Date())
    private var photoUri: Uri? = savedStateHandle.get<String>(KEY_URI)?.let { uriString -> Uri.parse(uriString) }
    private var dismiss: (() -> Unit)? = null
    private var saveResult: ((List<Uri>) -> Unit)? = null

    fun entries(
        showFilePicker: (() -> Unit) -> Unit,
        takeAPhoto: (Uri, () -> Unit) -> Unit,
        saveResult: (List<Uri>) -> Unit,
        dismiss: () -> Unit,
    ): List<OptionEntry<Unit>> =
        listOf(
            object : OptionEntry<Unit> {
                override val icon: Int = CorePresentation.drawable.ic_proton_image
                override val label: Int = I18N.string.proton_docs_option_select_file
                override val onClick: (Unit) -> Unit = {
                    showFilePicker { handleActivityNotFound(I18N.string.operation_open_document) }
                }
            },
            object : OptionEntry<Unit> {
                override val icon: Int = CorePresentation.drawable.ic_proton_camera
                override val label: Int = I18N.string.proton_docs_option_take_photo
                override val onClick: (Unit) -> Unit = { onTakeAPhoto(takeAPhoto) }
            }
        ).also {
            this.saveResult = saveResult
            this.dismiss = dismiss
        }

    fun onAddFileResult(uriStrings: List<String>, dismiss: () -> Unit) {
        viewModelScope.launch {
            val uris = uriStrings.mapNotNull { uriString -> Uri.parse(uriString) }
            saveResult?.invoke(uris)
            dismiss()
        }
    }

    fun onCameraResult(isTaken: Boolean, dismiss: () -> Unit) {
        viewModelScope.launch {
            if (isTaken) {
                val uri = requireNotNull(photoUri) { "Uri must not be null at this point" }
                saveResult?.invoke(listOf(uri))
            } else {
                withContext(Job() + Dispatchers.IO) {
                    photoUri?.path?.let { path -> File(path).delete() }
                }
            }
            updatePhotoUri(null)
            dismiss()
        }
    }

    private fun onTakeAPhoto(takeAPhoto: (Uri, () -> Unit) -> Unit) {
        viewModelScope.launch(Job() + Dispatchers.IO) {
            val photoFile = File.createTempFile("IMG_${now}_", ".jpg", getCacheTempFolder(userId))
            updatePhotoUri(photoFile)
            getUriForFile(photoFile)
                .onFailure { throwable ->
                    CoreLogger.w(VIEW_MODEL, throwable, "Failed to get Uri for a file")
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
        savedStateHandle[ParentFolderOptionsViewModel.KEY_URI] = photoUri?.toString()
    }

    private fun handleActivityNotFound(@StringRes operation: Int) {
        this.dismiss?.invoke()
        notifyActivityNotFound(userId, operation)
    }

    companion object {
        const val KEY_URI = "key.uri"
    }
}
