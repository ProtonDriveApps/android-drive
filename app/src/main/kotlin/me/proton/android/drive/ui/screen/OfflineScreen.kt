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

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.navigation.PagerType
import me.proton.android.drive.ui.viewmodel.OfflineViewModel
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.files.presentation.component.DriveLinksFlow
import me.proton.core.drive.files.presentation.component.Files
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.sorting.domain.entity.Sorting

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
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<OfflineViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
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

    Files(
        driveLinks = DriveLinksFlow.PagingList(viewModel.driveLinks, viewModel.listEffect),
        viewState = viewState.filesViewState,
        viewEvent = viewEvent,
        modifier = modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
        getTransferProgress = viewModel::getDownloadProgressFlow,
    )
}
