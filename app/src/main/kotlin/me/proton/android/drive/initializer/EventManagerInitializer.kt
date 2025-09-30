/*
 * Copyright (c) 2021-2024 Proton AG.
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
import me.proton.core.drive.eventmanager.LinkEventListener
import me.proton.core.drive.eventmanager.presentation.DriveEventManager

@Suppress("unused")
class EventManagerInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with (
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                EventManagerInitializerEntryPoint::class.java
            )
        ) {
            val driveEventManager = manager()
            linkEventListener.setStopEventLoop { config ->
                driveEventManager.stop(config)
            }
            driveEventManager.start()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>> = listOf(
        LoggerInitializer::class.java,
        WorkManagerInitializer::class.java
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EventManagerInitializerEntryPoint {
        fun manager(): DriveEventManager
        val linkEventListener: LinkEventListener
    }
}
