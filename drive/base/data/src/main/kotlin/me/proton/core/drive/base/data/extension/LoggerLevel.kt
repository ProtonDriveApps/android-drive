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

package me.proton.core.drive.base.data.extension

import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.data.entity.LoggerLevel.DEBUG
import me.proton.core.drive.base.data.entity.LoggerLevel.ERROR
import me.proton.core.drive.base.data.entity.LoggerLevel.INFO
import me.proton.core.drive.base.data.entity.LoggerLevel.VERBOSE
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.util.kotlin.CoreLogger

fun LoggerLevel?.log(tag: String, error: Throwable, message: String?) {
    val log: (String, Throwable, String) -> Unit = when (this) {
        DEBUG -> CoreLogger::d
        VERBOSE -> CoreLogger::v
        INFO -> CoreLogger::i
        WARNING -> CoreLogger::w
        ERROR -> CoreLogger::e
        null -> CoreLogger::e
    }
    log(tag, error, message.orEmpty())
}
