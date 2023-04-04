/*
 * Copyright (c) 2023 Proton AG.
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation


@Composable
fun CircleSelection(isSelected: Boolean) {
    if (isSelected) {
        Image(
            modifier = Modifier.size(ProtonDimens.DefaultIconSize),
            painter = painterResource(id = BasePresentation.drawable.ic_checkmark_circle_filled),
            contentDescription = null
        )
    } else {
        Icon(
            modifier = Modifier.size(ProtonDimens.DefaultIconSize),
            painter = painterResource(id = CorePresentation.drawable.ic_proton_circle),
            tint = ProtonTheme.colors.iconWeak,
            contentDescription = null
        )
    }
}