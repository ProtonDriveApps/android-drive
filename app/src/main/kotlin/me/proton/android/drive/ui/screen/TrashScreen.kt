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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.R
import me.proton.android.drive.ui.common.ProtonSwipeRefresh
import me.proton.android.drive.ui.effect.TrashEffect
import me.proton.android.drive.ui.viewmodel.TrashViewModel
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.compose.component.bottomsheet.BottomSheetEntry
import me.proton.core.compose.component.bottomsheet.ModalBottomSheet
import me.proton.core.compose.component.bottomsheet.ModalBottomSheetViewState
import me.proton.core.compose.component.bottomsheet.rememberModalBottomSheetContentState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrong
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.TopAppBarHeight
import me.proton.core.drive.files.presentation.component.DriveLinksFlow
import me.proton.core.drive.files.presentation.component.Files
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.util.kotlin.exhaustive
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@Composable
@ExperimentalCoroutinesApi
@OptIn(ExperimentalMaterialApi::class)
fun TrashScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    navigateToEmptyTrash: () -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    navigateToSortingDialog: (Sorting) -> Unit,
) {
    val viewModel = hiltViewModel<TrashViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateBack = navigateBack,
            navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
            navigateToSortingDialog = navigateToSortingDialog,
        )
    }
    val snackbarHostState = remember { ProtonSnackbarHostState() }
    val modalBottomSheetContentState = rememberModalBottomSheetContentState()
    val trashIconState by rememberFlowWithLifecycle(flow = viewModel.emptyTrashState)
        .collectAsState(EmptyTrashIconState.HIDDEN)
    LaunchedEffect(viewModel, LocalContext.current) {
        viewModel.trashEffect
            .onEach { effect ->
                when (effect) {
                    is TrashEffect.ShowSnackbar -> snackbarHostState.showSnackbar(
                        type = ProtonSnackbarType.ERROR,
                        message = effect.message
                    )
                    TrashEffect.MoreOptions -> {
                        modalBottomSheetContentState.sheetContent.value = { runAction ->
                            TrashMoreOptions {
                                runAction { navigateToEmptyTrash() }
                            }
                        }
                        modalBottomSheetContentState.sheetState.show()
                    }
                }
            }
            .launchIn(this)
    }
    ModalBottomSheet(
        sheetState = modalBottomSheetContentState.sheetState,
        sheetContent = modalBottomSheetContentState.sheetContent.value,
        viewState = remember { ModalBottomSheetViewState() },
    ) {
        Box(
            modifier = modifier.systemBarsPadding()
        ) {
            ProtonSwipeRefresh(
                listContentState = viewState.listContentState,
                onRefresh = viewModel::refresh,
                topPadding = TopAppBarHeight,
            ) {
                Files(
                    driveLinks = DriveLinksFlow.PagingList(viewModel.driveLinks, viewModel.listEffect),
                    viewState = viewState,
                    viewEvent = viewEvent,
                ) {
                    Crossfade(trashIconState) { trashIconState ->
                        when (trashIconState) {
                            EmptyTrashIconState.HIDDEN -> Unit
                            EmptyTrashIconState.LOADING -> Unit
                            EmptyTrashIconState.VISIBLE -> ActionButton(
                                modifier = Modifier.padding(end = ExtraSmallSpacing),
                                icon = CorePresentation.drawable.ic_proton_three_dots_vertical,
                                contentDescription = R.string.trash_more_options,
                                onClick = viewModel::onMoreOptionsClicked,
                            )
                        }.exhaustive
                    }
                }

                ProtonSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun TrashMoreOptions(
    modifier: Modifier = Modifier,
    navigateToEmptyTrash: () -> Unit,
) {
    BottomSheetContent(
        modifier = modifier.navigationBarsPadding(),
        header = {
            Text(
                text = stringResource(R.string.trash_more_options),
                color = ProtonTheme.colors.textWeak,
                style = ProtonTheme.typography.defaultSmallStrong,
            )
        },
        content = {
            BottomSheetEntry(
                icon = CorePresentation.drawable.ic_proton_trash_cross,
                title = stringResource(id = BasePresentation.string.title_empty_trash_action),
                onClick = navigateToEmptyTrash,
            )
        },
    )
}

enum class EmptyTrashIconState {
    HIDDEN, VISIBLE, LOADING
}
