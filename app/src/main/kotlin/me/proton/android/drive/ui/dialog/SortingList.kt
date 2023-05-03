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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.SortingDialogViewModel
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.compose.theme.ProtonDimens.DefaultBottomSheetHeaderMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmall
import me.proton.core.drive.sorting.presentation.SortingList
import me.proton.core.drive.sorting.presentation.entity.toSortingOptions
import me.proton.core.drive.i18n.R as I18N

@Composable
fun SortingList(
    runAction: RunAction,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SortingDialogViewModel>()
    Column(
        modifier = modifier.navigationBarsPadding()
    ) {
        SortingHeader()
        SortingList(
            sortingOptions = viewModel.sorting.toSortingOptions(),
            onSortingOption = { sortingOption ->
                runAction { viewModel.setSorting(sortingOption.toggleDirection) }
            },
        )
    }
}

@Composable
fun SortingHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = DefaultSpacing)
            .height(DefaultBottomSheetHeaderMinHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = stringResource(id = I18N.string.title_sort_by),
            style = ProtonTheme.typography.headlineSmall
        )
    }
}
