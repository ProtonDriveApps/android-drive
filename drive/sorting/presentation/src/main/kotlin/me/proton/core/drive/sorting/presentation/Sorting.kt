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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonDimens.SmallIconSize
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.drive.sorting.presentation.state.SortingViewState
import me.proton.core.drive.sorting.presentation.state.toSortingViewState
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun Sorting(
    viewState: SortingViewState,
    modifier: Modifier = Modifier,
    onSorting: () -> Unit,
) {
    Box(modifier = modifier.padding(horizontal = SortingPadding)) {
        Row(
            modifier = Modifier
                .clickable { onSorting() }
                .padding(SortingPadding),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = stringResource(id = viewState.title),
                style = ProtonTheme.typography.defaultSmallWeak,
            )
            Icon(
                modifier = Modifier
                    .padding(start = SortingPadding)
                    .size(SmallIconSize),
                painter = painterResource(id = viewState.icon),
                contentDescription = null,
                tint = ProtonTheme.colors.iconWeak
            )
        }
    }
}

@Preview(
    name = "Sorting in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Sorting in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
@Suppress("unused")
private fun PreviewSorting() {
    ProtonTheme {
        Sorting(
            viewState = me.proton.core.drive.sorting.domain.entity.Sorting.DEFAULT.toSortingViewState(),
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
        ) {

        }
    }
}

@Preview
@Composable
fun PreviewFileListHeader() {
    ProtonTheme {
        Surface {
            Sorting(
                viewState = SortingViewState(
                    icon = CorePresentation.drawable.ic_proton_arrow_down,
                    title = I18N.string.title_file_type
                ),
            ) {}
        }
    }
}

private val SortingPadding = SmallSpacing
