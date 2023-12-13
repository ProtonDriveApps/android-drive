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

package me.proton.core.drive.backup.domain.entity

sealed interface BackupStatus {
    val totalBackupPhotos: Int
    data class Complete(override val totalBackupPhotos: Int) : BackupStatus
    data class Uncompleted(override val totalBackupPhotos: Int, val failedBackupPhotos: Int) : BackupStatus
    data class InProgress(override val totalBackupPhotos: Int, val pendingBackupPhotos: Int) : BackupStatus {

        init {
            require(totalBackupPhotos > 0) {
                "total should be positive"
            }
            require(pendingBackupPhotos >= 0) {
                "pending should be positive or zero"
            }
            require(pendingBackupPhotos <= totalBackupPhotos) {
                "pending should be inferior or equal to total"
            }
        }

        val progress: Float
            get() = (totalBackupPhotos.toFloat() - pendingBackupPhotos) / totalBackupPhotos
    }

    data class Failed(
        val errors: List<BackupError>,
        override val totalBackupPhotos: Int,
        val pendingBackupPhotos: Int,
    ) : BackupStatus
}
