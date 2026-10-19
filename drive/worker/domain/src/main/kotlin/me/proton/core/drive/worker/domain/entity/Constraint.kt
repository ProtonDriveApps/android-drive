/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.worker.domain.entity

sealed interface Constraint {

    data class System(val type: Type) : Constraint {
        enum class Type {
            BATTERY_NOT_LOW,
            CHARGING,
            CONNECTIVITY,
            DEVICE_IDLE,
            STORAGE_NOT_LOW,
        }
    }

    sealed interface Runtime : Constraint {
        val isLimited: Boolean get() = true

        data object SdkQueueFull : Runtime

        data object NetworkNotAllowed : Runtime {
            override val isLimited = false
        }
    }
}
