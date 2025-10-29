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

package me.proton.android.drive.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.viewmodel.FilesViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.ProtonPullToRefresh
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.files.presentation.component.DriveLinksFlow
import me.proton.core.drive.files.presentation.component.Files
import me.proton.core.drive.files.presentation.component.TopAppBar
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.sorting.domain.entity.Sorting

@Composable
@ExperimentalCoroutinesApi
fun FilesScreen(
    homeScaffoldState: HomeScaffoldState,
    navigateToFiles: (folderId: FolderId, folderName: String?) -> Unit,
    navigateToPreview: (linkId: FileId) -> Unit,
    navigateToSortingDialog: (Sorting) -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    navigateToMultipleFileOrFolderOptions: (selectionId: SelectionId) -> Unit,
    navigateToParentFolderOptions: (folderId: FolderId) -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToBlackFridayPromo: () -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<FilesViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val selected by rememberFlowWithLifecycle(flow = viewState.selected)
        .collectAsState(initial = null)
    val inMultiselect = remember(selected) { selected?.isNotEmpty() ?: false }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateToFiles,
            navigateToPreview,
            navigateToSortingDialog,
            navigateToFileOrFolderOptions,
            navigateToMultipleFileOrFolderOptions,
            navigateToParentFolderOptions,
            navigateToSubscription,
            navigateToBlackFridayPromo,
            navigateBack,
            lifecycle,
        )
    }
    BackHandler(enabled = inMultiselect) { viewEvent.onBack() }
    LaunchedEffect(viewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                viewState = viewState,
                viewEvent = viewEvent,
            ) {
                TopBarActions(actionFlow = viewState.topBarActions)
            }
        }
        //homeScaffoldState.drawerGesturesEnabled.value = viewState.drawerGesturesEnabled
        launch {
            viewModel.isBottomNavigationEnabled
                .collectLatest { bottomNavigationEnabled ->
                    homeScaffoldState.bottomNavigationEnabled.value = bottomNavigationEnabled
                }
        }
    }
    viewModel.HandleHomeEffect(homeScaffoldState)

    ProtonPullToRefresh(
        isPullToRefreshEnabled = viewState.isRefreshEnabled,
        isRefreshing = viewState.listContentState.isRefreshing,
        onRefresh = viewModel::refresh,
    ) {
        Files(
            driveLinks = DriveLinksFlow.PagingList(viewModel.driveLinks, viewModel.listEffect),
            viewState = viewState,
            viewEvent = viewEvent,
            modifier = modifier.testTag(MyFilesScreenTestTag.screen),
            getTransferProgress = viewModel::getDownloadProgressFlow,
            uploadingFileLinks = viewModel.uploadingFileLinks,
            showTopAppBar = false,
        )
    }
}

object MyFilesScreenTestTag {
    const val screen = "my files screen"
}
