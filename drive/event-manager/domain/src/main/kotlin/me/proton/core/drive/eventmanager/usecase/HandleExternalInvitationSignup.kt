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
package me.proton.core.drive.eventmanager.usecase

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.share.user.domain.manager.ShareUserManager
import javax.inject.Inject

class HandleExternalInvitationSignup @Inject constructor(
    private val shareUserManager: ShareUserManager,
    private val getDriveLink: GetDriveLink,
) {
    suspend operator fun invoke(vos: List<LinkEventVO>) {
        vos
            .mapNotNull { vo ->
                vo.externalInvitationSignup?.let { externalInvitationSignup ->
                    getDriveLink(vo.link.id).toResult().getOrNull()?.let { driveLink ->
                        if ((driveLink.sharePermissions ?: Permissions.owner).isAdmin) {
                            driveLink.id to externalInvitationSignup
                        } else {
                            null
                        }
                    }
                }
            }
            .forEach { (linkId, externalInvitationSignup) ->
                shareUserManager.convertExternalInvitation(linkId, externalInvitationSignup)
            }
    }
}
