/*
 * Copyright (c) 2026 Proton AG.
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

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.textNorm

@Composable
fun ProtonRawListItem(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
        horizontalArrangement = horizontalArrangement
    )
}

@Composable
fun ProtonSettingsHeader(
    modifier: Modifier = Modifier,
    @StringRes title: Int
) {
    ProtonSettingsHeader(
        modifier = modifier,
        title = stringResource(id = title)
    )
}

@Composable
fun ProtonSettingsHeader(
    modifier: Modifier = Modifier,
    title: String
) {
    ProtonRawListItem(
        modifier = modifier
            .height(height = ProtonDimens.ListItemHeight)
            .padding(horizontal = ProtonDimens.DefaultSpacing),
    ) {
        Text(
            modifier = Modifier.align(Alignment.Bottom),
            text = title,
            color = ProtonTheme.colors.brandNorm,
            style = ProtonTheme.typography.body1Medium
        )
    }
}

@Composable
fun ProtonSettingsItem(
    modifier: Modifier = Modifier,
    name: String,
    hint: String? = null,
    isClickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    ProtonRawListItem(
        modifier = modifier
            .clickable(isClickable, onClick = onClick)
            .padding(
                vertical = ProtonDimens.ListItemTextStartPadding,
                horizontal = ProtonDimens.DefaultSpacing
            )
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .fillMaxWidth(),
        ) {
            Text(
                text = name,
                color = ProtonTheme.colors.textNorm,
                style = ProtonTheme.typography.body1Regular
            )
            hint?.let {
                Text(
                    modifier = Modifier.padding(top = ProtonDimens.ExtraSmallSpacing),
                    text = hint,
                    color = ProtonTheme.colors.textHint,
                    style = ProtonTheme.typography.body2Regular
                )
            }
        }
    }
}

@Composable
fun ProtonSettingsToggleItem(
    modifier: Modifier = Modifier,
    name: String,
    hint: String? = null,
    value: Boolean?,
    onToggle: (Boolean) -> Unit = {}
) {
    val isSwitchChecked = value ?: false
    val isViewEnabled = value != null
    Column(
        modifier = modifier
            .toggleable(
                value = isSwitchChecked,
                enabled = isViewEnabled,
                role = Role.Switch
            ) { onToggle(!isSwitchChecked) }
            .padding(horizontal = ProtonDimens.DefaultSpacing),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ProtonRawListItem(
            modifier = Modifier.sizeIn(minHeight = ProtonDimens.ListItemHeight),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                color = ProtonTheme.colors.textNorm(isViewEnabled),
                style = ProtonTheme.typography.body1Regular
            )
            Switch(
                checked = isSwitchChecked,
                onCheckedChange = null,
                enabled = isViewEnabled
            )
        }
        hint?.let {
            Text(
                modifier = Modifier.offset(y = ToggleItemNegativeOffset),
                text = hint,
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.body2Regular
            )
        }
    }
}

private val ToggleItemNegativeOffset = (-10).dp
