/*
 * Copyright (c) 2021-2024 Proton AG.
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

package me.proton.core.drive.base.domain.entity

@JvmInline
value class Permissions(val value: Long = 0L) {
    val isAdmin get() = has(Permission.ADMIN)
    val canRead get() = has(Permission.READ)
    val canWrite get() = has(Permission.WRITE)
    val canExecute get() = has(Permission.EXECUTE)

    fun add(permission: Permission) = Permissions(value.or(1L shl permission.bitPosition))
    fun remove(permission: Permission) = Permissions(value.and((1L shl permission.bitPosition).inv()))
    fun has(permission: Permission) = value shr permission.bitPosition and 1 == 1L

    enum class Permission(val bitPosition: Int) {
        EXECUTE(bitPosition = 0),
        WRITE(bitPosition = 1),
        READ(bitPosition = 2),
        ADMIN(bitPosition = 4),
    }
}
