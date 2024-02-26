package me.proton.android.drive.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.every
import io.mockk.mockk
import me.proton.core.accountrecovery.dagger.CoreAccountRecoveryFeaturesModule
import me.proton.core.accountrecovery.domain.IsAccountRecoveryEnabled
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
    fun provideIsNotificationsEnabled(): IsNotificationsEnabled = mockk {
        every { this@mockk.invoke(any()) } returns false
    }
}
