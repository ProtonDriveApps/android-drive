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

package me.proton.core.drive.backup.data.extension

import me.proton.core.drive.backup.data.db.entity.BackupStateCount
import me.proton.core.drive.backup.domain.entity.BackupFileState.DUPLICATED
import me.proton.core.drive.backup.domain.entity.BackupFileState.ENQUEUED
import me.proton.core.drive.backup.domain.entity.BackupFileState.FAILED
import me.proton.core.drive.backup.domain.entity.BackupFileState.IDLE
import me.proton.core.drive.backup.domain.entity.BackupFileState.POSSIBLE_DUPLICATE
import me.proton.core.drive.backup.domain.entity.BackupFileState.READY
import me.proton.core.drive.backup.domain.entity.BackupStatus

fun List<BackupStateCount>.toEntity(): BackupStatus {
    val preparing =
        filter { it.backupFileState in listOf(IDLE, POSSIBLE_DUPLICATE) }.sumOf { it.count }
    val pending = filter { it.backupFileState in listOf(READY, ENQUEUED) }.sumOf { it.count }
    val failed = filter { it.backupFileState in listOf(FAILED) }.sumOf { it.count }
    val total = filterNot { it.backupFileState in listOf(DUPLICATED) }.sumOf { it.count }
    return when {
        preparing != 0 -> BackupStatus.Preparing(
            total = total,
            preparing = preparing,
            pending = pending,
            failed = failed,
        )

        pending == 0 -> if (failed == 0) {
            BackupStatus.Complete(
                total = total,
                preparing = preparing,
                pending = pending,
                failed = failed,
            )
        } else {
            BackupStatus.Uncompleted(
                total = total,
                preparing = preparing,
                pending = pending,
                failed = failed,
            )
        }

        else -> BackupStatus.InProgress(
            total = total,
            preparing = preparing,
            pending = pending,
            failed = failed,
        )
    }
}
