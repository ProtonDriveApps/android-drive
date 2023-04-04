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
package me.proton.core.drive.base.presentation.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultIconSize
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default

@Composable
fun ProtonListItem(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    iconTitlePadding: Dp = ListItemTextStartPadding,
    iconTintColor: Color = ProtonTheme.colors.iconNorm,
) = ProtonListItem(painterResource(icon), stringResource(title), modifier, iconTitlePadding, iconTintColor)

@Composable
fun ProtonListItem(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    iconTitlePadding: Dp = ListItemTextStartPadding,
    iconTintColor: Color = ProtonTheme.colors.iconNorm,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = title
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(DefaultIconSize),
            painter = icon,
            tint = iconTintColor,
            contentDescription = null,
        )
        Text(
            text = title,
            style = ProtonTheme.typography.default,
            modifier = Modifier.padding(start = iconTitlePadding),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Suppress("unused")
@Composable
private fun PreviewListItem() {
    ProtonTheme {
        ProtonListItem(
            icon = ColorPainter(Color.Blue),
            title = "Title",
        )
    }
}

private val ListItemHeight = 48.dp
private val ListItemTextStartPadding = 12.dp
