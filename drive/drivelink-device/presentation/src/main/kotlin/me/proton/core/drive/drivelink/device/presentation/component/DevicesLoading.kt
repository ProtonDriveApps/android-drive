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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun DevicesLoading(
    modifier: Modifier = Modifier,
) {
    DeferredCircularProgressIndicator(modifier)
}

@Preview
@Composable
private fun PreviewDevicesLoading() {
    ProtonTheme {
        Surface {
            DevicesLoading(Modifier.fillMaxSize())
        }
    }
}
