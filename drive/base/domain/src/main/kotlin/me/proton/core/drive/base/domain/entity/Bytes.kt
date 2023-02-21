/*
 * Copyright (c) 2021-2023 Proton AG.
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

import kotlinx.serialization.Serializable
import me.proton.core.drive.base.domain.extension.bytes

@JvmInline
@Serializable
value class Bytes(val value: Long) {
    operator fun plus(other: Bytes): Bytes = Bytes(value + other.value)
    operator fun minus(other: Bytes): Bytes = Bytes(value - other.value)
    operator fun div(other: Bytes): Double = value.toDouble() / other.value
    operator fun times(other: Float) = (value * other).toLong().bytes
    operator fun times(other: Bytes) = (value * other.value).bytes
    operator fun compareTo(other: Bytes) = value.compareTo(other.value)
    operator fun compareTo(other: Int) = value.compareTo(other)
    operator fun inc() = value.inc().bytes
}

fun minOf(a: Int, b: Bytes) = minOf(a.toLong(), b.value)
