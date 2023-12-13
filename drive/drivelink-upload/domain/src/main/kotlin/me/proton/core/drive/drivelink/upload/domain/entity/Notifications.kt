/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.drivelink.upload.domain.entity

data class Notifications(
    val inApp: InAppNotifications,
    val system: SystemNotifications,
) {
    companion object {
        val TurnedOff = Notifications(
            inApp = InAppNotifications.TurnedOff,
            system = SystemNotifications.TurnedOff,
        )

        val TurnedOn = Notifications(
            inApp = InAppNotifications.TurnedOn,
            system = SystemNotifications.TurnedOn,
        )

        val TurnedOnExceptPreparingUpload = Notifications(
            inApp = InAppNotifications(
                showPreparingUpload = false,
                showFilesBeingUploaded = true,
            ),
            system = SystemNotifications.TurnedOn,
        )
    }
}

data class InAppNotifications(
    val showPreparingUpload: Boolean,
    val showFilesBeingUploaded: Boolean,
) {
    companion object {
        val TurnedOff = InAppNotifications(
            showPreparingUpload = false,
            showFilesBeingUploaded = false,
        )

        val TurnedOn = InAppNotifications(
            showPreparingUpload = true,
            showFilesBeingUploaded = true,
        )
    }
}

data class SystemNotifications(
    val announceUpload: Boolean = true,
) {
    companion object {
        val TurnedOff = SystemNotifications(
            announceUpload = false,
        )

        val TurnedOn = SystemNotifications(
            announceUpload = true,
        )
    }
}
