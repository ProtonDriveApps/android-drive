/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.ui.options

import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.drive.drivelink.shared.presentation.entry.PermissionsEntry

sealed interface MemberOption {
    data object PermissionsViewer : MemberOption {
        fun build(
            isSelected: Boolean,
            runAction: RunAction,
            onSelect: () -> Unit,
        ) = PermissionsEntry.Viewer(isSelected){
            runAction { onSelect() }
        }
    }

    data object PermissionsEditor : MemberOption {
        fun build(
            isSelected: Boolean,
            runAction: RunAction,
            onSelect: () -> Unit,
        ) = PermissionsEntry.Editor(isSelected) {
            runAction { onSelect() }
        }
    }
}
