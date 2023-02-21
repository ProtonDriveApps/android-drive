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

package me.proton.android.drive.initializer

import android.content.Context
import androidx.startup.Initializer
import me.proton.android.drive.log.DriveLogTag
import me.proton.core.util.kotlin.CoreLogger

@Suppress("unused")
class UncaughtExceptionHandlerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Thread.setDefaultUncaughtExceptionHandler(
            UncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler())
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(LoggerInitializer::class.java)

    private class UncaughtExceptionHandler(
        val defaultHandler: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {

        override fun uncaughtException(thread: Thread, error: Throwable) {
            CoreLogger.e(DriveLogTag.CRASH, error)
            defaultHandler?.uncaughtException(thread, error)
        }
    }
}
