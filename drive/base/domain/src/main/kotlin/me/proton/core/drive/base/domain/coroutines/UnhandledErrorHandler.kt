/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.base.domain.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import me.proton.core.drive.base.domain.extension.isFatal
import me.proton.core.util.kotlin.CoreLogger

/** Logs an unhandled error, unless it is fatal and the process must die. */
fun unhandledErrorHandler(logTag: String) = CoroutineExceptionHandler { _, error ->
    if (error.isFatal) {
        terminate(error)
    } else {
        CoreLogger.e(logTag, error, "Unhandled error in coroutine scope")
    }
}

private fun terminate(error: Throwable) {
    val handler = Thread.getDefaultUncaughtExceptionHandler() ?: throw error
    handler.uncaughtException(Thread.currentThread(), error)
}
