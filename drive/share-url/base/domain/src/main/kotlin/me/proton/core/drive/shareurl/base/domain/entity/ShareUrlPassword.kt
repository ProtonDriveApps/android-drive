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
package me.proton.core.drive.shareurl.base.domain.entity

@JvmInline
value class ShareUrlPasswordFlags(val value: Long = 0) {
    val isLegacy get() = value == 0L
    val isCustom get() = has(Flag.CUSTOM)
    val isRandom get() = has(Flag.RANDOM)

    fun add(flag: Flag) = ShareUrlPasswordFlags(value.or(1L shl flag.bitPosition))
    fun remove(flag: Flag) = ShareUrlPasswordFlags(value.and((1L shl flag.bitPosition).inv()))
    fun has(flag: Flag) = value shr flag.bitPosition and 1 == 1L

    enum class Flag(val bitPosition: Int) {
        CUSTOM(bitPosition = 0),
        RANDOM(bitPosition = 1),
    }
}
