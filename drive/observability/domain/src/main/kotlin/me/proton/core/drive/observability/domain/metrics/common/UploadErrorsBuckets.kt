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

package me.proton.core.drive.observability.domain.metrics.common

object UploadErrorsBuckets {

    fun getBucketValue(value: Long): Long = when (value) {
        in 0..1024 -> 1024
        in 1025..1048576 -> 1048576
        in 1048577..4194304 -> 4194304
        in 4194305..33554432 -> 33554432
        in 33554433..67108864 -> 67108864
        in 67108865..134217728 -> 134217728
        in 134217729..268435456 -> 268435456
        in 268435457..536870912 -> 536870912
        in 536870913..1073741824 -> 1073741824
        in 1073741825..2147483648 -> 2147483648
        in 2147483649..4294967296 -> 4294967296
        in 4294967297..8589934592 -> 8589934592
        in 8589934593..Long.MAX_VALUE -> 17179869184
        else -> error("Invalid value: $value")
    }
}
