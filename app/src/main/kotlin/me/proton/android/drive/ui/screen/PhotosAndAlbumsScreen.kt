/*
 * Copyright (c) 2025 Proton AG.
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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.photos.presentation.component.Albums
import me.proton.android.drive.photos.presentation.viewevent.AlbumsViewEvent
import me.proton.android.drive.photos.presentation.viewstate.AlbumsViewState
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.viewmodel.AlbumsViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.component.TopBarActions

@Composable
fun PhotosAndAlbumsScreen(
    homeScaffoldState: HomeScaffoldState,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<AlbumsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        initialValue = viewModel.initialViewState
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent()
    }
    viewModel.HandleHomeEffect(homeScaffoldState)
    LaunchedEffect(viewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                viewState = viewState,
                viewEvent = viewEvent,
            ) {
                TopBarActions(actionFlow = viewState.topBarActions)
            }
        }
    }
    Albums(
        viewState = viewState,
        viewEvent = viewEvent,
        items = viewModel.albumItems,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun TopAppBar(
    viewState: AlbumsViewState,
    viewEvent: AlbumsViewEvent,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        navigationIcon = if (viewState.navigationIconResId != 0) {
            painterResource(id = viewState.navigationIconResId)
        } else null,
        notificationDotVisible = false,
        onNavigationIcon = viewEvent.onTopAppBarNavigation,
        title = "Albums",//TODO: this cannot be part of this view model
        actions = actions
    )
}
