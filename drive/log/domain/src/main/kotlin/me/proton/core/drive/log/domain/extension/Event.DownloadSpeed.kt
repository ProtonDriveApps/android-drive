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

package me.proton.core.drive.log.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.extension.toKibibytesPerSecond
import me.proton.core.drive.log.domain.entity.Log

internal fun Event.DownloadSpeed.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "Download speed ${runCatching { toKibibytesPerSecond() }.getOrNull()} KiB/s; " +
            "bytes(min): ${bytes.value}, " +
            "elapsed(ms): ${elapsedTime.value}",
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_UPLOAD
)
