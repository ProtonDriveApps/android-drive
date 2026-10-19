/*
 * Copyright (c) 2025 Proton AG.
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.android.drive.photos.presentation.viewstate.EnableBackupForBucketsViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallStrongNorm
import me.proton.core.drive.base.presentation.component.BottomSheetHeader
import me.proton.core.drive.base.presentation.component.ProtonSolidButton
import me.proton.core.drive.base.presentation.component.ProtonTextButton
import me.proton.core.drive.base.presentation.theme.DriveTheme
import me.proton.core.drive.i18n.R as I18N

@Composable
fun EnableBackupForBucketsDialogContent(
    viewState: EnableBackupForBucketsViewState,
    onDismiss: () -> Unit,
    onToggleBucket: (Int, Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
    ) {
        BottomSheetHeader(content = {
            Text(
                text = viewState.title,
                style = ProtonTheme.typography.defaultSmallStrongNorm,
            )
        })
        HorizontalDivider(
            modifier = Modifier.padding(bottom = ProtonDimens.SmallSpacing),
            color = ProtonTheme.colors.separatorNorm,
        )
        Text(
            text = viewState.description,
            modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
            style = ProtonTheme.typography.defaultNorm,
        )
        val itemModifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = ProtonDimens.ListItemHeight)
            .padding(
                vertical = ProtonDimens.SmallSpacing,
                horizontal = ProtonDimens.DefaultSpacing,
            )
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
        ) {
            items(viewState.buckets) { bucket ->
                BucketItemRow(
                    modifier = itemModifier,
                    bucket = bucket,
                    onToggle = onToggleBucket,
                )
            }
        }
        Spacer(modifier = Modifier.height(ProtonDimens.DefaultSpacing))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.DefaultSpacing),
            horizontalArrangement = Arrangement.End,
        ) {
            ProtonTextButton(
                onClick = onDismiss,
            ) {
                Text(text = stringResource(I18N.string.common_cancel_action))
            }
            ProtonSolidButton(
                onClick = onSave,
                enabled = viewState.hasSelection,
                modifier = Modifier.padding(start = ProtonDimens.SmallSpacing),
            ) {
                Text(text = stringResource(I18N.string.common_save_action))
            }
        }
    }
}

@Composable
private fun BucketItemRow(
    bucket: EnableBackupForBucketsViewState.BucketItem,
    onToggle: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryFolderItem(
        name = bucket.name,
        description = {
            Text(
                text = bucket.description,
                style = ProtonTheme.typography.captionWeak,
            )
        },
        checked = bucket.isSelected,
        uri = bucket.uri,
        onToggle = { isSelected -> onToggle(bucket.id, isSelected) },
        modifier = modifier,
        role = Role.Checkbox,
        toggleControl = { checked, enabled ->
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
            )
        },
    )
}

private val ThumbnailSize = 40.dp

@Preview
@Composable
private fun EnableBackupForBucketsDialogContentPreview() {
    DriveTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            EnableBackupForBucketsDialogContent(
                viewState = EnableBackupForBucketsViewState(
                    title = stringResource(I18N.string.photos_folder_selection_title),
                    description = stringResource(I18N.string.photos_folder_selection_description),
                    buckets = listOf(
                        EnableBackupForBucketsViewState.BucketItem(
                            id = 1,
                            name = "Camera",
                            description = "150 photos, 20 videos",
                            uri = null,
                            isSelected = true,
                        ),
                        EnableBackupForBucketsViewState.BucketItem(
                            id = 2,
                            name = "Screenshots",
                            description = "45 photos",
                            uri = null,
                            isSelected = false,
                        ),
                        EnableBackupForBucketsViewState.BucketItem(
                            id = 3,
                            name = "Photos",
                            description = "200 photos, 10 videos",
                            uri = null,
                            isSelected = true,
                        ),
                        EnableBackupForBucketsViewState.BucketItem(
                            id = 4,
                            name = "Downloads",
                            description = "5 photos",
                            uri = null,
                            isSelected = false,
                        ),
                    ),
                    hasSelection = true,
                    isLoading = false,
                ),
                onDismiss = {},
                onToggleBucket = { _, _ -> },
                onSave = {},
            )
        }
    }
}
