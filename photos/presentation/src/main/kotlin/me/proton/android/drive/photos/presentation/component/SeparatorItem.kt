/*
 * Copyright (c) 2026 Proton AG.
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.drive.base.presentation.component.CircleSelection
import me.proton.core.drive.i18n.R as I18N

@Composable
fun SeparatorItem(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.separatorPadding(),
        text = title,
        style = ProtonTheme.typography.defaultWeak,
    )
}

@Composable
fun SeparatorItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(I18N.string.content_description_select_separator, title)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .separatorPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
    ) {
        CircleSelection(isSelected = isSelected)
        Text(
            text = title,
            style = ProtonTheme.typography.defaultWeak,
        )
    }
}

private fun Modifier.separatorPadding() = padding(
    top = ProtonDimens.MediumSpacing,
    bottom = ProtonDimens.SmallSpacing,
    start = ProtonDimens.DefaultSpacing,
    end = ProtonDimens.DefaultSpacing,
)
