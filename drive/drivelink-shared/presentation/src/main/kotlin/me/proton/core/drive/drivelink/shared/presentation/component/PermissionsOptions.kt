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

package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.presentation.component.BottomSheetEntry
import me.proton.core.drive.drivelink.shared.presentation.viewstate.PermissionViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.PermissionsViewState
import me.proton.core.presentation.R as CorePresentation


@Composable
fun PermissionsOptions(
    permissionsViewState: PermissionsViewState,
    onClick: (Permissions) -> Unit,
) {
    Column(
        modifier = Modifier.navigationBarsPadding(),
    ) {
        permissionsViewState.options.forEach { option ->
            BottomSheetEntry(
                title = option.label,
                leadingIcon = option.icon,
                trailingIcon = if (option.selected) {
                    CorePresentation.drawable.ic_proton_checkmark
                } else null,
                onClick = {
                    onClick(option.permissions)
                }
            )
        }
    }
}


@Preview
@Composable
fun PermissionsOptionsPreview() {
    ProtonTheme {
        Surface {
            PermissionsOptions(
                permissionsViewState = PermissionsViewState(
                    options = listOf(
                        PermissionViewState(
                            icon = CorePresentation.drawable.ic_proton_eye,
                            label = "Viewer",
                            selected = true,
                            permissions = Permissions()
                        ),
                        PermissionViewState(
                            icon = CorePresentation.drawable.ic_proton_pen,
                            label = "Editor",
                            selected = false,
                            permissions = Permissions()
                        ),
                    )
                ),
                onClick = {},
            )
        }
    }
}
