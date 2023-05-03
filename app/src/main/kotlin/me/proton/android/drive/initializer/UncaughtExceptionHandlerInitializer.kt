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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.usecase.HandleUncaughtException
import me.proton.core.util.kotlin.CoreLogger

@Suppress("unused")
class UncaughtExceptionHandlerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Thread.setDefaultUncaughtExceptionHandler(
            UncaughtExceptionHandler(
                Thread.getDefaultUncaughtExceptionHandler(),
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    UncaughtExceptionHandlerInitializerEntryPoint::class.java,
                ).handleUncaughtException
            )
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(LoggerInitializer::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UncaughtExceptionHandlerInitializerEntryPoint {
        val handleUncaughtException: HandleUncaughtException
    }

    private class UncaughtExceptionHandler(
        val defaultHandler: Thread.UncaughtExceptionHandler?,
        val handleUncaughtException: HandleUncaughtException,
    ) : Thread.UncaughtExceptionHandler {

        override fun uncaughtException(thread: Thread, error: Throwable) {
            val isExceptionHandled = handleUncaughtException(error).getOrNull() ?: false
            if (!isExceptionHandled) {
                CoreLogger.e(DriveLogTag.CRASH, error)
                defaultHandler?.uncaughtException(thread, error)
            }
        }
    }
}
