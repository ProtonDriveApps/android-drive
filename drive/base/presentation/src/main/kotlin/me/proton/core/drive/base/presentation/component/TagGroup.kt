/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.base.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ChipDefaults.filterChipColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.viewstate.TagViewState
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TagGroup(
    tags: List<TagViewState>,
    modifier: Modifier = Modifier,
    onClick: (TagViewState) -> Unit,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
        contentPadding = PaddingValues(horizontal = ProtonDimens.DefaultSpacing),
    ) {
        items(tags) { tag ->
            FilterChip(
                onClick = {
                    if (!tag.selected) {
                        onClick(tag)
                    }
                },
                content = {
                    Text(tag.label)
                },
                selected = tag.selected,
                leadingIcon = {
                    Icon(
                        painter = painterResource(tag.icon),
                        contentDescription = null,
                        modifier = Modifier.size(ProtonDimens.SmallIconSize)
                    )
                },
                shape = ProtonTheme.shapes.medium,
                colors = filterChipColors(
                    backgroundColor = ProtonTheme.colors.backgroundNorm,
                    selectedBackgroundColor = ProtonTheme.colors.backgroundSecondary,
                    contentColor = ProtonTheme.colors.textWeak,
                    selectedContentColor = ProtonTheme.colors.textNorm,
                    leadingIconColor = ProtonTheme.colors.textWeak,
                    selectedLeadingIconColor = ProtonTheme.colors.textNorm,
                )
            )
        }
    }
}

@Preview
@Composable
fun TagGroupPreview() {
    ProtonTheme {
        TagGroup(
            tags = listOf(
                TagViewState(
                    label = stringResource(I18N.string.albums_filter_all),
                    icon = CorePresentation.drawable.ic_proton_checkmark,
                    selected = true,
                ),
                TagViewState(
                    label = stringResource(I18N.string.albums_filter_my_albums),
                    icon = CorePresentation.drawable.ic_proton_user,
                    selected = false,
                ),
                TagViewState(
                    label = stringResource(I18N.string.albums_filter_shared_by_me),
                    icon = CorePresentation.drawable.ic_proton_link,
                    selected = false,
                ),
                TagViewState(
                    label = stringResource(I18N.string.albums_filter_shared_with_me),
                    icon = CorePresentation.drawable.ic_proton_users,
                    selected = false,
                ),
            ),
            onClick = {}
        )
    }
}
