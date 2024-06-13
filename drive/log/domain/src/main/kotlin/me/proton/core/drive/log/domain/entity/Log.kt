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

package me.proton.core.drive.log.domain.entity

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs

data class Log(
    val id: Long = 0,
    val userId: UserId,
    val creationTime: TimestampMs,
    val message: String,
    val moreContent: String? = null,
    val level: Level = Level.NORMAL,
    val origin: Origin,
) {
    enum class Level {
        NORMAL,
        WARNING,
        ERROR,
    }

    enum class Origin {
        EVENT_DOWNLOAD,
        EVENT_UPLOAD,
        EVENT_THROWABLE,
        EVENT_NETWORK,
        EVENT_LOGGER,
    }
}
