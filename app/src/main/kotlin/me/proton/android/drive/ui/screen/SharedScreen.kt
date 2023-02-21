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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.common.ProtonSwipeRefresh
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.viewmodel.SharedViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.files.presentation.component.DriveLinksFlow
import me.proton.core.drive.files.presentation.component.Files
import me.proton.core.drive.files.presentation.component.TopAppBar
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.sorting.domain.entity.Sorting

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun SharedScreen(
    homeScaffoldState: HomeScaffoldState,
    navigateToFiles: (folderId: FolderId, folderName: String?) -> Unit,
    navigateToPreview: (fileId: FileId) -> Unit,
    navigateToSortingDialog: (Sorting) -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SharedViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    homeScaffoldState.drawerGesturesEnabled.value = viewState.filesViewState.drawerGesturesEnabled
    viewModel.HandleHomeEffect(homeScaffoldState)

    val viewEvent = remember {
        viewModel.viewEvent(
            navigateToFiles = navigateToFiles,
            navigateToPreview = navigateToPreview,
            navigateToSortingDialog = navigateToSortingDialog,
            navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
        )
    }

    LaunchedEffect(viewState.filesViewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                viewState = viewState.filesViewState,
                viewEvent = viewEvent,
            )
        }
    }

    ProtonSwipeRefresh(
        listContentState = viewState.filesViewState.listContentState,
        onRefresh = viewModel::refresh,
    ) {
        Files(
            driveLinks = DriveLinksFlow.NonPagingList(viewModel.driveLinksFlow),
            viewState = viewState.filesViewState,
            viewEvent = viewEvent,
            modifier = modifier.testTag(SharedScreenTestTag.screen),
            getTransferProgress = viewModel::getDownloadProgressFlow,
            showTopAppBar = false,
        )
    }
}

object SharedScreenTestTag {
    const val screen = "shared screen"
}
