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

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.common.onClick
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.usecase.OnFilesDriveLinkError
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.download.domain.usecase.GetDownloadProgress
import me.proton.core.drive.drivelink.list.domain.usecase.GetPagedDriveLinksList
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.drivelink.selection.domain.usecase.SelectAll
import me.proton.core.drive.files.presentation.event.FilesViewEvent
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.files.presentation.state.ListContentAppendingState
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.drive.files.presentation.state.ListEffect
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinks
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.drive.upload.domain.usecase.CancelUploadFile
import me.proton.core.drive.upload.domain.usecase.GetUploadProgress
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.usecase.GetLayoutType
import me.proton.drive.android.settings.domain.usecase.ToggleLayoutType
import javax.inject.Inject
import me.proton.core.drive.base.domain.extension.combine as baseCombine
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@ExperimentalCoroutinesApi
@SuppressLint("StaticFieldLeak")
class FilesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getDownloadProgress: GetDownloadProgress,
    getDriveLink: GetDecryptedDriveLink,
    private val cancelUploadFile: CancelUploadFile,
    getLayoutType: GetLayoutType,
    private val toggleLayoutType: ToggleLayoutType,
    private val getPagedDriveLinks: GetPagedDriveLinksList,
    private val getUploadFileLinks: GetUploadFileLinks,
    private val getUploadProgress: GetUploadProgress,
    private val onFilesDriveLinkError: OnFilesDriveLinkError,
    private val selectLinks: SelectLinks,
    private val selectAll: SelectAll,
    private val deselectLinks: DeselectLinks,
    private val getSelectedDriveLinks: GetSelectedDriveLinks,
    private val savedStateHandle: SavedStateHandle,
    getSorting: GetSorting,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle), HomeTabViewModel {

    private val shareId = savedStateHandle.get<String>(Screen.Files.SHARE_ID)
    private val folderId = savedStateHandle.get<String>(Screen.Files.FOLDER_ID)?.let { folderId ->
        shareId?.let { FolderId(ShareId(userId, shareId), folderId) }
    }
    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val driveLink: StateFlow<DriveLink.Folder?> = retryTrigger.transformLatest {
        emitAll(
            getDriveLink(userId, folderId, failOnDecryptionError = false)
                .filterSuccessOrError()
                .mapWithPrevious { previous, result ->
                    result
                        .onSuccess { driveLink ->
                            CoreLogger.d(VIEW_MODEL, "drive link onSuccess")
                            return@mapWithPrevious driveLink
                        }
                        .onFailure { error ->
                            onFilesDriveLinkError(userId, previous, error, listContentState)
                            error.log(VIEW_MODEL)
                        }
                    return@mapWithPrevious null
                }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val driveLinks: Flow<PagingData<DriveLink>> =
        driveLink.filterNotNull()
            .flatMapLatest { folder -> getPagedDriveLinks(folder.id) }
            .cachedIn(viewModelScope)

    val uploadingFileLinks: Flow<List<UploadFileLink>> = driveLink
        .filterNotNull()
        .flatMapLatest { driveLink ->
            getUploadFileLinks(userId, driveLink.id)
        }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val sorting: Flow<Sorting> = getSorting(userId).shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val isRootFolder: Boolean = folderId == null || folderId.id.isEmpty()
    private val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    private val listContentAppendingState = MutableStateFlow<ListContentAppendingState>(ListContentAppendingState.Idle)
    private val _listEffect = MutableSharedFlow<ListEffect>()
    private val _homeEffect = MutableSharedFlow<HomeEffect>()
    private val layoutType = getLayoutType(userId).stateIn(viewModelScope, SharingStarted.Eagerly, LayoutType.DEFAULT)
    private val selectionId = MutableStateFlow(savedStateHandle.get<String?>(KEY_SELECTION_ID)?.let { SelectionId(it) })
    private val addFilesAction = FilesViewState.Action(
        iconResId = CorePresentation.drawable.ic_proton_plus,
        contentDescriptionResId = I18N.string.content_description_files_upload_upload_file,
        onAction = { viewEvent?.onParentFolderOptions?.invoke() },
    )
    private val selectAllAction = FilesViewState.Action(
        iconResId = CorePresentation.drawable.ic_proton_check_triple,
        contentDescriptionResId = I18N.string.content_description_select_all,
        onAction = {
            viewModelScope.launch {
                driveLink.value?.let { parent ->
                    selectAll(parent.id, selectionId.value)
                }
            }
        }
    )
    private val selectedOptionsAction = FilesViewState.Action(
        iconResId = CorePresentation.drawable.ic_proton_three_dots_vertical,
        contentDescriptionResId = I18N.string.content_description_selected_options,
        onAction = { viewEvent?.onSelectedOptions?.invoke() }
    )
    private val topBarActions: MutableStateFlow<Set<FilesViewState.Action>> = MutableStateFlow(setOf(addFilesAction))
    private val selected: StateFlow<Set<LinkId>> = selectionId
        .filterNotNull()
        .transformLatest { id ->
            val parentId = driveLink.filterNotNull().first().id
            emitAll(
                getSelectedDriveLinks(id, parentId).map { driveLinks ->
                    if (driveLinks.isEmpty()) {
                        topBarActions.value = setOf(addFilesAction)
                    } else {
                        topBarActions.value = setOf(selectAllAction, selectedOptionsAction)
                    }
                    driveLinks.map { driveLink -> driveLink.id }.toSet()
                }
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val isBottomNavigationEnabled: Flow<Boolean> = selected.map { set -> set.isEmpty() }
    val initialViewState = FilesViewState(
        title = savedStateHandle[Screen.Files.FOLDER_NAME],
        titleResId = I18N.string.title_my_files,
        sorting = Sorting.DEFAULT,
        navigationIconResId = if (isRootFolder && selected.value.isEmpty()) {
            CorePresentation.drawable.ic_proton_hamburger
        } else {
            CorePresentation.drawable.ic_arrow_back
        },
        drawerGesturesEnabled = isRootFolder,
        listContentState = listContentState.value,
        listContentAppendingState = listContentAppendingState.value,
        getUploadProgress = ::getUploadProgressFlow,
        isRefreshEnabled = selected.value.isEmpty(),
        selected = selected,
        topBarActions = topBarActions,
    )
    val viewState: Flow<FilesViewState> = baseCombine(
        driveLink,
        sorting,
        listContentState,
        listContentAppendingState,
        layoutType,
        selected,
    ) { driveLink, sorting, contentState, appendingState, layoutType, selected ->
        initialViewState.copy(
            title = if (selected.isNotEmpty()) {
                appContext.quantityString(
                    I18N.plurals.common_selected,
                    selected.size
                )
            } else {
                if (isRootFolder) null else {
                    driveLink?.name ?: initialViewState.title
                }
            },
            isTitleEncrypted = selected.isEmpty() && isRootFolder.not() && driveLink.isNameEncrypted,
            navigationIconResId = if (isRootFolder && selected.isEmpty()) {
                CorePresentation.drawable.ic_proton_hamburger
            } else if (selected.isNotEmpty()) {
                CorePresentation.drawable.ic_proton_cross
            } else {
                CorePresentation.drawable.ic_arrow_back
            },
            sorting = sorting,
            listContentState = contentState,
            listContentAppendingState = appendingState,
            isGrid = layoutType == LayoutType.GRID,
            isRefreshEnabled = selected.isEmpty(),
            drawerGesturesEnabled = isRootFolder && selected.isEmpty(),
        )
    }
    val listEffect: Flow<ListEffect>
        get() = _listEffect.asSharedFlow()
    override val homeEffect: Flow<HomeEffect>
        get() = _homeEffect.asSharedFlow()

    private val emptyState = ListContentState.Empty(
        imageResId = BasePresentation.drawable.empty_folder,
        titleId = if (isRootFolder) I18N.string.title_empty_my_files else I18N.string.title_empty_folder,
        descriptionResId = if (isRootFolder) I18N.string.description_empty_my_files else I18N.string.description_empty_folder,
        actionResId = I18N.string.action_empty_files_add_files,
    )
    private var viewEvent: FilesViewEvent? = null
    fun viewEvent(
        navigateToFiles: (folderId: FolderId, folderName: String?) -> Unit,
        navigateToPreview: (fileId: FileId) -> Unit,
        navigateToSortingDialog: (Sorting) -> Unit,
        navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
        navigateToMultipleFileOrFolderOptions: (selectionId: SelectionId) -> Unit,
        navigateToParentFolderOptions: (folderId: FolderId) -> Unit,
        navigateBack: () -> Unit,
    ): FilesViewEvent = object : FilesViewEvent {

        private val driveLinkShareFlow = MutableSharedFlow<DriveLink>(extraBufferCapacity = 1).also { flow ->
            viewModelScope.launch {
                flow.take(1).collect { driveLink ->
                    driveLink.onClick(navigateToFiles, navigateToPreview)
                }
            }
        }

        override val onTopAppBarNavigation = {
            if (selected.value.isNotEmpty()) {
                selectionId.value?.let { viewModelScope.launch { deselectLinks(it) } }
                Unit
            } else {
                if (isRootFolder) {
                    viewModelScope.launch { _homeEffect.emit(HomeEffect.OpenDrawer) }
                    Unit
                } else {
                    navigateBack()
                }
            }
        }
        override val onSorting = navigateToSortingDialog
        override val onDriveLink = { driveLink: DriveLink ->
            if (selected.value.isNotEmpty()) {
                if (selected.value.contains(driveLink.id)) {
                    removeSelected(listOf(driveLink.id))
                } else {
                    addSelected(listOf(driveLink.id))
                }
            } else {
                driveLinkShareFlow.tryEmit(driveLink)
                Unit
            }
        }
        override val onLoadState = onLoadState(
            appContext = appContext,
            useExceptionMessage = configurationProvider.useExceptionMessage,
            listContentState = listContentState,
            listAppendContentState = listContentAppendingState,
            coroutineScope = viewModelScope,
            emptyState = emptyState,
        ) { message ->
            viewModelScope.launch {
                _homeEffect.emit(HomeEffect.ShowSnackbar(message))
            }
        }
        override val onErrorAction = { retry() }
        override val onAppendErrorAction = { retry() }
        override val onMoreOptions = { driveLink: DriveLink -> navigateToFileOrFolderOptions(driveLink.id) }
        override val onSelectedOptions = { onSelectedOptions(navigateToFileOrFolderOptions, navigateToMultipleFileOrFolderOptions) }
        override val onParentFolderOptions = { onParentFolderOptions(navigateToParentFolderOptions) }
        override val onCancelUpload = { uploadFileLink: UploadFileLink -> onCancelUpload(uploadFileLink) }
        override val onAddFiles = { onParentFolderOptions(navigateToParentFolderOptions) }
        override val onToggleLayout = this@FilesViewModel::onToggleLayout
        override val onSelectDriveLink = { driveLink: DriveLink -> addSelected(listOf(driveLink.id)) }
        override val onDeselectDriveLink = { driveLink: DriveLink -> removeSelected(listOf(driveLink.id)) }
        override val onBack = { removeAllSelected() }
    }.also { viewEvent ->
        this.viewEvent = viewEvent
    }

    fun addSelected(linkIds: List<LinkId>) {
        viewModelScope.launch {
            selectionId.value?.let { selectionId ->
                selectLinks(selectionId, linkIds)
            } ?: setSelectionId(selectLinks(linkIds).getOrNull())
        }
    }

    fun removeSelected(linkIds: List<LinkId>) {
        viewModelScope.launch {
            selectionId.value?.let { selectionId ->
                deselectLinks(selectionId, linkIds)
            }
        }
    }

    private fun removeAllSelected() {
        if (selected.value.isNotEmpty()) {
            viewModelScope.launch {
                selectionId.value?.let { selectionId -> deselectLinks(selectionId) }
            }
        }
    }

    private fun setSelectionId(selectionId: SelectionId?) {
        this.selectionId.value = selectionId
        savedStateHandle[KEY_SELECTION_ID] = selectionId?.id
    }

    fun getDownloadProgressFlow(link: DriveLink): Flow<Percentage>? =
        if (link is DriveLink.File) {
            getDownloadProgress(link)
        } else {
            null
        }

    private fun onToggleLayout() {
        viewModelScope.launch { toggleLayoutType(userId = userId, currentLayoutType = layoutType.value) }
    }

    private fun getUploadProgressFlow(uploadFileLink: UploadFileLink): Flow<Percentage>? =
        getUploadProgress(uploadFileLink)

    private fun retry() {
        viewModelScope.launch {
            if (driveLink.value == null) {
                retryDriveLink()
            } else {
                retryList()
            }
        }
    }

    private suspend fun retryDriveLink() {
        retryTrigger.emit(Unit)
        listContentState.value = ListContentState.Loading
    }

    private suspend fun retryList() {
        _listEffect.emit(ListEffect.RETRY)
    }

    private fun onParentFolderOptions(navigateToParentFolderOptions: (folderId: FolderId) -> Unit) {
        viewModelScope.launch {
            navigateToParentFolderOptions(driveLink.filterNotNull().first().id)
        }
    }

    private fun onCancelUpload(uploadFileLink: UploadFileLink) {
        viewModelScope.launch {
            cancelUploadFile(uploadFileLink)
        }
    }

    private fun onSelectedOptions(
        navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
        navigateToMultipleFileOrFolderOptions: (selectionId: SelectionId) -> Unit,
    ) {
        if (selected.value.size == 1) {
            navigateToFileOrFolderOptions(selected.value.first())
        } else {
            selectionId.value?.let { selectionId -> navigateToMultipleFileOrFolderOptions(selectionId) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.REFRESH)
        }
    }

    override fun onCleared() {
        super.onCleared()
        selectionId.value?.let {
            CoroutineScope(Dispatchers.Main).launch {
                deselectLinks(it)
            }
        }
    }

    companion object {
        private const val KEY_SELECTION_ID = "key.selectionId"
    }
}
