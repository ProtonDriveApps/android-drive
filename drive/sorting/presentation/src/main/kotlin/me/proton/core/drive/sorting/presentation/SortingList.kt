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

package me.proton.core.drive.sorting.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.drive.sorting.presentation.entity.SortingOption
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@Composable
fun SortingList(
    sortingOptions: List<SortingOption>,
    modifier: Modifier = Modifier,
    onSortingOption: (SortingOption) -> Unit,
) {
    Column(modifier) {
        sortingOptions.forEach { sortingOption ->
            SortingListItem(sortingOption, onSortingOption)
        }
    }
}

@Composable
fun SortingListItem(
    sortingOption: SortingOption,
    onSortingOption: (SortingOption) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SortingListItemHeight)
            .clickable { onSortingOption(sortingOption) }
            .padding(horizontal = DefaultSpacing),
        contentAlignment = Alignment.CenterStart
    ) {
        if (sortingOption.isApplied) {
            Icon(
                painter = painterResource(id = sortingOption.icon),
                contentDescription = null,
                tint = ProtonTheme.colors.iconNorm,
            )
        }
        Text(
            text = stringResource(id = sortingOption.title),
            style = ProtonTheme.typography.default,
            modifier = Modifier.padding(start = SortingListItemTextStartPadding)
        )
    }
}

@Preview(
    name = "SortingList in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "SortingList in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
@Suppress("unused")
private fun PreviewSortingList() {
    ProtonTheme {
        SortingList(
            listOf(
                SortingOption(
                    icon = CorePresentation.drawable.ic_proton_arrow_up,
                    title = BasePresentation.string.title_app,
                    isApplied = true,
                    toggleDirection = me.proton.core.drive.sorting.domain.entity.Sorting.DEFAULT,
                ),
                SortingOption(
                    icon = CorePresentation.drawable.ic_proton_arrow_down,
                    title = BasePresentation.string.title_empty_files,
                    isApplied = true,
                    toggleDirection = me.proton.core.drive.sorting.domain.entity.Sorting.DEFAULT,
                ),
            ),
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
        ) {

        }
    }
}

private val SortingListItemHeight = 56.dp
private val SortingListItemTextStartPadding = 52.dp - DefaultSpacing
