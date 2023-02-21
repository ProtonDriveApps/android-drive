/*
 * Copyright (c) 2022-2023 Proton AG.
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun EncryptedItem(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.then(
            Modifier
                .height(MEDIUM_HEIGHT)
                .fillMaxWidth()
                .background(
                    color = ProtonTheme.colors.backgroundSecondary,
                    shape = ProtonTheme.shapes.small
                )
        )
    )
}

val MEDIUM_HEIGHT = 20.dp
val LARGE_HEIGHT = 24.dp
