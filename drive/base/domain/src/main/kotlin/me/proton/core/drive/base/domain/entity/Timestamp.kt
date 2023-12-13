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
import java.util.concurrent.TimeUnit

sealed interface Timestamp : Comparable<Timestamp> {
    companion object {
        val now get() = TimestampMs()
    }
}

@JvmInline
@Serializable
value class TimestampS(
    val value: Long = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
) : Timestamp {
    override operator fun compareTo(other: Timestamp): Int {
        val otherValue = when (other) {
            is TimestampMs -> other.toTimestampS().value
            is TimestampS -> other.value
        }
        return value.compareTo(otherValue)
    }
}

fun TimestampS.toTimestampMs() = TimestampMs(TimeUnit.SECONDS.toMillis(value))


@JvmInline
@Serializable
value class TimestampMs(val value: Long = System.currentTimeMillis()) : Timestamp {
    override operator fun compareTo(other: Timestamp): Int {
        val otherValue = when (other) {
            is TimestampMs -> other.value
            is TimestampS -> other.toTimestampMs().value
        }
        return value.compareTo(otherValue)
    }
}

fun TimestampMs.toTimestampS() = TimestampS(TimeUnit.MILLISECONDS.toSeconds(value))
