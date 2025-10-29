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
    val preparing: Int
    val pending: Int
    val failed: Int
    val total: Int

    data class Complete(
        override val total: Int,
        override val preparing: Int = 0,
        override val pending: Int = 0,
        override val failed: Int = 0,
    ) : BackupStatus

    data class Uncompleted(
        override val total: Int,
        override val failed: Int,
        override val preparing: Int = 0,
        override val pending: Int = 0,
    ) : BackupStatus

    data class InProgress(
        override val total: Int,
        override val pending: Int,
        override val preparing: Int = 0,
        override val failed: Int = 0,
    ) : BackupStatus {

        init {
            require(total > 0) {
                "total should be positive"
            }
            require(pending >= 0) {
                "pending should be positive or zero"
            }
            require(pending <= total) {
                "pending should be inferior or equal to total"
            }
        }

        val progress: Float
            get() = (total.toFloat() - pending) / total
    }

    data class Preparing(
        override val total: Int,
        override val preparing: Int,
        override val pending: Int = 0,
        override val failed: Int = 0,
    ) : BackupStatus {

        init {
            require(total > 0) {
                "total should be positive"
            }
            require(preparing >= 0) {
                "preparing should be positive or zero"
            }
            require(preparing <= total) {
                "preparing should be inferior or equal to total"
            }
        }

        val progress: Float
            get() = (total.toFloat() - preparing) / total
    }

    data class Failed(
        val errors: List<BackupError>,
        override val total: Int,
        override val failed: Int,
        override val preparing: Int = 0,
        override val pending: Int = 0,
    ) : BackupStatus
}
