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

package me.proton.core.drive.drivelink.device.presentation.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.IllustratedMessage
import me.proton.core.drive.drivelink.device.presentation.R
import me.proton.core.drive.i18n.R as I18N

@Composable
fun DevicesEmpty(
    @DrawableRes imageResId: Int,
    @StringRes titleResId: Int,
    @StringRes descriptionResId: Int?,
    modifier: Modifier = Modifier,
) {
    IllustratedMessage(
        imageResId = imageResId,
        titleResId = titleResId,
        descriptionResId = descriptionResId,
        modifier = modifier.fillMaxSize(),
    )
}

@Preview
@Composable
fun PreviewDevicesEmpty() {
    ProtonTheme {
        DevicesEmpty(
            imageResId = R.drawable.empty_devices_daynight,
            titleResId = I18N.string.computers_empty_title,
            descriptionResId = I18N.string.computers_empty_description,
        )
    }
}
