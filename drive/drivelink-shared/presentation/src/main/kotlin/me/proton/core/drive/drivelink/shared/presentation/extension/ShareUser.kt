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

package me.proton.core.drive.drivelink.shared.presentation.extension

import android.content.Context
import me.proton.core.drive.base.domain.extension.firstCodePointAsStringOrNull
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.i18n.R as I18N

fun ShareUser.toViewState(appContext: Context) = ShareUserViewState(
    id = id,
    email = email,
    permissionLabel = toPermissionLabel(appContext),
    firstLetter = firstLetter(),
    displayName = displayName,
    isInvitation = this !is ShareUser.Member,
)

private fun ShareUser.firstLetter() =
    (displayName ?: email).firstCodePointAsStringOrNull?.uppercase() ?: "?"

private fun ShareUser.toPermissionLabel(appContext: Context) = appContext.getString(
    when {
        permissions.isOwner -> I18N.string.share_via_invitations_permission_owner
        permissions.isAdmin -> I18N.string.share_via_invitations_permission_admin
        permissions.canWrite -> I18N.string.share_via_invitations_permission_editor
        permissions.canRead -> I18N.string.share_via_invitations_permission_viewer
        else -> I18N.string.share_via_invitations_permission_unknown
    }
)
