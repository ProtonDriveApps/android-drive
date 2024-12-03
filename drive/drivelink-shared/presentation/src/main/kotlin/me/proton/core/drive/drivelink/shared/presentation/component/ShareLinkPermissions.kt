/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.BottomSheetEntry
import me.proton.core.drive.drivelink.shared.presentation.entry.PermissionsEntry
import me.proton.core.drive.drivelink.shared.presentation.entry.ShareUserOptionEntry

@Composable
fun ShareLinkPermissionsOptions(
    entries: List<ShareUserOptionEntry>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        entries.forEach { entry ->
            BottomSheetEntry(
                leadingIcon = entry.leadingIcon,
                trailingIcon = entry.trailingIcon,
                title = stringResource(entry.label),
                onClick = { entry.onClick() }
            )
        }
    }
}


@Preview
@Composable
fun ShareLinkPermissionsOptionsPreview() {
    ProtonTheme {
        Surface {
            ShareLinkPermissionsOptions(
                entries = listOf(
                    PermissionsEntry.Viewer(isSelected = true) {},
                    PermissionsEntry.Editor(isSelected = false) {},
                )
            )
        }
    }
}
