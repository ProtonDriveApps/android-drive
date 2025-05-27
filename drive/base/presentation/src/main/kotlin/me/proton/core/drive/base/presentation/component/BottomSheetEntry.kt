/*
 * Copyright (c) 2024 Proton AG.
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun BottomSheetEntry(
    @DrawableRes leadingIcon: Int,
    @DrawableRes trailingIcon: Int?,
    title: String,
    modifier: Modifier = Modifier,
    trailingIconTintColor: Color? = null,
    notificationDotVisible: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ProtonDimens.SmallSpacing)
            .clickable(onClick = onClick)
            .height(ItemHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxWithNotificationDot(
            notificationDotVisible = notificationDotVisible,
            modifier = Modifier.padding(start = ProtonDimens.DefaultSpacing, end = ProtonDimens.MediumSpacing),
        ) { modifier ->
            Icon(
                painter = painterResource(id = leadingIcon),
                contentDescription = null,
                modifier = modifier,
            )
        }
        Text(
            text = title,
            style = ProtonTheme.typography.defaultNorm,
            modifier = Modifier
                .weight(1F)
                .padding(end = ProtonDimens.DefaultSpacing),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) {
            Icon(
                painter = painterResource(id = trailingIcon),
                tint = trailingIconTintColor ?: ProtonTheme.colors.iconNorm,
                contentDescription = null,
                modifier = Modifier.padding(
                    start = ProtonDimens.DefaultSpacing,
                    end = ProtonDimens.MediumSpacing
                )
            )
        }
    }
}

private val ItemHeight = ProtonDimens.DefaultButtonMinHeight
