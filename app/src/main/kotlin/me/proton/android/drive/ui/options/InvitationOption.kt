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
import me.proton.core.drive.drivelink.shared.presentation.entry.CopyInviteLinkEntry
import me.proton.core.drive.drivelink.shared.presentation.entry.PermissionsEntry
import me.proton.core.drive.drivelink.shared.presentation.entry.RemoveAccessEntry
import me.proton.core.drive.drivelink.shared.presentation.entry.ResendInviteLinkEntry

sealed interface InvitationOption {

    data object PermissionsViewer : InvitationOption {
        fun build(
            isSelected: Boolean,
            runAction: RunAction,
            onSelect: () -> Unit,
        ) = PermissionsEntry.Viewer(isSelected){
            runAction { onSelect() }
        }
    }

    data object PermissionsEditor : InvitationOption {
        fun build(
            isSelected: Boolean,
            runAction: RunAction,
            onSelect: () -> Unit,
        ) = PermissionsEntry.Editor(isSelected) {
            runAction { onSelect() }
        }
    }

    data object CopyInvitationLink : InvitationOption {
        fun build(
            runAction: RunAction,
            copyInvitationLinkToClipboard: () -> Unit,
        ) = CopyInviteLinkEntry {
            runAction { copyInvitationLinkToClipboard() }
        }
    }

    data object ResendInvitation : InvitationOption {
        fun build(
            runAction: RunAction,
            onClick: () -> Unit,
        ) = ResendInviteLinkEntry {
            runAction { onClick() }
        }
    }

    data object RemoveAccess : InvitationOption {
        fun build(
            runAction: RunAction,
            onClick: () -> Unit,
        ) = RemoveAccessEntry {
            runAction { onClick() }
        }
    }
}
