/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.every
import io.mockk.mockk
import me.proton.core.accountrecovery.dagger.CoreAccountRecoveryFeaturesModule
import me.proton.core.accountrecovery.domain.IsAccountRecoveryEnabled
import me.proton.core.accountrecovery.domain.IsAccountRecoveryResetEnabled
import me.proton.core.notification.dagger.CoreNotificationFeaturesModule
import me.proton.core.notification.domain.usecase.IsNotificationsEnabled
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [
        CoreNotificationFeaturesModule::class,
        CoreAccountRecoveryFeaturesModule::class
    ]
)
class TestCoreFeaturesModule {
    @Singleton
    @Provides
    fun provideIsAccountRecoveryEnabled(): IsAccountRecoveryEnabled = mockk {
        every { this@mockk.invoke(any()) } returns false
    }

    @Singleton
    @Provides
    fun provideIsAccountRecoveryResetEnabled(): IsAccountRecoveryResetEnabled = mockk {
        every { this@mockk.invoke(any()) } returns false
    }

    @Singleton
    @Provides
    fun provideIsNotificationsEnabled(): IsNotificationsEnabled = mockk {
        every { this@mockk.invoke(any()) } returns false
    }
}
