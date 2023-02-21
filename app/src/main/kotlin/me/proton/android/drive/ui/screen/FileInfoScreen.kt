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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.viewmodel.FileInfoViewModel
import me.proton.android.drive.ui.viewstate.FileInfoViewState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.file.info.presentation.FileInfoContent
import me.proton.core.presentation.R as CorePresentation

@ExperimentalCoroutinesApi
@Composable
fun FileInfoScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<FileInfoViewModel>()
    val viewState by rememberFlowWithLifecycle(viewModel.viewState).collectAsState(initial = null)
    FileInfo(
        viewState = viewState,
        modifier = modifier.fillMaxSize(),
        navigateBack = navigateBack,
    )
}

@Composable
fun FileInfo(
    viewState: FileInfoViewState?,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    Column(
        modifier = modifier.navigationBarsPadding()
    ) {
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = navigateBack,
            title = "",
            modifier = Modifier.statusBarsPadding()
        )
        viewState?.let {
            FileInfoContent(
                driveLink = viewState.link,
                pathToFileNode = viewState.path,
                modifier = Modifier
                    .padding(top = DefaultSpacing)
                    .padding(horizontal = DefaultSpacing)
            )
        }
    }
}
