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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.viewevent.SharedTabsViewEvent
import me.proton.android.drive.ui.viewmodel.SharedTabsViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.android.drive.ui.viewstate.SharedTab
import me.proton.android.drive.ui.viewstate.SharedTabsViewState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.ProtonTab
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar

@Composable
fun SharedTabsScreen(
    homeScaffoldState: HomeScaffoldState,
    navigateToFiles: (FolderId, String?) -> Unit,
    navigateToPreview: (FileId) -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    navigateToFileOrFolderOptions: (LinkId) -> Unit,
    navigateToUserInvitation: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SharedTabsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent()
    }
    viewModel.HandleHomeEffect(homeScaffoldState)
    LaunchedEffect(viewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                titleResId = viewState.titleResId,
                navigationIconResId = viewState.navigationIconResId,
                onTopAppBarNavigation = viewEvent.onTopAppBarNavigation,
            )
        }
    }
    SharedTabs(
        homeScaffoldState = homeScaffoldState,
        viewState = viewState,
        viewEvent = viewEvent,
        modifier = modifier.fillMaxSize(),
        navigateToFiles = navigateToFiles,
        navigateToPreview = navigateToPreview,
        navigateToAlbum = navigateToAlbum,
        navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
        navigateToUserInvitation = navigateToUserInvitation,
    )
}

@Composable
fun SharedTabs(
    homeScaffoldState: HomeScaffoldState,
    viewState: SharedTabsViewState,
    viewEvent: SharedTabsViewEvent,
    navigateToFiles: (FolderId, String?) -> Unit,
    navigateToPreview: (FileId) -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    navigateToFileOrFolderOptions: (LinkId) -> Unit,
    navigateToUserInvitation: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            viewState.tabs.forEach { sharedTab ->
                ProtonTab(
                    titleResId = sharedTab.titleResId,
                    isSelected = viewState.selectedTab == sharedTab,
                    onTab = { viewEvent.onTab(sharedTab) }
                )
            }
        }
        Box {
            Divider(
                color = ProtonTheme.colors.separatorNorm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
            )
        }
        when (viewState.selectedTab.type) {
            SharedTab.Type.SHARED_WITH_ME -> SharedWithMeScreen(
                homeScaffoldState = homeScaffoldState,
                navigateToFiles = navigateToFiles,
                navigateToPreview = navigateToPreview,
                navigateToAlbum = navigateToAlbum,
                navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
                navigateToUserInvitation = navigateToUserInvitation,
            )
            SharedTab.Type.SHARED_BY_ME -> SharedByMeScreen(
                homeScaffoldState = homeScaffoldState,
                navigateToFiles = navigateToFiles,
                navigateToPreview = navigateToPreview,
                navigateToAlbum = navigateToAlbum,
                navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
            )
        }
    }
}

@Composable
private fun TopAppBar(
    @StringRes titleResId: Int,
    @DrawableRes navigationIconResId: Int,
    modifier: Modifier = Modifier,
    onTopAppBarNavigation: () -> Unit,
) {
    BaseTopAppBar(
        navigationIcon = painterResource(id = navigationIconResId),
        onNavigationIcon = onTopAppBarNavigation,
        title = stringResource(id = titleResId),
        modifier = modifier,
    )
}
