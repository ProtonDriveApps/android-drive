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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.viewevent.ComputersViewEvent
import me.proton.android.drive.ui.viewmodel.ComputersViewModel
import me.proton.android.drive.ui.viewstate.ComputersViewState
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.ProtonPullToRefresh
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.onContent
import me.proton.core.drive.base.presentation.extension.onEmpty
import me.proton.core.drive.base.presentation.extension.onError
import me.proton.core.drive.base.presentation.extension.onLoading
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.drivelink.device.presentation.component.DevicesContent
import me.proton.core.drive.drivelink.device.presentation.component.DevicesEmpty
import me.proton.core.drive.drivelink.device.presentation.component.DevicesError
import me.proton.core.drive.drivelink.device.presentation.component.DevicesLoading
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar

@Composable
fun ComputersScreen(
    homeScaffoldState: HomeScaffoldState,
    navigateToSyncedFolders: (FolderId, String?) -> Unit,
    navigateToComputerOptions: (deviceId: DeviceId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ComputersViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateToSyncedFolders,
            navigateToComputerOptions,
        )
    }
    val devices by rememberFlowWithLifecycle(viewModel.devices).collectAsState(initial = null)
    viewModel.HandleHomeEffect(homeScaffoldState)
    Computers(
        homeScaffoldState = homeScaffoldState,
        viewState = viewState,
        viewEvent = viewEvent,
        devices = devices ?: emptyList(),
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun Computers(
    homeScaffoldState: HomeScaffoldState,
    viewState: ComputersViewState,
    viewEvent: ComputersViewEvent,
    devices: List<Device>,
    modifier: Modifier = Modifier,
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
        onRefresh = viewEvent.onRefresh,
    ) {
        Computers(
            listContentState = viewState.listContentState,
            devices = devices,
            modifier = modifier.fillMaxSize(),
            onError = {},
            onDevice = { device ->
                viewEvent.onDevice(device)
            },
            onMoreOptions = { device ->
                viewEvent.onMoreOptions(device)
            },
            onRenderThumbnail = { device ->
                viewEvent.onRenderThumbnail(device)
            }
        )
    }
}

@Composable
fun Computers(
    listContentState: ListContentState,
    devices: List<Device>,
    modifier: Modifier = Modifier,
    onError: () -> Unit,
    onDevice: (Device) -> Unit,
    onMoreOptions: (Device) -> Unit,
    onRenderThumbnail: (Device) -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .conditional(listContentState !is ListContentState.Content) {
                // Needed for pull to refresh
                verticalScroll(scrollState)
            },
        contentAlignment = Alignment.Center,
    ) {
        listContentState
            .onLoading {
                DevicesLoading()
            }
            .onEmpty { state ->
                DevicesEmpty(
                    imageResId = state.imageResId,
                    titleResId = state.titleId,
                    descriptionResId = state.descriptionResId,
                )
            }
            .onError { state ->
                DevicesError(
                    errorMessage = state.message,
                    actionResId = state.actionResId,
                    onAction = onError,
                )
            }
            .onContent {
                DevicesContent(
                    devices = devices,
                    onClick = onDevice,
                    onMoreOptions = onMoreOptions,
                    onRenderThumbnail = onRenderThumbnail,
                    modifier = Modifier
                        .testTag(ComputersTestTag.content)
                )
            }
    }
}

@Composable
private fun TopAppBar(
    viewState: ComputersViewState,
    viewEvent: ComputersViewEvent,
) {
    BaseTopAppBar(
        navigationIcon = if (viewState.navigationIconResId != 0) {
            painterResource(id = viewState.navigationIconResId)
        } else null,
        notificationDotVisible = viewState.notificationDotVisible,
        onNavigationIcon = viewEvent.onTopAppBarNavigation,
        title = viewState.title,
    )
}

object ComputersTestTag {
    const val content = "computers content"
}
