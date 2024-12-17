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

package me.proton.core.drive.entitlement.domain.entity

sealed class Entitlement<T>(val key: Key<T>) {
    abstract val value: T

    data class PublicCollaboration(override val value: Boolean) : Entitlement<Boolean>(Key.PublicCollaboration)
    data class MaxRevisionCount(override val value: Long) : Entitlement<Long>(Key.MaxRevisionCount)

    sealed class Key<T>(val name: String) {
        data object PublicCollaboration : Key<Boolean>("PublicCollaboration")
        data object MaxRevisionCount : Key<Long>("MaxRevisionCount")
    }
}
