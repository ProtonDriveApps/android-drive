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

package me.proton.core.drive.base.domain.extension

import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toTimestampMs
import kotlin.time.Duration

fun TimestampMs?.isOlderThen(duration: Duration): Boolean = this?.let { timestamp ->
    (timestamp.value + duration.inWholeMilliseconds) < System.currentTimeMillis()
} ?: true

fun TimestampS?.isOlderThen(duration: Duration): Boolean = this?.toTimestampMs().isOlderThen(duration)

val TimestampS?.orNow get() = TimestampS()
