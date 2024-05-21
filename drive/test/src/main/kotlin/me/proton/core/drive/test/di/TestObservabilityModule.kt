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

package me.proton.core.drive.test.di

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.core.drive.test.manager.TestObservabilityWorkerManager
import me.proton.core.observability.dagger.CoreObservabilityModule
import me.proton.core.observability.data.IsObservabilityEnabledImpl
import me.proton.core.observability.data.ObservabilityRepositoryImpl
import me.proton.core.observability.data.usecase.SendObservabilityEventsImpl
import me.proton.core.observability.domain.ObservabilityRepository
import me.proton.core.observability.domain.ObservabilityWorkerManager
import me.proton.core.observability.domain.usecase.IsObservabilityEnabled
import me.proton.core.observability.domain.usecase.SendObservabilityEvents

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreObservabilityModule::class]
)
public interface TestObservabilityModule {
    @Binds
    public fun bindSendObservabilityEvents(impl: SendObservabilityEventsImpl): SendObservabilityEvents

    @Binds
    public fun bindIsObservabilityEnabled(impl: IsObservabilityEnabledImpl): IsObservabilityEnabled

    @Binds
    public fun bindObservabilityRepository(impl: ObservabilityRepositoryImpl): ObservabilityRepository

    @Binds
    public fun bindObservabilityWorkerManager(impl: TestObservabilityWorkerManager): ObservabilityWorkerManager
}
