/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.base.presentation.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.base.domain.usecase.IsBackgroundRestricted
import me.proton.core.drive.base.domain.usecase.IsIgnoringBatteryOptimizations
import me.proton.core.drive.base.presentation.usecase.IsBackgroundRestrictedImpl
import me.proton.core.drive.base.presentation.usecase.IsIgnoringBatteryOptimizationsImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface BaseBindModule {

    @Binds
    @Singleton
    fun bindsIsIgnoringBatteryOptimizations(impl: IsIgnoringBatteryOptimizationsImpl): IsIgnoringBatteryOptimizations

    @Binds
    @Singleton
    fun bindsisLimitedInBackground(impl: IsBackgroundRestrictedImpl): IsBackgroundRestricted

}