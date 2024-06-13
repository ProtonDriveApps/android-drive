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

import me.proton.core.drive.base.domain.entity.Permissions.Permission.ADMIN
import me.proton.core.drive.base.domain.entity.Permissions.Permission.EXECUTE
import me.proton.core.drive.base.domain.entity.Permissions.Permission.READ
import me.proton.core.drive.base.domain.entity.Permissions.Permission.SUPER_ADMIN
import me.proton.core.drive.base.domain.entity.Permissions.Permission.WRITE

@JvmInline
value class Permissions(val value: Long = 0L) {
    val isOwner get() = has(SUPER_ADMIN)
    val isAdmin get() = has(ADMIN)
    val canRead get() = has(READ)
    val canWrite get() = has(WRITE)
    val canExecute get() = has(EXECUTE)

    fun add(permission: Permission) = Permissions(value.or(1L shl permission.bitPosition))

    fun remove(permission: Permission) =
        Permissions(value.and((1L shl permission.bitPosition).inv()))

    fun has(permission: Permission) = value shr permission.bitPosition and 1 == 1L

    enum class Permission(val bitPosition: Int) {
        EXECUTE(bitPosition = 0),
        WRITE(bitPosition = 1),
        READ(bitPosition = 2),
        ADMIN(bitPosition = 4),
        SUPER_ADMIN(bitPosition = 5),
    }

    companion object {
        val viewer = Permissions().add(READ)
        val editor = Permissions().add(WRITE).add(READ)
        val admin = Permissions().add(WRITE).add(READ).add(ADMIN)
        val owner = Permissions().add(EXECUTE).add(WRITE).add(READ).add(ADMIN).add(SUPER_ADMIN)
    }
}
