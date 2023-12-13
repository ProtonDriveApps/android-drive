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

package me.proton.android.drive.ui.action

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.PhotosExportDataViewModel
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.drive.i18n.R

@Composable
fun PhotoExtractDataAction(
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<PhotosExportDataViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent()
    }
    if (viewState.isExportDataEnabled) {
        PhotoExtractDataAction(
            modifier = modifier,
            onExportData = viewEvent.onExportData,
            isLoading = viewState.isExportDataLoading,
        )
    }
}

@Composable
private fun PhotoExtractDataAction(
    onExportData: (Context) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    ProtonRawListItem(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = ProtonDimens.ListItemHeight)
            .clickable {
                onExportData(localContext)
            }
            .padding(horizontal = ProtonDimens.DefaultSpacing),
    ) {
        Text(
            text = stringResource(id = R.string.photos_export_data),
            style = ProtonTheme.typography.defaultNorm,
            modifier = Modifier.weight(1f),
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                strokeWidth = 1.dp,
            )
        }
    }
}

@Preview
@Composable
private fun PhotoExtractDataActionPreview() {
    ProtonTheme {
        PhotoExtractDataAction(
            onExportData = {},
            isLoading = true
        )
    }
}
