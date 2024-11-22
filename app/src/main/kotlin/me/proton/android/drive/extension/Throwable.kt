/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.extension

import android.content.Context
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.domain.exception.DriveException
import me.proton.core.drive.base.data.extension.getDefaultMessage as baseGetDefaultMessage
import me.proton.core.drive.base.data.extension.log as baseLog

fun Throwable.getDefaultMessage(
    context: Context,
    useExceptionMessage: Boolean,
): String = when (this) {
    is DriveException -> getDefaultMessage(context)
    else -> baseGetDefaultMessage(context, useExceptionMessage)
}

fun Throwable.log(tag: String, message: String? = null, level: LoggerLevel? = null): Throwable = this.also {
    when (this) {
        is DriveException -> message?.let { log(tag, message, level) } ?: log(tag)
        else -> baseLog(tag, message, level)
    }
}
