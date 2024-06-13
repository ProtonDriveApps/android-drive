/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.drivelink.shared.presentation.entry

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

interface ShareUserOptionEntry {
    @get:DrawableRes
    val leadingIcon: Int
    @get:DrawableRes
    val trailingIcon: Int?
    @get:StringRes
    val label: Int
    val onClick: () -> Unit
}

class CopyInviteLinkEntry(
    override val onClick: () -> Unit,
) : ShareUserOptionEntry {
    override val leadingIcon: Int = CorePresentation.drawable.ic_proton_link
    override val trailingIcon: Int? = null
    override val label: Int = I18N.string.share_via_invitations_copy_invite_link_action
}

class ResendInviteLinkEntry(
    override val onClick: () -> Unit,
) : ShareUserOptionEntry {
    override val leadingIcon: Int = CorePresentation.drawable.ic_proton_paper_plane
    override val trailingIcon: Int? = null
    override val label: Int = I18N.string.share_via_invitations_resend_invite_action
}


data class PermissionsEntry(
    override val label: Int,
    override val leadingIcon: Int,
    override val trailingIcon: Int?,
    override val onClick: () -> Unit,
) : ShareUserOptionEntry {
    @Suppress("FunctionName")
    companion object {
        fun Viewer(isSelected: Boolean, onClick: () -> Unit) = PermissionsEntry(
            label = I18N.string.share_via_invitations_permission_viewer,
            leadingIcon = CorePresentation.drawable.ic_proton_eye,
            trailingIcon = CorePresentation.drawable.ic_proton_checkmark.takeIf { isSelected },
            onClick = onClick,
        )
        fun Editor(isSelected: Boolean, onClick: () -> Unit) = PermissionsEntry(
            label = I18N.string.share_via_invitations_permission_editor,
            leadingIcon = CorePresentation.drawable.ic_proton_pen,
            trailingIcon = CorePresentation.drawable.ic_proton_checkmark.takeIf { isSelected },
            onClick = onClick
        )
    }
}

class RemoveAccessEntry(
    override val onClick: () -> Unit,
) : ShareUserOptionEntry {
    override val leadingIcon: Int = CorePresentation.drawable.ic_proton_cross
    override val trailingIcon: Int? = null
    override val label: Int = I18N.string.share_via_invitations_remove_access_action
}
