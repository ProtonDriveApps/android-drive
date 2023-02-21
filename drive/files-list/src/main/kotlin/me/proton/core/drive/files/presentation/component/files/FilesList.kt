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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.paging.compose.items
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.component.ErrorPadding
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonErrorMessageWithAction
import me.proton.core.compose.component.ProtonSecondaryButton
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.caption
import me.proton.core.compose.theme.defaultSmall
import me.proton.core.compose.theme.headlineSmall
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.component.LazyColumnItems
import me.proton.core.util.kotlin.exhaustive
import kotlin.math.ceil

@Composable
fun FilesListLoading(modifier: Modifier = Modifier) {
    DeferredCircularProgressIndicator(modifier)
}

fun LazyListScope.FilesListLoading(modifier: Modifier = Modifier) {
    item {
        me.proton.core.drive.files.presentation.component.files.FilesListLoading(modifier.fillParentMaxSize())
    }
}

@Composable
fun FilesListEmpty(
    @DrawableRes imageResId: Int,
    @StringRes titleResId: Int,
    @StringRes descriptionResId: Int,
    @StringRes actionResId: Int,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = imageResId), contentDescription = null)

        Text(
            text = stringResource(id = titleResId),
            style = ProtonTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = MediumSpacing)
        )
        if (descriptionResId != 0) {
            Text(
                text = stringResource(id = descriptionResId),
                style = ProtonTheme.typography.defaultSmall.copy(textAlign = TextAlign.Center),
                modifier = Modifier.padding(
                    top = SmallSpacing,
                    start = DefaultSpacing,
                    end = DefaultSpacing,
                )
            )
        }
        if (actionResId != 0) {
            ProtonSecondaryButton(
                onClick = onAction,
                modifier = Modifier.padding(top = MediumSpacing)
            ) {
                Text(
                    text = stringResource(id = actionResId),
                    style = ProtonTheme.typography.caption,
                    modifier = Modifier.padding(horizontal = DefaultSpacing)
                )
            }
        }
    }
}

fun LazyListScope.FilesListEmpty(
    @DrawableRes imageResId: Int,
    @StringRes titleResId: Int,
    @StringRes descriptionResId: Int,
    @StringRes actionResId: Int,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    item {
        me.proton.core.drive.files.presentation.component.files.FilesListEmpty(
            imageResId = imageResId,
            titleResId = titleResId,
            descriptionResId = descriptionResId,
            actionResId = actionResId,
            onAction = onAction,
            modifier = modifier.fillParentMaxSize()
        )
    }
}

@Composable
fun FilesListError(
    message: String,
    @StringRes actionResId: Int?,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(ErrorPadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (actionResId != null) {
            ProtonErrorMessageWithAction(
                errorMessage = message,
                action = stringResource(id = actionResId),
                onAction = onAction
            )
        } else {
            ProtonErrorMessage(errorMessage = message)
        }
    }
}

fun LazyListScope.FilesListError(
    message: String,
    @StringRes actionResId: Int?,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    item {
        me.proton.core.drive.files.presentation.component.files.FilesListError(
            message = message,
            actionResId = actionResId,
            modifier = modifier.fillParentMaxSize(),
            onAction = onAction
        )
    }
}

fun LazyListScope.FilesListContent(
    driveLinks: LazyColumnItems,
    onItemsIndexed: @Composable LazyItemScope.(DriveLink) -> Unit,
) {
    when (driveLinks) {
        is LazyColumnItems.PagingItems -> items(
            items = driveLinks.value,
            key = { driveLink -> driveLink.id.id },
        ) { driveLink ->
            driveLink?.let { onItemsIndexed(driveLink) }
        }
        is LazyColumnItems.ListItems -> items(
            items = driveLinks.value,
            key = { driveLink -> driveLink.id.id },
        ) { driveLink ->
            onItemsIndexed(driveLink)
        }
    }.exhaustive
}

fun LazyListScope.FilesGridContent(
    driveLinks: LazyColumnItems,
    itemSize: Dp,
    itemsPerRow: Int,
    onItemsIndexed: @Composable (DriveLink) -> Unit,
) {
    require(itemsPerRow > 0) { "itemsPerRow must be > 0, value passed $itemsPerRow" }
    items(ceil(driveLinks.size.toFloat() / itemsPerRow).toInt()) { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            repeat(itemsPerRow) { repeat ->
                val driveLink = driveLinks[row * itemsPerRow + repeat]
                if (driveLink != null) {
                    onItemsIndexed(driveLink)
                } else {
                    Spacer(modifier = Modifier.width(itemSize))
                }
            }
        }
    }
}

