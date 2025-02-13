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

package me.proton.core.drive.drivelink.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.contact.domain.usecase.GetContactEmails
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import javax.inject.Inject

class UpdateShareUserDisplayName @Inject constructor(
    private val getContactEmails: GetContactEmails,
) {
    suspend operator fun invoke(driveLink: DriveLink): DriveLink = when (driveLink) {
        is DriveLink.Folder -> {
            driveLink.copy(
                shareUser = driveLink.shareUser.updateDisplayName(driveLink.userId)
            )
        }
        is DriveLink.File -> {
            driveLink.copy(
                shareUser = driveLink.shareUser.updateDisplayName(driveLink.userId)
            )
        }
        is DriveLink.Album -> {
            driveLink.copy(
                shareUser = driveLink.shareUser.updateDisplayName(driveLink.userId)
            )
        }
    }

    private suspend fun ShareUser?.updateDisplayName(userId: UserId): ShareUser? = when (this) {
        is ShareUser.Member -> copy(
            displayName = getContactEmails(
                userId = userId,
                email = inviter,
            ).getOrNull()?.name ?: inviter
        )
        else -> this
    }
}
