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
import me.proton.core.drive.base.domain.exception.DriveException
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.share.domain.exception.ShareException
import me.proton.core.util.kotlin.CoreLogger

fun DriveException.getDefaultMessage(context: Context): String = context.getString(
    when (this) {
        is ShareException.MainShareLocked -> BasePresentation.string.error_main_share_locked
        is ShareException.MainShareNotFound -> BasePresentation.string.error_main_share_not_found
        else -> throw IllegalStateException("Default message for exception is missing")
    }
)

fun DriveException.log(tag: String, message: String = this.message.orEmpty()): DriveException = also {
    CoreLogger.d(tag, this, message)
}
