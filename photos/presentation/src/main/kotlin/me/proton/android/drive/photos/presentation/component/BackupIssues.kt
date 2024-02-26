/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.photos.presentation.component

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun BackupIssues(
    medias: List<Uri>,
    onBack: () -> Unit,
    onRetryAll: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onBack() }
    Box(modifier) {
        Column {
            TopAppBar(
                navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
                onNavigationIcon = { onBack() },
                title = stringResource(id = I18N.string.backup_issues_title),
            )
            LazyVerticalGrid(
                modifier = Modifier.weight(1F),
                columns = PhotosGridCells(minSize = CellMinSize, minCount = 3),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(
                    items = medias,
                    span = { GridItemSpan(1) },
                    key = { item -> item }
                ) { item ->
                    MediaItem(
                        uri = item,
                    )
                }
            }
            Buttons(
                onRetryAll = { onRetryAll() },
                onSkip = { onSkip() },
            )
        }
    }
}

@Composable
private fun MediaItem(
    uri: Uri,
    modifier: Modifier = Modifier,
) = with(LocalDensity.current) {
    Box(
        modifier = modifier
            .aspectRatio(MediaRatio)
            .background(ProtonTheme.colors.backgroundSecondary)
    ) {

        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .scale(Scale.FILL)
                .data(uri)
                .size(
                    width = CellMinSize.toPx().toInt(),
                    height = (CellMinSize.toPx() / MediaRatio).toInt(),
                )
                .build()
        )
        Image(
            modifier = Modifier
                .fillMaxSize()
                .placeholder(
                    visible = painter.state == AsyncImagePainter.State.Loading(painter),
                    color = ProtonTheme.colors.backgroundSecondary,
                    highlight = PlaceholderHighlight.shimmer(ProtonTheme.colors.backgroundNorm)
                ),
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun Buttons(
    modifier: Modifier = Modifier,
    onRetryAll: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(vertical = ProtonDimens.SmallSpacing)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
    ) {
        val buttonModifier = Modifier
            .conditional(isPortrait) {
                fillMaxWidth()
            }
            .conditional(isLandscape) {
                widthIn(min = ButtonMinWidth)
            }
            .padding(horizontal = ProtonDimens.MediumSpacing)
            .heightIn(min = ProtonDimens.ListItemHeight)
        ProtonSolidButton(
            onClick = onRetryAll,
            modifier = buttonModifier,
        ) {
            Text(
                text = stringResource(id = I18N.string.backup_issues_retry_all_action),
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing)
            )
        }
        ProtonTextButton(
            onClick = onSkip,
            modifier = buttonModifier
        ) {
            Text(
                text = stringResource(id = I18N.string.backup_issues_skip_action),
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing)
            )
        }
    }
}

private val CellMinSize = 128.dp
private val MediaRatio = 0.75F
private val ButtonMinWidth = 300.dp

@Preview
@Composable
fun PhotosIssuesPreview() {
    ProtonTheme {
        BackupIssues(
            medias = (1..5).map { index -> "media://item$index".toUri() },
            onBack = {},
            onSkip = {},
            onRetryAll = {},
        )
    }
}
