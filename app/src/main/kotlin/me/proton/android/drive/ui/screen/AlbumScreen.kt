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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.photos.presentation.component.Album
import me.proton.android.drive.ui.viewmodel.AlbumViewModel
import me.proton.core.compose.flow.rememberFlowWithLifecycle

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun AlbumScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<AlbumViewModel>()
    val viewState = viewModel.viewState.collectAsStateWithLifecycle(initialValue = null)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(navigateBack)
    }
    val photoItems = viewModel.driveLinks.collectAsLazyPagingItems()
    val listEffect = rememberFlowWithLifecycle(flow = viewModel.listEffect)

    viewState.value?.let { viewState ->
        Album(
            viewState = viewState,
            viewEvent = viewEvent,
            items = photoItems,
            listEffect = listEffect,
            driveLinksFlow = viewModel.driveLinksMap,
            isRefreshEnabled = viewState.isRefreshEnabled,
            isRefreshing = viewState.listContentState.isRefreshing,
            onRefresh = viewEvent.onRefresh,
            modifier = modifier.testTag(AlbumScreenTestTag.screen),
        )
    }
}

object AlbumScreenTestTag {
    const val screen = "album screen"
}
