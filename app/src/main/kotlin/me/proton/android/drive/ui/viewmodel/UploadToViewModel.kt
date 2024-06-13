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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.CombinedLoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.navigation.UploadParameters
import me.proton.android.drive.ui.viewevent.UploadToViewEvent
import me.proton.android.drive.ui.viewstate.UploadToViewState
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.list.domain.usecase.GetPagedDriveLinksList
import me.proton.core.drive.drivelink.upload.domain.entity.Notifications
import me.proton.core.drive.drivelink.upload.domain.usecase.UploadFiles
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.upload.domain.exception.NotEnoughSpaceException
import me.proton.core.drive.upload.domain.provider.FileProvider
import me.proton.core.drive.upload.domain.usecase.CopyUriToCacheTempFolder
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@ExperimentalCoroutinesApi
@HiltViewModel
class UploadToViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val copyUriToCacheTempFolder: CopyUriToCacheTempFolder,
    private val uploadFiles: UploadFiles,
    private val fileProvider: FileProvider,
    getDriveLink: GetDecryptedDriveLink,
    getPagedDriveLinks: GetPagedDriveLinksList,
    configurationProvider: ConfigurationProvider,
) : HostFilesViewModel(appContext, getDriveLink, getPagedDriveLinks, savedStateHandle, configurationProvider) {
    private val parentShareId: ShareId? = savedStateHandle.get<String?>(PARENT_SHARE_ID)?.let { id ->
        ShareId(userId, id)
    }
    override val parentId = savedStateHandle.get<String>(PARENT_ID)?.let { parentId ->
        parentShareId?.let {
            FolderId(parentShareId, parentId).also { folderId ->
                trigger.tryEmit(folderId)
            }
        }
    }
    private val uploadParameters = savedStateHandle.get<UploadParameters>(URIS)

    val initialViewState = UploadToViewState(
        filesViewState = initialFilesViewState,
        title = "",
        navigationIconResId = CorePresentation.drawable.ic_proton_cross,
        fileNames = uploadParameters?.uris?.map { uri -> uri.fileName } ?: emptyList(),
    )
    val driveLink: StateFlow<DriveLink.Folder?> = getDriveLink(userId, folderId = null)
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val viewState: Flow<UploadToViewState> = combine(
        parentLink,
        listContentState,
        listContentAppendingState
    ) { parentLink, contentState, appendingState ->
        val isRoot = parentLink != null && parentLink.parentId == null
        initialViewState.copy(
            filesViewState = initialViewState.filesViewState.copy(
                listContentState = contentState,
                listContentAppendingState = appendingState
            ),
            isBackHandlerEnabled = !(parentLink == null || isRoot),
            title = parentLink?.name.orEmpty(),
            isTitleEncrypted = isRoot.not() && parentLink.isNameEncrypted,
            navigationIconResId = if (parentLink == null || isRoot) {
                CorePresentation.drawable.ic_proton_cross
            } else {
                CorePresentation.drawable.ic_arrow_back
            },
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    fun viewEvent(
        navigateToStorageFull: () -> Unit,
        navigateToCreateFolder: (parentId: FolderId) -> Unit,
        exitApp: () -> Unit,
    ): UploadToViewEvent = object : UploadToViewEvent {
        override val onDriveLink: ((DriveLink) -> Unit) = { driveLink ->
            if (driveLink is DriveLink.Folder) {
                viewModelScope.launch {
                    trigger.emit(driveLink.id)
                }
            }
        }
        override val onLoadState: (CombinedLoadStates, Int) -> Unit = this@UploadToViewModel.onLoadState
        override val onSorting: (Sorting) -> Unit = this@UploadToViewModel.onSorting
        override val onTopAppBarNavigation: () -> Unit = this@UploadToViewModel.onTopAppBarNavigation(exitApp)
        override val onErrorAction: () -> Unit = this@UploadToViewModel.onRetry
        override val onAppendErrorAction: () -> Unit = this@UploadToViewModel.onRetry
        override val upload: () -> Unit = { upload(navigateToStorageFull, exitApp) }
        override val onCreateFolder: () -> Unit = { this@UploadToViewModel.onCreateFolder(navigateToCreateFolder) }
    }

    private fun upload(navigateToStorageFull: () -> Unit, exitApp: () -> Unit) = viewModelScope.launch {
        parentLink.value?.let { folder ->
            val copiedUris = mutableListOf<String>()
            coRunCatching {
                val job = launch {
                    delay(1.seconds)
                    showInfo(I18N.string.files_upload_preparing)
                }
                uploadParameters?.uris?.map { uriWithFileName ->
                    copyUriToCacheTempFolder(
                        userId,
                        uriWithFileName.uri,
                        uriWithFileName.fileName,
                    ).getOrThrow().toString().also { uriString ->
                        copiedUris.add(uriString)
                    }
                }?.let { localUris ->
                    job.cancel()
                    uploadFiles(
                        folder = folder,
                        uploadFileDescriptions = localUris.map { uri -> UploadFileDescription(uri) },
                        shouldDeleteSource = true,
                        notifications = Notifications.TurnedOnExceptPreparingUpload,
                        priority = UploadFileLink.USER_PRIORITY,
                    ).getOrThrow()
                    delay(2.seconds)
                    exitApp()
                }
            }.onFailure { error ->
                error.log(LogTag.UPLOAD)
                cleanupOnFailure(copiedUris)
                when (error) {
                    is NotEnoughSpaceException -> navigateToStorageFull()
                    is FileAlreadyExistsException -> showError(
                        I18N.string.in_app_notification_importing_file_already_exists
                    )
                    else -> with (error) {
                        showError(getDefaultMessage(appContext, configurationProvider.useExceptionMessage))
                    }
                }
            }
        }
    }

    private fun cleanupOnFailure(localUris: List<String>) = localUris.forEach { uriString ->
        fileProvider.getFile(uriString).delete()
    }

    companion object {
        const val URIS = "uris"
        const val PARENT_SHARE_ID = "parentShareId"
        const val PARENT_ID = "parentId"
    }
}
