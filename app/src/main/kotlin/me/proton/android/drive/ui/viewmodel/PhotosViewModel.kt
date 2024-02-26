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
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.entity.PhotoBackupState
import me.proton.android.drive.photos.domain.usecase.EnablePhotosBackup
import me.proton.android.drive.photos.domain.usecase.GetPhotosDriveLink
import me.proton.android.drive.photos.presentation.R
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.PhotosViewEvent
import me.proton.android.drive.photos.presentation.viewmodel.BackupPermissionsViewModel
import me.proton.android.drive.photos.presentation.viewmodel.BackupStatusFormatter
import me.proton.android.drive.photos.presentation.viewmodel.SeparatorFormatter
import me.proton.android.drive.photos.presentation.viewstate.PhotosViewState
import me.proton.android.drive.ui.common.onClick
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.android.drive.usecase.OnFilesDriveLinkError
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.usecase.GetBackupState
import me.proton.core.drive.backup.domain.usecase.RetryBackup
import me.proton.core.drive.backup.domain.usecase.SyncFolders
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.combine
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.extension.launchApplicationDetailsSettings
import me.proton.core.drive.base.presentation.extension.launchIgnoreBatteryOptimizations
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.drivelink.photo.domain.usecase.GetPagedPhotoListingsList
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.drivelink.selection.domain.usecase.SelectAll
import me.proton.core.drive.files.presentation.state.ListContentAppendingState
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.drive.files.presentation.state.ListEffect
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink.Companion.RECENT_BACKUP_PRIORITY
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.usecase.GetPhotoCount
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.util.kotlin.CoreLogger
import java.util.Calendar
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
@Suppress("StaticFieldLeak", "LongParameterList")
class PhotosViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    getPagedPhotoListingsList: GetPagedPhotoListingsList,
    savedStateHandle: SavedStateHandle,
    private val separatorFormatter: SeparatorFormatter,
    private val backupStatusFormatter: BackupStatusFormatter,
    private val getPhotosDriveLink: GetPhotosDriveLink,
    private val enablePhotosBackup: EnablePhotosBackup,
    private val retryBackup: RetryBackup,
    private val backupPermissionsManager: BackupPermissionsManager,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
    getBackupState: GetBackupState,
    getPhotoCount: GetPhotoCount,
    getSelectedDriveLinks: GetSelectedDriveLinks,
    selectAll: SelectAll,
    selectLinks: SelectLinks,
    deselectLinks: DeselectLinks,
    val backupPermissionsViewModel: BackupPermissionsViewModel,
    private val photoDriveLinks: PhotoDriveLinks,
    private val onFilesDriveLinkError: OnFilesDriveLinkError,
    private val syncFolders: SyncFolders,
) : SelectionViewModel(
    savedStateHandle, selectLinks, deselectLinks, selectAll, getSelectedDriveLinks
), HomeTabViewModel {

    private var viewEvent: PhotosViewEvent? = null
    private var fetchingJob: Job? = null
    private val selectedOptionsAction
        get() = selectedOptionsAction {
            viewEvent?.onSelectedOptions?.invoke()
        }
    private val topBarActions: MutableStateFlow<Set<Action>> = MutableStateFlow(emptySet())

    private val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    private val firstVisibleItemIndex = MutableStateFlow<Int?>(null)
    private val forceStatusExpand = MutableStateFlow(false)

    val initialViewState = PhotosViewState(
        title = appContext.getString(I18N.string.photos_title),
        navigationIconResId = CorePresentation.drawable.ic_proton_hamburger,
        topBarActions = topBarActions,
        listContentState = ListContentState.Loading,
        showEmptyList = null,
        showPhotosStateIndicator = false,
        showPhotosStateBanner = false,
        backupStatusViewState = null,
        selected = selected,
        isRefreshEnabled = selected.value.isEmpty(),
    )
    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val driveLink: StateFlow<DriveLink.Folder?> = retryTrigger.transformLatest {
        emitAll(
            getPhotosDriveLink(userId)
                .filterSuccessOrError()
                .mapWithPrevious { previous, result ->
                    result
                        .onSuccess { driveLink ->
                            CoreLogger.d(VIEW_MODEL, "drive link onSuccess")
                            parentFolderId.value = driveLink.id
                            return@mapWithPrevious driveLink
                        }
                        .onFailure { error ->
                            onFilesDriveLinkError(
                                userId = userId,
                                previous = previous,
                                error = error,
                                contentState = listContentState,
                                shareType = Share.Type.PHOTO,
                            )
                            error.log(VIEW_MODEL, "Cannot get drive link")
                        }
                    return@mapWithPrevious null
                }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val driveLinksMap: Flow<Map<LinkId, DriveLink>> = photoDriveLinks.getDriveLinksMapFlow(userId)

    val driveLinks: Flow<PagingData<PhotosItem>> =
        parentFolderId
            .filterNotNull()
            .distinctUntilChanged()
            .transformLatest { folderId ->
                emitAll(
                    getPagedPhotoListingsList(userId)
                        .map { pagingData ->
                            pagingData.map { photoListing ->
                                PhotosItem.PhotoListing(
                                    photoListing.linkId,
                                    photoListing.captureTime,
                                    null
                                )
                            }
                        }
                        .map {
                            it.insertSeparators { before: PhotosItem.PhotoListing?, after: PhotosItem.PhotoListing? ->
                                if (after == null) {
                                    null
                                } else if (before == null) {
                                    PhotosItem.Separator(
                                        value = separatorFormatter.toSeparator(after.captureTime),
                                    )
                                } else {
                                    val beforeCalendar = Calendar.getInstance().apply {
                                        timeInMillis = before.captureTime.value * 1000L
                                    }
                                    val afterCalendar = Calendar.getInstance().apply {
                                        timeInMillis = after.captureTime.value * 1000L
                                    }
                                    if (beforeCalendar.get(Calendar.YEAR)
                                        != afterCalendar.get(Calendar.YEAR) ||
                                        beforeCalendar.get(Calendar.MONTH)
                                        != afterCalendar.get(Calendar.MONTH)
                                    ) {
                                        PhotosItem.Separator(
                                            value = separatorFormatter.toSeparator(after.captureTime),
                                        )
                                    } else {
                                        null
                                    }
                                }
                            }
                        }
                )
            }.cachedIn(viewModelScope)

    private val listContentAppendingState = MutableStateFlow<ListContentAppendingState>(
        ListContentAppendingState.Idle
    )

    private val _homeEffect = MutableSharedFlow<HomeEffect>()
    override val homeEffect: Flow<HomeEffect>
        get() = _homeEffect.asSharedFlow()

    private val _listEffect = MutableSharedFlow<ListEffect>()
    val listEffect: Flow<ListEffect>
        get() = _listEffect.asSharedFlow()

    private val emptyState = ListContentState.Empty(
        imageResId = emptyStateImageResId,
        titleId = I18N.string.photos_empty_title,
        descriptionResId = I18N.string.photos_empty_description,
        actionResId = 0,
    )
    private val emptyStateImageResId: Int get() = getThemeDrawableId(
        light = R.drawable.empty_photos_light,
        dark = R.drawable.empty_photos_dark,
        dayNight = R.drawable.empty_photos_daynight,
    )

    private val backupState = parentFolderId.filterNotNull().flatMapLatest { folderId ->
        getBackupState(folderId = folderId)
    }

    val viewState: Flow<PhotosViewState> = combine(
        selected,
        listContentState,
        backupState,
        getPhotoCount(userId = userId),
        firstVisibleItemIndex,
        forceStatusExpand,
    ) { selected, contentState, backupState, count, firstVisibleItemIndex, forceStatusExpand ->
        val listContentState = when (contentState) {
            is ListContentState.Empty -> contentState.copy(
                imageResId = emptyStateImageResId,
            )
            else -> contentState
        }
        if (selected.isEmpty()) {
            topBarActions.value = emptySet()
        } else {
            topBarActions.value = setOf(selectAllAction, selectedOptionsAction)
        }
        val isDisableOrRunning = !backupState.isBackupEnabled
                || backupState.backupStatus?.isRunning() == true
        val showPhotosStateBanner = isDisableOrRunning || forceStatusExpand
        val showPhotosStateIndicator = selected.isEmpty() &&
                ((firstVisibleItemIndex?.let { index -> index > 0 } ?: false)
                        || !isDisableOrRunning)
        initialViewState.copy(
            title = if (selected.isNotEmpty()) {
                appContext.quantityString(
                    I18N.plurals.common_selected,
                    selected.size
                )
            } else {
                appContext.getString(I18N.string.photos_title)
            },
            navigationIconResId = if (selected.isNotEmpty()) {
                CorePresentation.drawable.ic_proton_cross
            } else {
                CorePresentation.drawable.ic_proton_hamburger
            },
            listContentState = listContentState,
            showEmptyList = backupState.isBackupEnabled || backupState.hasDefaultFolder == false ,
            showPhotosStateIndicator = showPhotosStateIndicator,
            showPhotosStateBanner = showPhotosStateBanner,
            backupStatusViewState = backupStatusFormatter.toViewState(
                backupState = backupState,
                count = count.takeIf { configurationProvider.photosSavedCounter },
            ),
            isRefreshEnabled = selected.isEmpty(),
        )
    }

    fun viewEvent(
        navigateToPreview: (fileId: FileId) -> Unit,
        navigateToPhotosOptions: (fileId: FileId) -> Unit,
        navigateToMultiplePhotosOptions: (selectionId: SelectionId) -> Unit,
        navigateToSubscription: () -> Unit,
        navigateToPhotosIssues: (FolderId) -> Unit,
        navigateToBackupSettings: () -> Unit,
    ): PhotosViewEvent = object : PhotosViewEvent {

        private val driveLinkShareFlow =
            MutableSharedFlow<DriveLink>(extraBufferCapacity = 1).also { flow ->
                viewModelScope.launch {
                    flow.take(1).collect { driveLink ->
                        driveLink.onClick({ _, _ -> }, navigateToPreview)
                    }
                }
            }

        override val onTopAppBarNavigation = onTopAppBarNavigation {
            viewModelScope.launch { _homeEffect.emit(HomeEffect.OpenDrawer) }
            Unit
        }
        override val onDriveLink = { driveLink: DriveLink ->
            onDriveLink(driveLink) {
                driveLinkShareFlow.tryEmit(driveLink)
                Unit
            }
        }
        override val onLoadState: (CombinedLoadStates, Int) -> Unit = onLoadState(
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
        override val onRefresh = this@PhotosViewModel::onRefresh
        override val onErrorAction = this@PhotosViewModel::onErrorAction
        override val onSelectedOptions =
            { onSelectedOptions(navigateToPhotosOptions, navigateToMultiplePhotosOptions) }
        override val onSelectDriveLink = { driveLink: DriveLink -> onSelectDriveLink(driveLink) }
        override val onDeselectDriveLink =
            { driveLink: DriveLink -> onDeselectDriveLink(driveLink) }
        override val onBack = { onBack() }
        override val onEnable = this@PhotosViewModel::onEnable
        override val onPermissionsChanged = this@PhotosViewModel::onPermissionsChanged
        override val onPermissions = { appContext.launchApplicationDetailsSettings() }
        override val onRetry = this@PhotosViewModel::onRetry
        override val onScroll = this@PhotosViewModel::onScroll
        override val onStatusClicked = this@PhotosViewModel::onStatusClicked
        override val onGetStorage: () -> Unit = navigateToSubscription
        override val onResolveMissingFolder: () -> Unit = navigateToBackupSettings
        override val onChangeNetwork: () -> Unit = navigateToBackupSettings
        override val onIgnoreBackgroundRestrictions: (Context) -> Unit = {context ->
            context.launchIgnoreBatteryOptimizations()
        }
        override val onResolve: () -> Unit = {
            parentFolderId.value?.let { folderId ->
                navigateToPhotosIssues(folderId)
            }
        }
    }.also { viewEvent ->
        this.viewEvent = viewEvent
    }

    private fun onPermissionsChanged(backupPermissions: BackupPermissions) {
        viewModelScope.launch {
            if (backupPermissions == BackupPermissions.Granted) {
                val previousPhotosPermissions = backupPermissionsManager.backupPermissions.first()
                if (previousPhotosPermissions is BackupPermissions.Denied) {
                    enablePhotosBackup()
                }
            }
            backupPermissionsManager.onPermissionChanged(backupPermissions)
        }
    }

    private fun onEnable() {
        parentFolderId.value?.let { folderId ->
            backupPermissionsViewModel.toggleBackup(folderId) { state ->
                onPhotoBackupState(state)
            }

        }
    }

    private fun onScroll(firstVisibleItemIndex: Int, driveLinkIds: Set<LinkId>) {
        this.firstVisibleItemIndex.value = firstVisibleItemIndex
        if (driveLinkIds.isNotEmpty()) {
            fetchingJob?.cancel()
            fetchingJob = viewModelScope.launch {
                delay(300.milliseconds)
                photoDriveLinks.load(driveLinkIds)
            }
        }
    }

    private fun onStatusClicked() {
        forceStatusExpand.value = !forceStatusExpand.value
    }

    private suspend fun enablePhotosBackup() {
        parentFolderId.value?.let { folderId ->
            enablePhotosBackup(folderId).onSuccess { state ->
                onPhotoBackupState(state)
            }.onFailure { error ->
                error.log(BACKUP, "Cannot enable backup for folder: ${folderId.id.logId()}")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage
                    ),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
        }
    }

    private fun onPhotoBackupState(state: PhotoBackupState) {
        when (state) {
            is PhotoBackupState.NoFolder -> {
                broadcastMessages(
                    userId = userId,
                    message = appContext
                        .getString(I18N.string.photos_error_no_folders)
                        .format(state.folderName),
                    type = BroadcastMessage.Type.WARNING,
                )
            }

            is PhotoBackupState.Enabled -> {
                broadcastMessages(
                    userId = userId,
                    message = appContext.resources
                        .getQuantityString(
                            I18N.plurals.photos_message_folders_setup,
                            state.backupFolders.size
                        )
                        .format(state.folderName, state.backupFolders.size),
                    type = BroadcastMessage.Type.INFO,
                )
            }

            PhotoBackupState.Disabled -> Unit
        }
    }

    private fun onRetry() {
        viewModelScope.launch {
            parentFolderId.value?.let { folderId ->
                retryBackup(folderId).onFailure { error ->
                    error.log(BACKUP, "Cannot retry on backup")
                    broadcastMessages(
                        userId = userId,
                        message = error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
            }
        }
    }

    private fun BackupStatus.isRunning(): Boolean = when (this) {
        is BackupStatus.Complete -> totalBackupPhotos > 0
        else -> true
    }

    private fun onErrorAction() {
        viewModelScope.launch {
            if (driveLink.value == null) {
                retryLoadingPhotosDriveLinkFolder()
            } else {
                retryList()
            }
        }
    }

    private suspend fun retryLoadingPhotosDriveLinkFolder() {
        retryTrigger.emit(Unit)
        listContentState.value = ListContentState.Loading
    }

    private suspend fun retryList() {
        _listEffect.emit(ListEffect.RETRY)
    }

    private fun onRefresh() {
        viewModelScope.launch {
            parentFolderId.value?.let { folderId ->
                syncFolders(folderId, RECENT_BACKUP_PRIORITY)
                    .onFailure { error ->
                        error.log(VIEW_MODEL, "Failed sync folder on manual refresh")
                    }
            }
            _listEffect.emit(ListEffect.REFRESH)
        }
    }
}
