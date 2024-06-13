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

package me.proton.core.drive.base.presentation.component.list

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.drive.base.presentation.component.IllustratedMessage
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait

@Composable
fun ListEmpty(
    @DrawableRes imageResId: Int,
    @StringRes titleResId: Int,
    @StringRes descriptionResId: Int?,
    @StringRes actionResId: Int?,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        IllustratedMessage(
            imageResId = imageResId,
            titleResId = titleResId,
            descriptionResId = descriptionResId,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (actionResId != null) {
                ProtonSolidButton(
                    onClick = onAction,
                    modifier = Modifier
                        .conditional(isPortrait) {
                            this
                                .padding(all = ProtonDimens.MediumSpacing)
                                .fillMaxWidth()
                        }
                        .conditional(isLandscape) {
                            this
                                .padding(all = ProtonDimens.SmallSpacing)
                                .widthIn(min = ButtonMinWidth)
                        }
                        .heightIn(min = ProtonDimens.ListItemHeight),
                ) {
                    Text(
                        text = stringResource(id = actionResId),
                        modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing)
                    )
                }
            }
        }
    }
}

private val ButtonMinWidth = 300.dp
