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

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.effect.PreviewEffect
import me.proton.android.drive.ui.navigation.PagerType
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.usecase.OpenProtonDocumentInBrowser
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.arch.transformSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.function.pagedList
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.documentsprovider.domain.usecase.GetDocumentUri
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetSessionForkProtonDocumentUriString
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.getThumbnailId
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinksCount
import me.proton.core.drive.drivelink.download.domain.usecase.GetFile
import me.proton.core.drive.drivelink.list.domain.usecase.GetDecryptedDriveLinks
import me.proton.core.drive.drivelink.offline.domain.usecase.GetDecryptedOfflineDriveLinks
import me.proton.core.drive.drivelink.offline.domain.usecase.GetOfflineDriveLinksCount
import me.proton.core.drive.drivelink.sorting.domain.usecase.SortDriveLinks
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.files.preview.presentation.component.PreviewComposable
import me.proton.core.drive.files.preview.presentation.component.event.PreviewViewEvent
import me.proton.core.drive.files.preview.presentation.component.state.ContentState
import me.proton.core.drive.files.preview.presentation.component.state.PreviewContentState
import me.proton.core.drive.files.preview.presentation.component.state.PreviewViewState
import me.proton.core.drive.files.preview.presentation.component.toComposable
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.link.domain.extension.isProtonDocument
import me.proton.core.drive.link.domain.extension.requireFolderId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailVO
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.startsWith
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@Suppress("StaticFieldLeak", "LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val configurationProvider: ConfigurationProvider,
    getDriveLink: GetDriveLink,
    getDecryptedDriveLink: GetDecryptedDriveLink,
    private val getFile: GetFile,
    private val getDocumentUri: GetDocumentUri,
    private val getSessionForkProtonDocumentUriString: GetSessionForkProtonDocumentUriString,
    private val openProtonDocumentInBrowser: OpenProtonDocumentInBrowser,
    private val broadcastMessages: BroadcastMessages,
    getDecryptedDriveLinks: GetDecryptedDriveLinks,
    getDecryptedOfflineDriveLinks: GetDecryptedOfflineDriveLinks,
    getOfflineDriveLinksCount: GetOfflineDriveLinksCount,
    getDriveLinksCount: GetDriveLinksCount,
    getSorting: GetSorting,
    getPhotoShare: GetPhotoShare,
    getShare: GetShare,
    photoRepository: PhotoRepository,
    albumRepository: AlbumRepository,
    sortDriveLinks: SortDriveLinks,
    val savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val albumId =
        savedStateHandle.get<String>(Screen.PagerPreview.ALBUM_ID)?.let { albumIdString ->
            AlbumId(
                shareId = ShareId(userId, savedStateHandle.require(Screen.PagerPreview.ALBUM_SHARE_ID)),
                id = albumIdString,
            )
        }
    private val photoTag = savedStateHandle.get<String>(Screen.PagerPreview.PHOTO_TAG)?.let { tag ->
        PhotoTag.fromLong(tag.toLong())
    }
    private val trigger = MutableSharedFlow<Trigger>(1).apply {
        val shareId = savedStateHandle.require<String>(Screen.PagerPreview.SHARE_ID)
        val fileId = savedStateHandle.require<String>(Screen.PagerPreview.FILE_ID)
        tryEmit(Trigger(FileId(ShareId(userId, shareId), fileId)))
    }
    private val fileId: FileId
        get() = trigger.replayCache.first().fileId
    private val currentIndex = MutableStateFlow(-1)

    private var onInsertImageCallback: ((List<Uri>) -> Unit)? = null
    val _unused = savedStateHandle.getStateFlow<List<Uri>>(PROTON_DOCS_IMAGE_URIS, emptyList())
        .onEach { uris ->
            CoreLogger.d(VIEW_MODEL, "$PROTON_DOCS_IMAGE_URIS: $uris")
            onInsertImageCallback?.invoke(uris)?.also {
                onInsertImageCallback = null
                savedStateHandle.set<List<Uri>>(PROTON_DOCS_IMAGE_URIS, emptyList())
            }
        }
        .launchIn(viewModelScope)

    private val provider: PreviewContentProvider =
        when (savedStateHandle.require<PagerType>(Screen.PagerPreview.PAGER_TYPE)) {
            PagerType.FOLDER -> FolderContentProvider(
                userId = userId,
                getDriveLink = getDriveLink,
                getDecryptedDriveLink = getDecryptedDriveLink,
                getDriveLinksCount = getDriveLinksCount,
                getDecryptedDriveLinks = getDecryptedDriveLinks,
                getSorting = getSorting,
                sortDriveLinks = sortDriveLinks,
                coroutineScope = viewModelScope,
                fileId = fileId,
            )
            PagerType.SINGLE -> SingleContentProvider(
                getDecryptedDriveLink = getDecryptedDriveLink,
            )
            PagerType.OFFLINE -> OfflineContentProvider(
                userId = userId,
                getDecryptedOfflineDriveLinks = getDecryptedOfflineDriveLinks,
                getSorting = getSorting,
                getDecryptedDriveLink = getDecryptedDriveLink,
                sortDriveLinks = sortDriveLinks,
                getOfflineDriveLinksCount = getOfflineDriveLinksCount,
                coroutineScope = viewModelScope,
            )
            PagerType.PHOTO -> PhotoContentProvider(
                userId = userId,
                photoTag = photoTag,
                getDecryptedDriveLink = getDecryptedDriveLink,
                getPhotoShare = getPhotoShare,
                photoRepository = photoRepository,
                configurationProvider = configurationProvider,
                coroutineScope = viewModelScope,
            )
            PagerType.ALBUM -> AlbumContentProvider(
                userId = userId,
                albumId = requireNotNull(albumId) {"Missing albumId"} ,
                getDecryptedDriveLink = getDecryptedDriveLink,
                getShare = getShare,
                albumRepository = albumRepository,
                configurationProvider = configurationProvider,
                coroutineScope = viewModelScope,
            )
        }

    private val contentStatesCache = mutableMapOf<FileId, Flow<ContentState>>()
    private val protonDocumentUriStringCache = mutableMapOf<FileId, String>()

    private val driveLinks: StateFlow<List<DriveLink.File>?> = trigger.transformLatest { trigger ->
        emitAll(
            provider.getDriveLinks(trigger.fileId)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _previewEffect = MutableSharedFlow<PreviewEffect>()
    private val isFullscreen = MutableStateFlow(false)
    private val renderFailed = MutableStateFlow<Pair<Throwable, Any>?>(null)
    val initialViewState = PreviewViewState(
        navigationIconResId = CorePresentation.drawable.ic_arrow_back,
        isFullscreen = isFullscreen,
        previewContentState = PreviewContentState.Loading,
        items = emptyList(),
        currentIndex = 0,
        host = configurationProvider.host,
        appVersionHeader = configurationProvider.appVersionHeader,
    )
    val viewState: Flow<PreviewViewState> = driveLinks.filterNotNull().transformLatest { driveLinks ->
        if (driveLinks.isEmpty() && currentIndex.value != -1) {
            _previewEffect.emit(PreviewEffect.Close)
            return@transformLatest
        }
        val indexOfFirst = driveLinks.indexOfFirst { link -> link.id == fileId }
        val contentStates =
            driveLinks
                .filter { driveLink -> driveLink.activeRevisionId.isNotEmpty() }
                .associateBy({ link -> link.id }) { link -> link.getContentStateFlow() }
        val (previewContentState, index) = when {
            driveLinks.isEmpty() -> PreviewContentState.Empty to 0
            indexOfFirst == -1 -> PreviewContentState.Content to currentIndex.value.coerceAtLeast(0)
            else -> PreviewContentState.Content to indexOfFirst
        }
        currentIndex.value = index
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
                    contentState = contentStates[link.id] ?: flowOf(ContentState.Decrypting(link.photoThumbnailSource)),
                )
            },
            currentIndex = currentIndex.value,
        )
        if (BuildConfig.DEBUG) {
            CoreLogger.d(VIEW_MODEL, "$previewViewState")
        }
        emit(previewViewState)
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val previewEffect: Flow<PreviewEffect> = _previewEffect.asSharedFlow()
        .onStart { emit(PreviewEffect.Fullscreen(isFullscreen.value)) }

    fun viewEvent(
        navigateBack: () -> Unit,
        navigateToFileOrFolderOptions: (LinkId, AlbumId?) -> Unit,
        navigateToProtonDocsInsertImageOptions: () -> Unit,
    ): PreviewViewEvent = object : PreviewViewEvent {
        override val onTopAppBarNavigation = { navigateBack() }
        override val onMoreOptions = { navigateToFileOrFolderOptions(fileId, albumId) }
        override val onSingleTap = { toggleFullscreen() }
        override val onRenderFailed = { throwable: Throwable, source: Any -> renderFailed.value = throwable to source }
        override val mediaControllerVisibility = { visible: Boolean ->
            if ((visible && isFullscreen.value) || (!visible && !isFullscreen.value)) {
                toggleFullscreen()
            }
        }
        override val onOpenInBrowser = { openInBrowser() }
        override val onProtonDocsDownloadResult = { result: Result<String> ->
            result
                .onSuccess { name ->
                    broadcastMessages(
                        userId = userId,
                        message = appContext.getString(
                            I18N.string.common_in_app_notification_download_complete,
                            name,
                        ),
                        type = BroadcastMessage.Type.INFO,
                    )
                }
                .onFailure { error ->
                    error.log(VIEW_MODEL)
                    broadcastMessages(
                        userId = userId,
                        message = error.getDefaultMessage(
                            context = appContext,
                            useExceptionMessage = configurationProvider.useExceptionMessage,
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
            Unit
        }
        override val onProtonDocsShowFileChooser = {
            filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams? ->
            val acceptTypes = fileChooserParams?.acceptTypes?.toList() ?: listOf("image/*")
            when {
                acceptTypes.any { mimeType -> mimeType.startsWith("image/")} -> let {
                    navigateToProtonDocsInsertImageOptions().also {
                        this@PreviewViewModel.onInsertImageCallback = { uris ->
                            CoreLogger.d(LogTag.WEBVIEW, "onShowFileChooser callback ${uris.joinToString()}")
                            filePathCallback?.onReceiveValue(uris.takeIfNotEmpty()?.toTypedArray())
                        }
                    }
                    if (acceptTypes.any { mimeType -> !mimeType.startsWith("image/") }) {
                        CoreLogger.w(VIEW_MODEL, "Unsupported file type: ${acceptTypes.joinToString()}")
                        broadcastMessages(
                            userId = userId,
                            message = appContext.getString(I18N.string.proton_docs_unsupported_file_type),
                            type = BroadcastMessage.Type.WARNING,
                        )
                        false
                    } else {
                        true
                    }
                }
                else -> let {
                    CoreLogger.e(VIEW_MODEL, "Unsupported file type: ${acceptTypes.joinToString()}")
                    broadcastMessages(
                        userId = userId,
                        message = appContext.getString(I18N.string.proton_docs_unsupported_file_type),
                        type = BroadcastMessage.Type.ERROR,
                    )
                    false
                }
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
                    verifySignature = verifySignature,
                    retry = true,
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

    private fun getContentState(
        driveLink: DriveLink.File,
        getFileState: GetFile.State,
        renderFailed: Pair<Throwable, Any>? = null,
        fallback: Map<Any, Any?> = emptyMap(),
    ): ContentState {
        val uri = getUri(fileId)
        return renderFailed?.takeIf { (_, source) ->
            fallback[source] == source || source == uri
        }?.let { (throwable, source) ->
            fallback[source]?.let { fallbackSource ->
                getFileState.toContentState(this, fallbackSource)
            } ?: ContentState.Error.NonRetryable(
                message = throwable.getDefaultMessage(
                    context = appContext,
                    useExceptionMessage = configurationProvider.useExceptionMessage,
                ),
                messageResId = 0,
            )
        } ?: getFileState.toContentState(
            viewModel = this,
            source = uri,
            photoThumbnailSource = driveLink.photoThumbnailSource,
        )
    }

    fun getUri(fileId: FileId) = getDocumentUri(userId, fileId)

    private fun DriveLink.File.getContentStateFlow(): Flow<ContentState> =
        contentStatesCache.getOrPut(id) {
            if (mimeType.toFileTypeCategory().toComposable() == PreviewComposable.Unknown) {
                NO_PREVIEW_SUPPORTED
            } else if (isProtonDocument) {
                trigger.map {
                    getProtonDocumentUriString(this@getContentStateFlow, it.retry)
                        .fold(
                            onSuccess = { uriString ->
                                ContentState.Available(uriString).also {
                                    protonDocumentUriStringCache[id] = uriString
                                }
                            },
                            onFailure = { error ->
                                if (error.isRetryable) {
                                    ContentState.Error.Retryable(
                                        messageResId = I18N.string.proton_docs_load_error,
                                        actionResId = I18N.string.common_retry,
                                    ) {
                                        retry(verifySignature = true)
                                    }
                                } else {
                                    ContentState.Error.NonRetryable(
                                        message = error.getDefaultMessage(
                                            context = appContext,
                                            useExceptionMessage = configurationProvider.useExceptionMessage,
                                        ),
                                        messageResId = 0,
                                    )
                                }
                            }
                        )
                }
            } else {
                trigger.filter { trigger -> trigger.fileId == id }.flatMapLatest { trigger ->
                    combine(
                        getFile(this, trigger.verifySignature),
                        renderFailed,
                    ) { fileState, renderFailed ->
                        getContentState(this, fileState, renderFailed, previewFallbackSources)
                    }
                }
            }
        }

    private suspend fun getProtonDocumentUriString(driveLink: DriveLink.File, refresh: Boolean): Result<String> {
        val cachedUriString = protonDocumentUriStringCache[driveLink.id]
        return if (cachedUriString != null && !refresh) {
            Result.success(cachedUriString)
        } else {
            getSessionForkProtonDocumentUriString(driveLink).also { result ->
                if (result.isSuccess) {
                    protonDocumentUriStringCache[driveLink.id] = result.getOrThrow()
                }
            }
        }
    }

    private val DriveLink.File.previewFallbackSources: Map<Any, Any?> get() {
        val uri = getUri(id)
        val photoThumbnailVO = getThumbnailId(ThumbnailType.PHOTO)?.let { thumbnailVO(ThumbnailType.PHOTO) }
        val defaultThumbnailVO = getThumbnailId(ThumbnailType.DEFAULT)?.let { thumbnailVO(ThumbnailType.DEFAULT) }
        return when {
            photoThumbnailVO == null -> mapOf(uri to null)
            defaultThumbnailVO == null -> mapOf(
                uri to photoThumbnailVO,
                photoThumbnailVO to null,
            )
            else -> mapOf(
                uri to photoThumbnailVO,
                photoThumbnailVO to defaultThumbnailVO,
                defaultThumbnailVO to null,
            )
        }
    }

    private val DriveLink.File.photoThumbnailSource: ThumbnailVO? get() =
        takeIf { mimeType.toFileTypeCategory() == FileTypeCategory.Image }
            ?.let {
                getThumbnailId(ThumbnailType.PHOTO)?.let { thumbnailVO(ThumbnailType.PHOTO) } ?:
                    getThumbnailId(ThumbnailType.DEFAULT)?.let { thumbnailVO(ThumbnailType.DEFAULT) }
            }

    private fun openInBrowser() {
        driveLinks.value.orEmpty().firstOrNull { driveLink -> driveLink.id == fileId }
            ?.let { driveLink ->
                viewModelScope.launch {
                    openProtonDocumentInBrowser(driveLink)
                        .onFailure { error ->
                            error.log(LogTag.DEFAULT, "Open document failed")
                            val message = when {
                                error is ActivityNotFoundException -> appContext.getString(I18N.string.common_error_no_browser_available)
                                else -> error.getDefaultMessage(
                                    context = appContext,
                                    useExceptionMessage = configurationProvider.useExceptionMessage,
                                )
                            }
                            broadcastMessages(
                                userId = userId,
                                message = message,
                                type = BroadcastMessage.Type.ERROR
                            )
                        }
                }
            }
    }

    private data class Trigger(
        val fileId: FileId,
        val verifySignature: Boolean = true,
        val retry: Boolean = false,
    )

    companion object {
        private val NO_PREVIEW_SUPPORTED = flowOf(ContentState.Available(Uri.EMPTY))
        const val PROTON_DOCS_IMAGE_URIS = "protonDocsImageUris"
    }
}


fun GetFile.State.toContentState(viewModel: PreviewViewModel, source: Any, photoThumbnailSource: ThumbnailVO? = null): ContentState {
    return when (this) {
        is GetFile.State.Downloading -> ContentState.Downloading(progress, photoThumbnailSource)
        GetFile.State.Decrypting -> ContentState.Decrypting(photoThumbnailSource)
        is GetFile.State.Ready -> ContentState.Available(source, photoThumbnailSource)
        GetFile.State.Error.NoConnection,
        is GetFile.State.Error.Downloading -> ContentState.Error.Retryable(
            messageResId = I18N.string.description_file_download_failed,
            actionResId = I18N.string.common_retry
        ) {
            viewModel.retry(verifySignature = true)
        }
        is GetFile.State.Error.Decrypting -> ContentState.Error.NonRetryable(
            message = null,
            messageResId = I18N.string.description_file_decryption_failed
        )
        is GetFile.State.Error.VerifyingSignature -> ContentState.Error.Retryable(
            messageResId = I18N.string.description_file_verify_signature_failed,
            actionResId = I18N.string.ignore_file_signature_action
        ) {
            viewModel.retry(verifySignature = false)
        }
        GetFile.State.Error.NotFound -> ContentState.NotFound
    }
}

interface PreviewContentProvider {
    fun getDriveLinks(fileId: FileId): Flow<List<DriveLink.File>>
}

@ExperimentalCoroutinesApi
class FolderContentProvider(
    private val userId: UserId,
    private val getDriveLink: GetDriveLink,
    private val getDecryptedDriveLink: GetDecryptedDriveLink,
    private val getDriveLinksCount: GetDriveLinksCount,
    private val getDecryptedDriveLinks: GetDecryptedDriveLinks,
    private val getSorting: GetSorting,
    private val sortDriveLinks: SortDriveLinks,
    coroutineScope: CoroutineScope,
    fileId: FileId,
) : PreviewContentProvider {

    private val folderId: StateFlow<FolderId?> =
        getDriveLink(fileId)
            .transformSuccess { (_, driveLink) ->
                emitAll(
                    getDriveLink(userId, folderId = driveLink.requireFolderId())
                )
            }
            .mapSuccessValueOrNull()
            .map { driveLink -> driveLink?.id }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val decryptedDriveLinks: StateFlow<List<DriveLink.File>> =
        folderId
            .transformLatest { folderId ->
                folderId?.let {
                    emitAll(
                        getDriveLinksCount(folderId)
                            .distinctUntilChanged()
                            .mapLatest {
                                getDecryptedDriveLinks(folderId)
                                    .getOrNull()
                                    ?.filterIsInstance<DriveLink.File>()
                                    ?: emptyList<DriveLink.File>()
                            }
                    )
                } ?: emit(emptyList())
            }
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    override fun getDriveLinks(fileId: FileId): Flow<List<DriveLink.File>> = combine(
        getSorting(userId),
        getDecryptedDriveLink(fileId).filterSuccessOrError().mapSuccessValueOrNull(),
        decryptedDriveLinks,
    ) { sorting, driveLink, links ->
        val driveLinks = if (driveLink == null) {
            links.toMutableList().apply {
                removeIf { link -> link.id == fileId }
            }
        } else {
            links.toMutableList().apply {
                if (removeIf { link -> link.id == fileId }) {
                    add(driveLink)
                }
            }
        }
        coRunCatching {
            sortDriveLinks(
                sorting = sorting,
                driveLinks = driveLinks,
            ).filterIsInstance<DriveLink.File>()
        }.fold(
            onSuccess = { sortedDriveLinks -> sortedDriveLinks },
            onFailure = { error ->
                error.log(LogTag.DEFAULT, "Sorting failed fallback to unsorted list")
                driveLinks
            }
        )
    }
}


@ExperimentalCoroutinesApi
class SingleContentProvider(
    private val getDecryptedDriveLink: GetDecryptedDriveLink,
) : PreviewContentProvider {

    override fun getDriveLinks(fileId: FileId): Flow<List<DriveLink.File>> =
        getDecryptedDriveLink(fileId)
            .filterSuccessOrError()
            .mapSuccessValueOrNull()
            .map { driveLink ->
                takeIf { driveLink != null && driveLink.isTrashed.not() }
                    ?.let {
                        listOfNotNull(driveLink)
                    }
                    ?: emptyList()
            }
}

@ExperimentalCoroutinesApi
class OfflineContentProvider(
    private val userId: UserId,
    private val getDecryptedOfflineDriveLinks: GetDecryptedOfflineDriveLinks,
    private val getSorting: GetSorting,
    private val getDecryptedDriveLink: GetDecryptedDriveLink,
    private val sortDriveLinks: SortDriveLinks,
    getOfflineDriveLinksCount: GetOfflineDriveLinksCount,
    coroutineScope: CoroutineScope,
) : PreviewContentProvider {

    private val decryptedOfflineDriveLinks: StateFlow<List<DriveLink.File>> =
        getOfflineDriveLinksCount(userId)
            .distinctUntilChanged()
            .mapLatest {
                getDecryptedOfflineDriveLinks(userId)
                    .getOrNull()
                    ?.filterIsInstance<DriveLink.File>()
                    ?: emptyList<DriveLink.File>()
            }
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    override fun getDriveLinks(fileId: FileId): Flow<List<DriveLink.File>> = combine(
        getSorting(userId),
        getDecryptedDriveLink(fileId).filterSuccessOrError().mapSuccessValueOrNull(),
        decryptedOfflineDriveLinks,
    ) { sorting, driveLink, links ->
        val driveLinks = if (driveLink == null) {
            links.toMutableList().apply {
                removeIf { link -> link.id == fileId }
            }
        } else {
            links.toMutableList().apply {
                if (removeIf { link -> link.id == fileId }) {
                    add(driveLink)
                }
            }
        }
        coRunCatching {
            sortDriveLinks(
                sorting = sorting,
                driveLinks = driveLinks,
            ).filterIsInstance<DriveLink.File>()
        }.fold(
            onSuccess = { sortedDriveLinks -> sortedDriveLinks },
            onFailure = { error ->
                error.log(LogTag.DEFAULT, "Sorting failed fallback to unsorted list")
                driveLinks
            }
        )
    }
}

class PhotoContentProvider(
    private val getDecryptedDriveLink: GetDecryptedDriveLink,
    getPhotoShare: GetPhotoShare,
    photoRepository: PhotoRepository,
    userId: UserId,
    photoTag: PhotoTag?,
    configurationProvider: ConfigurationProvider,
    coroutineScope: CoroutineScope,
) : PreviewContentProvider {

    private val photoShare: StateFlow<Share?> = getPhotoShare(userId)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val photoListings: StateFlow<List<PhotoListing>> = photoShare
        .filterNotNull()
        .distinctUntilChanged()
        .transform { photoShare ->
            emitAll(
                photoRepository.getPhotoListingCount(userId, photoShare.volumeId, photoTag)
                    .distinctUntilChanged()
                    .transformLatest {
                        emit(
                            pagedList(
                                pageSize = configurationProvider.dbPageSize,
                            ) { fromIndex, count ->
                                photoRepository.getPhotoListings(
                                    userId = userId,
                                    volumeId = photoShare.volumeId,
                                    fromIndex = fromIndex,
                                    count = count,
                                    tag = photoTag,
                                )
                            }
                        )
                    }
            )
        }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    override fun getDriveLinks(fileId: FileId): Flow<List<DriveLink.File>> = combine(
        photoShare.filterNotNull(),
        getDecryptedDriveLink(fileId).filterSuccessOrError().mapSuccessValueOrNull(),
        photoListings,
    ) { photoShare, driveLink, photoListings ->
        photoListings.map { photoListing ->
            if (photoListing.linkId == driveLink?.id) {
                driveLink
            } else {
                photoListing.placeholderDriveLink(photoShare)
            }
        }
    }
}

class AlbumContentProvider(
    private val getDecryptedDriveLink: GetDecryptedDriveLink,
    getShare: GetShare,
    albumRepository: AlbumRepository,
    userId: UserId,
    configurationProvider: ConfigurationProvider,
    coroutineScope: CoroutineScope,
    albumId: AlbumId,
) : PreviewContentProvider {

    private val photoShare: StateFlow<Share?> = getShare(albumId.shareId)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val albumPhotoListings: StateFlow<List<PhotoListing>> = photoShare
        .filterNotNull()
        .distinctUntilChanged()
        .transform { photoShare ->
            emitAll(
                albumRepository.getAlbumPhotoListingCount(userId, photoShare.volumeId, albumId)
                    .distinctUntilChanged()
                    .transformLatest {
                        emit(
                            pagedList(
                                pageSize = configurationProvider.dbPageSize,
                            ) { fromIndex, count ->
                                albumRepository.getAlbumPhotoListings(
                                    userId = userId,
                                    volumeId = photoShare.volumeId,
                                    albumId = albumId,
                                    fromIndex = fromIndex,
                                    count = count,
                                    sortingBy = PhotoListing.Album.SortBy.CAPTURED,
                                    sortingDirection = Direction.DESCENDING
                                )
                            }
                        )
                    }
            )
        }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    override fun getDriveLinks(fileId: FileId): Flow<List<DriveLink.File>> = combine(
        photoShare.filterNotNull(),
        getDecryptedDriveLink(fileId).filterSuccessOrError().mapSuccessValueOrNull(),
        albumPhotoListings,
    ) { photoShare, driveLink, albumPhotoListings ->
        albumPhotoListings.map { albumPhotoListing ->
            if (albumPhotoListing.linkId == driveLink?.id) {
                driveLink
            } else {
                albumPhotoListing.placeholderDriveLink(photoShare)
            }
        }
    }
}

private fun PhotoListing.placeholderDriveLink(
    photoShare: Share,
): DriveLink.File = DriveLink.File(
    link = Link.File(
        id = linkId as FileId,
        parentId = photoShare.rootFolderId,
        name = "",
        size = 0.bytes,
        lastModified = TimestampS(),
        mimeType = "",
        isShared = false,
        key = "",
        passphrase = "",
        passphraseSignature = "",
        numberOfAccesses = 0,
        shareUrlExpirationTime = null,
        uploadedBy = "",
        attributes = Attributes(0),
        permissions = Permissions(),
        state = Link.State.ACTIVE,
        nameSignatureEmail = null,
        hash = nameHash.orEmpty(),
        expirationTime = null,
        nodeKey = "",
        nodePassphrase = "",
        nodePassphraseSignature = "",
        signatureEmail = "",
        creationTime = TimestampS(),
        trashedTime = null,
        hasThumbnail = false,
        activeRevisionId = "",
        xAttr = null,
        sharingDetails = null,
        contentKeyPacket = "",
        contentKeyPacketSignature = null,
        photoCaptureTime = captureTime,
        photoContentHash = contentHash,
        mainPhotoLinkId = null,
    ),
    volumeId = photoShare.volumeId,
    isMarkedAsOffline = false,
    isAnyAncestorMarkedAsOffline = false,
    downloadState = null,
    trashState = null,
    cryptoName = CryptoProperty.Encrypted(""),
    cryptoXAttr = CryptoProperty.Encrypted(""),
    shareInvitationCount = null,
    shareMemberCount = null,
    shareUser = null,
)
