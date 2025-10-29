/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.backup.domain.extension

import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.backup.domain.entity.BackupStatus

fun BackupStatus.toState(): Event.Backup.BackupState = when (this) {
    is BackupStatus.Complete -> Event.Backup.BackupState.COMPLETE
    is BackupStatus.Failed -> Event.Backup.BackupState.FAILED
    is BackupStatus.InProgress -> Event.Backup.BackupState.IN_PROGRESS
    is BackupStatus.Preparing -> Event.Backup.BackupState.PREPARING
    is BackupStatus.Uncompleted -> Event.Backup.BackupState.UNCOMPLETED
}
