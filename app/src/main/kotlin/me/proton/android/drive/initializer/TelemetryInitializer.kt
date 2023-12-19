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
import me.proton.android.drive.telemetry.NumberOfLocalItemsValueInterceptor
import me.proton.core.drive.telemetry.domain.event.PhotosEvent
import me.proton.core.drive.telemetry.domain.extension.plus
import me.proton.core.drive.telemetry.domain.filter.MeasurementGroupAndNamesFilter
import me.proton.core.drive.telemetry.domain.filter.MeasurementGroupsFilter
import me.proton.core.drive.telemetry.domain.interceptor.PlanDimensionInterceptor
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryManager

class TelemetryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                TelemetryInitializerEntryPoint::class.java
            )
        ) {
            driveTelemetryManager.addInterceptor(
                MeasurementGroupsFilter(PhotosEvent.group) + planDimensionInterceptor
            )
            driveTelemetryManager.addInterceptor(
                MeasurementGroupAndNamesFilter(
                    group = PhotosEvent.group,
                    "backup.stopped", "setting.enabled"
                ) + numberOfLocalItemsValueInterceptor
            )
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        LoggerInitializer::class.java,
        WorkManagerInitializer::class.java,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TelemetryInitializerEntryPoint {
        val driveTelemetryManager: DriveTelemetryManager
        val planDimensionInterceptor: PlanDimensionInterceptor
        val numberOfLocalItemsValueInterceptor: NumberOfLocalItemsValueInterceptor
    }
}
