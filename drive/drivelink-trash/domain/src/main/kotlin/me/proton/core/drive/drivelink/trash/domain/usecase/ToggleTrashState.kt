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

package me.proton.core.drive.drivelink.trash.domain.usecase

import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.trash.domain.usecase.RestoreFromTrash
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import javax.inject.Inject

class ToggleTrashState @Inject constructor(
    private val sendToTrash: SendToTrash,
    private val restoreFromTrash: RestoreFromTrash,
) {

    suspend operator fun invoke(driveLink: DriveLink) = if (driveLink.isTrashed) {
        restoreFromTrash(driveLink.userId, driveLink.volumeId, listOf(driveLink.id))
    } else {
        sendToTrash(driveLink.userId, driveLink.volumeId, listOf(driveLink.id))
    }
}
