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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.core.drive.log.presentation.component.LogOptions
import me.proton.core.drive.log.presentation.viewmodel.LogOptionsViewModel

@Composable
fun LogOptions(
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<LogOptionsViewModel>()
    LogOptions(
        logLevelItems = viewModel.logLevelItems,
        logOriginItems = viewModel.logOriginItems,
        viewState = viewModel.initialViewState,
        viewEvent = viewModel.viewEvent(),
        modifier = modifier
            .navigationBarsPadding(),
    )
}
