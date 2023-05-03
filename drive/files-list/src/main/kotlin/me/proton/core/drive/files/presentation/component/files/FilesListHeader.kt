/*
 * Copyright (c) 2021-2023 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.files.presentation.component.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.SmallIconSize
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.sorting.presentation.Sorting
import me.proton.core.drive.sorting.presentation.state.SortingViewState
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun FilesListHeader(
    sortingViewState: SortingViewState,
    isDisplayingGridView: Boolean,
    modifier: Modifier = Modifier,
    onSorting: () -> Unit,
    onToggleLayout: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ListHeaderHeight)
            .padding(end = SmallSpacing),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Sorting(
            viewState = sortingViewState,
            onSorting = onSorting,
        )
        IconButton(
            modifier = Modifier.size(DefaultButtonMinHeight),
            onClick = onToggleLayout
        ) {
            val (icon, contentDescription) = if (isDisplayingGridView) {
                CorePresentation.drawable.ic_proton_list_bullets to I18N.string.files_toggle_layout_to_list_content_description
            } else {
                CorePresentation.drawable.ic_proton_grid_2 to I18N.string.files_toggle_layout_to_grid_content_description
            }
            Icon(
                modifier = Modifier
                    .padding(top = ToggleLayoutPaddingTop, bottom = ToggleLayoutPaddingBottom)
                    .size(ToggleLayoutSize),
                painter = painterResource(id = icon),
                tint = ProtonTheme.colors.iconNorm,
                contentDescription = stringResource(id = contentDescription),
            )
        }
    }
}

@Preview
@Composable
fun PreviewFileListHeader() {
    ProtonTheme {
        FilesListHeader(
            sortingViewState = SortingViewState(
                icon = CorePresentation.drawable.ic_proton_arrow_down,
                title = I18N.string.title_file_type
            ),
            isDisplayingGridView = false,
            onSorting = {},
            onToggleLayout = {}
        )
    }
}

private val ToggleLayoutPaddingTop = 21.dp
private val ToggleLayoutPaddingBottom = 11.dp
private val ToggleLayoutSize = SmallIconSize
private val ListHeaderHeight = 52.dp
