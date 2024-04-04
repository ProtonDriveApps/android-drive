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

package me.proton.android.drive.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.viewmodel.SyncedFoldersViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.ProtonPullToRefresh
import me.proton.core.drive.files.presentation.component.DriveLinksFlow
import me.proton.core.drive.files.presentation.component.DriveLinksFlow.PagingList
import me.proton.core.drive.files.presentation.component.Files
import me.proton.core.drive.files.presentation.event.FilesViewEvent
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar

@Composable
fun SyncedFoldersScreen(
    homeScaffoldState: HomeScaffoldState,
    navigateToFiles: (FolderId, String?) -> Unit,
    navigateToSortingDialog: (Sorting) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SyncedFoldersViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent(navigateToFiles, navigateToSortingDialog, navigateBack)
    }
    viewModel.HandleHomeEffect(homeScaffoldState = homeScaffoldState)
    SyncedFolders(
        homeScaffoldState = homeScaffoldState,
        driveLinks = PagingList(viewModel.driveLinks, viewModel.listEffect),
        viewState = viewState,
        viewEvent = viewEvent,
        modifier = modifier
            .fillMaxSize()
            .testTag(SyncedFoldersTestTag.screen),
        onRefresh = viewModel::refresh,
    )
}

@Composable
fun SyncedFolders(
    homeScaffoldState: HomeScaffoldState,
    driveLinks: DriveLinksFlow,
    viewState: FilesViewState,
    viewEvent: FilesViewEvent,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
) {
    LaunchedEffect(viewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                viewState = viewState,
                viewEvent = viewEvent,
            )
        }
    }
    ProtonPullToRefresh(
        isPullToRefreshEnabled = viewState.isRefreshEnabled,
        isRefreshing = viewState.listContentState.isRefreshing,
        onRefresh = onRefresh,
    ) {
        Files(
            driveLinks = driveLinks,
            viewState = viewState,
            viewEvent = viewEvent,
            showTopAppBar = false,
            modifier = modifier
                .navigationBarsPadding(),
            )
    }
}

@Composable
private fun TopAppBar(
    viewState: FilesViewState,
    viewEvent: FilesViewEvent,
) {
    BaseTopAppBar(
        navigationIcon = if (viewState.navigationIconResId != 0) {
            painterResource(id = viewState.navigationIconResId)
        } else null,
        onNavigationIcon = viewEvent.onTopAppBarNavigation,
        title = viewState.title ?: "",
        isTitleEncrypted = viewState.isTitleEncrypted,
    )
}

object SyncedFoldersTestTag {
    const val screen = "computer synced folders screen"
}
