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

package me.proton.android.drive.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.effect.PreviewEffect
import me.proton.android.drive.ui.navigation.PagerType
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.arch.transformSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.presentation.entity.toFileTypeCategory
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.documentsprovider.domain.usecase.GetDocumentUri
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.download.domain.usecase.GetFile
import me.proton.core.drive.drivelink.list.domain.usecase.GetSortedDecryptedDriveLinks
import me.proton.core.drive.drivelink.offline.domain.usecase.GetDecryptedOfflineDriveLinks
import me.proton.core.drive.files.preview.presentation.component.PreviewComposable
import me.proton.core.drive.files.preview.presentation.component.event.PreviewViewEvent
import me.proton.core.drive.files.preview.presentation.component.state.ContentState
import me.proton.core.drive.files.preview.presentation.component.state.PreviewContentState
import me.proton.core.drive.files.preview.presentation.component.state.PreviewViewState
import me.proton.core.drive.files.preview.presentation.component.state.ZoomEffect
import me.proton.core.drive.files.preview.presentation.component.toComposable
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModel @Inject constructor(
    getDriveLink: GetDecryptedDriveLink,
    private val getFile: GetFile,
    private val getDocumentUri: GetDocumentUri,
    getDecryptedDriveLinks: GetSortedDecryptedDriveLinks,
    getOfflineDriveNodes: GetDecryptedOfflineDriveLinks,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val trigger = MutableSharedFlow<Trigger>(1).apply {
        val shareId = savedStateHandle.require<String>(Screen.PagerPreview.SHARE_ID)
        val fileId = savedStateHandle.require<String>(Screen.PagerPreview.FILE_ID)
        tryEmit(Trigger(FileId(ShareId(userId, shareId), fileId)))
    }
    private val fileId: FileId
        get() = trigger.replayCache.first().fileId

    private val provider: PreviewContentProvider =
        when (savedStateHandle.require<PagerType>(Screen.PagerPreview.PAGER_TYPE)) {
            PagerType.FOLDER -> FolderContentProvider(
                userId = userId,
                getDriveLink = getDriveLink,
                getDecryptedDriveLinks = getDecryptedDriveLinks,
                fileId = fileId,
            )
            PagerType.SINGLE -> SingleContentProvider(
                getDriveLink = getDriveLink,
                fileId = fileId,
            )
            PagerType.OFFLINE -> OfflineContentProvider(userId, getOfflineDriveNodes)
        }

    private val driveLinks: StateFlow<List<DriveLink.File>?> = provider.getDriveLinks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _previewEffect = MutableSharedFlow<PreviewEffect>()
    private val _zoomEffect = MutableSharedFlow<ZoomEffect>()
    private val isFullscreen = MutableStateFlow(false)
    private val renderFailed = MutableStateFlow<Throwable?>(null)
    val initialViewState = PreviewViewState(
        navigationIconResId = CorePresentation.drawable.ic_arrow_back,
        isFullscreen = isFullscreen,
        previewContentState = PreviewContentState.Loading,
        items = emptyList(),
        currentIndex = 0,
    )
    val viewState: Flow<PreviewViewState> = driveLinks.filterNotNull().transformLatest { driveLinks ->
        val index = driveLinks.indexOfFirst { link -> link.id == fileId }
        val contentStates =
            driveLinks.associateBy({ link -> link.id }) { link -> link.getContentStateFlow() }
        val previewContentState = if (driveLinks.isEmpty()) PreviewContentState.Empty
        else PreviewContentState.Content
        val previewViewState = initialViewState.copy(
            isFullscreen = isFullscreen,
            previewContentState = previewContentState,
            items = driveLinks.map { link ->
                val category = link.mimeType.toFileTypeCategory()
                PreviewViewState.Item(
                    key = link.id.id,
                    title = link.name,
                    isTitleEncrypted = link.isNameEncrypted,
                    category = category,
                    contentState = requireNotNull(contentStates[link.id])
                )
            },
            currentIndex = index,
        )
        CoreLogger.d(VIEW_MODEL, "$previewViewState")
        emit(previewViewState)
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val previewEffect: Flow<PreviewEffect> = _previewEffect.asSharedFlow()
        .onStart { emit(PreviewEffect.Fullscreen(isFullscreen.value)) }
    val zoomEffect: Flow<ZoomEffect> = _zoomEffect.asSharedFlow()

    fun viewEvent(
        navigateBack: () -> Unit,
        navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    ): PreviewViewEvent = object : PreviewViewEvent {
        override val onTopAppBarNavigation = { navigateBack() }
        override val onMoreOptions = { navigateToFileOrFolderOptions(fileId) }
        override val onSingleTap = { toggleFullscreen() }
        override val onDoubleTap = { resetZoom() }
        override val onRenderFailed = { throwable: Throwable -> renderFailed.value = throwable }
        override val mediaControllerVisibility = { visible: Boolean ->
            if ((visible && isFullscreen.value) || (!visible && !isFullscreen.value)) {
                toggleFullscreen()
            }
        }
    }

    suspend fun onPageChanged(page: Int) {
        val links = driveLinks.value.orEmpty()
        if (page in links.indices) {
            val driveLink = links[page]
            if (trigger.replayCache.first().fileId != driveLink.id) {
                trigger.emit(Trigger(driveLink.id))
            }
        }
    }

    fun retry(verifySignature: Boolean) {
        viewModelScope.launch {
            trigger.emit(
                Trigger(
                    fileId = fileId,
                    verifySignature = verifySignature
                )
            )
        }
    }

    fun toggleFullscreen() {
        viewModelScope.launch {
            isFullscreen.value = !isFullscreen.value
            _previewEffect.emit(PreviewEffect.Fullscreen(isFullscreen.value))
        }
    }

    private fun resetZoom() {
        viewModelScope.launch {
            _zoomEffect.emit(ZoomEffect.Reset)
        }
    }

    private fun getContentState(
        getFileState: GetFile.State,
        renderFailed: Throwable? = null,
    ): ContentState {
        return renderFailed?.let { throwable ->
            ContentState.Error.NonRetryable(throwable.message, 0)
        } ?: getFileState.toContentState(this)
    }

    fun getUri(fileId: FileId) = getDocumentUri(userId, fileId)

    private fun DriveLink.File.getContentStateFlow(): Flow<ContentState> {
        if (mimeType.toFileTypeCategory().toComposable() == PreviewComposable.Unknown) {
            return NO_PREVIEW_SUPPORTED
        }
        var savedFlow: Flow<ContentState.Available>? = null
        return trigger.flatMapLatest { trigger ->
            val availableFlow = savedFlow
            when {
                availableFlow != null -> availableFlow
                trigger.fileId == id -> {
                    combine(
                        getFile(this, trigger.verifySignature),
                        renderFailed,
                    ) { fileState, renderFailed ->
                        getContentState(fileState, renderFailed).also { state ->
                            if (state is ContentState.Available) {
                                savedFlow = flowOf(state)
                            }
                        }
                    }
                }
                else -> DEFAULT_STATE
            }
        }
    }

    private data class Trigger(
        val fileId: FileId,
        val verifySignature: Boolean = true,
    )

    companion object {
        private val NO_PREVIEW_SUPPORTED = flowOf(ContentState.Available(Uri.EMPTY))
        private val DEFAULT_STATE = flowOf(ContentState.Downloading(null))
    }
}


fun GetFile.State.toContentState(viewModel: PreviewViewModel): ContentState {
    return when (this) {
        is GetFile.State.Downloading -> ContentState.Downloading(progress)
        GetFile.State.Decrypting -> ContentState.Decrypting
        is GetFile.State.Ready -> ContentState.Available(viewModel.getUri(fileId))
        GetFile.State.Error.NoConnection,
        is GetFile.State.Error.Downloading -> ContentState.Error.Retryable(
            messageResId = BasePresentation.string.description_file_download_failed,
            actionResId = BasePresentation.string.title_retry
        ) {
            viewModel.retry(verifySignature = true)
        }
        is GetFile.State.Error.Decrypting -> ContentState.Error.NonRetryable(
            message = null,
            messageResId = BasePresentation.string.description_file_decryption_failed
        )
        is GetFile.State.Error.VerifyingSignature -> ContentState.Error.Retryable(
            messageResId = BasePresentation.string.description_file_verify_signature_failed,
            actionResId = BasePresentation.string.ignore_file_signature_action
        ) {
            viewModel.retry(verifySignature = false)
        }
        GetFile.State.Error.NotFound -> ContentState.NotFound
    }
}

interface PreviewContentProvider {
    fun getDriveLinks(): Flow<List<DriveLink.File>>
}

@ExperimentalCoroutinesApi
class FolderContentProvider(
    private val userId: UserId,
    private val getDriveLink: GetDecryptedDriveLink,
    private val getDecryptedDriveLinks: GetSortedDecryptedDriveLinks,
    private val fileId: FileId,
) : PreviewContentProvider {

    override fun getDriveLinks(): Flow<List<DriveLink.File>> =
        getDriveLink(fileId)
            .transformSuccess<DriveLink.File, List<DriveLink.File>> { (_, driveLink) ->
                emitAll(
                    getDriveLink(userId, folderId = driveLink.parentId)
                        .transformSuccess { (_, folder) ->
                            emitAll(
                                getDecryptedDriveLinks(folder.id).map { driveLinksResult ->
                                    driveLinksResult.getOrNull()?.filterIsInstance<DriveLink.File>()?.asSuccess
                                        ?: DataResult.Error.Local(null, driveLinksResult.exceptionOrNull())
                                }
                            )
                        }
                )
            }
            .mapSuccessValueOrNull()
            .filterNotNull()
}


@ExperimentalCoroutinesApi
class SingleContentProvider(
    private val getDriveLink: GetDecryptedDriveLink,
    private val fileId: FileId,
) : PreviewContentProvider {

    override fun getDriveLinks(): Flow<List<DriveLink.File>> =
        getDriveLink(fileId)
            .mapSuccess { (_, driveLink) -> listOf(driveLink).asSuccess }
            .mapSuccessValueOrNull()
            .filterNotNull()
}

@ExperimentalCoroutinesApi
class OfflineContentProvider(
    private val userId: UserId,
    private val getOfflineDriveLinks: GetDecryptedOfflineDriveLinks,
) : PreviewContentProvider {

    override fun getDriveLinks(): Flow<List<DriveLink.File>> =
        getOfflineDriveLinks(userId)
            .map { driveLinks ->
                driveLinks.filterIsInstance<DriveLink.File>()
            }
}
