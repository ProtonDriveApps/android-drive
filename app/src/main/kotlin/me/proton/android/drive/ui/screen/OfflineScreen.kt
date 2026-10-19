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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.ui.effect.OfflineEffect
import me.proton.android.drive.ui.navigation.PagerType
import me.proton.android.drive.ui.viewmodel.OfflineViewModel
import me.proton.android.drive.ui.viewmodel.RemoveAllOfflineIconState
import me.proton.core.drive.base.presentation.component.BottomSheetContent
import me.proton.core.drive.base.presentation.component.BottomSheetEntry
import me.proton.core.drive.base.presentation.component.ModalBottomSheetViewState
import me.proton.core.drive.base.presentation.component.rememberModalBottomSheetContentState
import me.proton.core.util.kotlin.exhaustive
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongNorm
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.ModalBottomSheet
import me.proton.core.drive.files.presentation.component.DriveLinksFlow
import me.proton.core.drive.files.presentation.component.Files
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
@ExperimentalCoroutinesApi
fun OfflineScreen(
    navigateToFiles: (folderId: FolderId, folderName: String?) -> Unit,
    navigateToPreview: (pagerType: PagerType, fileId: FileId) -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    navigateToSortingDialog: (Sorting) -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    navigateToAlbumOptions: (AlbumId) -> Unit,
    navigateBack: () -> Unit,
    navigateToConfirmRemoveAllOffline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<OfflineViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        initialValue = viewModel.initialViewState
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateToFiles = navigateToFiles,
            navigateToPreview = navigateToPreview,
            navigateToAlbum = navigateToAlbum,
            navigateToSortingDialog = navigateToSortingDialog,
            navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
            navigateToAlbumOptions = navigateToAlbumOptions,
            navigateBack = navigateBack,
            lifecycle = lifecycle,
        )
    }
    val removeAllOfflineIconState by viewModel.removeAllOfflineIconState.collectAsStateWithLifecycle(
        initialValue = RemoveAllOfflineIconState.HIDDEN
    )
    val modalBottomSheetContentState = rememberModalBottomSheetContentState()

    LaunchedEffect(viewModel) {
        viewModel.offlineEffect
            .onEach { effect ->
                when (effect) {
                    is OfflineEffect.MoreOptions -> {
                        modalBottomSheetContentState.sheetContent.value = { runAction ->
                            OfflineMoreOptions {
                                runAction { navigateToConfirmRemoveAllOffline() }
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
        Files(
            driveLinks = DriveLinksFlow.PagingList(viewModel.driveLinks, viewModel.listEffect),
            viewState = viewState.filesViewState,
            viewEvent = viewEvent,
            modifier = modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            getTransferProgress = viewModel::getDownloadProgressFlow,
        ) {
            Crossfade(removeAllOfflineIconState) { state ->
                when (state) {
                    RemoveAllOfflineIconState.HIDDEN -> Unit
                    RemoveAllOfflineIconState.VISIBLE -> ActionButton(
                        modifier = Modifier.padding(end = ExtraSmallSpacing),
                        icon = CorePresentation.drawable.ic_proton_three_dots_vertical,
                        contentDescription = I18N.string.common_more,
                        onClick = viewModel::onMoreOptionsClicked,
                    )
                }.exhaustive
            }
        }
    }
}

@Composable
private fun OfflineMoreOptions(
    modifier: Modifier = Modifier,
    navigateToConfirmRemoveAllOffline: () -> Unit,
) {
    BottomSheetContent(
        modifier = modifier.navigationBarsPadding(),
        header = {
            Text(
                text = stringResource(I18N.string.common_more),
                color = ProtonTheme.colors.textWeak,
                style = ProtonTheme.typography.defaultSmallStrongNorm,
            )
        },
        content = {
            BottomSheetEntry(
                icon = CorePresentation.drawable.ic_proton_circle_slash,
                title = stringResource(I18N.string.common_remove_all_from_offline_available_action),
                onClick = navigateToConfirmRemoveAllOffline,
            )
        },
    )
}
