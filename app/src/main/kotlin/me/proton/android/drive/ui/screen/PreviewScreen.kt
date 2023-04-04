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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.ui.MainActivity
import me.proton.android.drive.ui.effect.PreviewEffect
import me.proton.android.drive.ui.viewmodel.PreviewViewModel
import me.proton.core.compose.activity.KeepScreenOn
import me.proton.core.compose.component.bottomsheet.ModalBottomSheet
import me.proton.core.compose.component.bottomsheet.ModalBottomSheetViewState
import me.proton.core.compose.component.bottomsheet.rememberModalBottomSheetContentState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.files.preview.presentation.component.Preview
import me.proton.core.drive.files.preview.presentation.component.PreviewEmpty
import me.proton.core.drive.files.preview.presentation.component.state.PreviewContentState
import me.proton.core.drive.link.domain.entity.LinkId
import kotlin.time.Duration.Companion.seconds

@Composable
fun PreviewScreen(
    navigateBack: () -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
) = PreviewScreen(
    viewModel = hiltViewModel(),
    navigateBack = navigateBack,
    navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
    modifier = Modifier.fillMaxSize(),
)

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun PreviewScreen(
    viewModel: PreviewViewModel,
    navigateBack: () -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOn()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val modalBottomSheetContentState = rememberModalBottomSheetContentState()
    val mainActivity = LocalContext.current as? MainActivity
    LaunchedEffect(LocalContext.current) {
        viewModel.previewEffect
            .onEach { previewEffect ->
                when (previewEffect) {
                    is PreviewEffect.Fullscreen -> {
                        mainActivity?.run {
                            if (previewEffect.isFullscreen) {
                                hideSystemBars()
                            } else {
                                showSystemBars()
                            }
                        }
                    }
                }
            }
            .launchIn(this)
    }
    val isFullScreen by rememberFlowWithLifecycle(viewState.isFullscreen).collectAsState(false)
    BackHandler(enabled = isFullScreen) {
        viewModel.toggleFullscreen()
    }
    ModalBottomSheet(
        sheetState = modalBottomSheetContentState.sheetState,
        sheetContent = modalBottomSheetContentState.sheetContent.value,
        viewState = remember { ModalBottomSheetViewState() },
    ) {
        when(viewState.previewContentState) {
            PreviewContentState.Content -> {
                Preview(
                    viewState = viewState,
                    viewEvent = viewModel.viewEvent(
                        navigateBack = navigateBack,
                        navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
                    ),
                    modifier = modifier,
                ) { page ->
                    viewModel.onPageChanged(page)
                }
            }
            PreviewContentState.Loading -> {
                Box(modifier.fillMaxSize()) {
                    Deferred(duration = 1.seconds) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
            }
            PreviewContentState.Empty -> {
                PreviewEmpty(
                    navigateBack = navigateBack,
                )
            }
        }
    }
}
