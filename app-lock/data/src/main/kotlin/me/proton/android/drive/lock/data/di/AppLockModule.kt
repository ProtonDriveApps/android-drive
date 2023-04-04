/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.android.drive.lock.data.di

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.work.WorkManager
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.android.drive.lock.data.db.AppLockDatabase
import me.proton.android.drive.lock.data.lock.CryptoSystemLock
import me.proton.android.drive.lock.data.lock.SystemLock
import me.proton.android.drive.lock.data.manager.AppLockManagerImpl
import me.proton.android.drive.lock.data.manager.AutoLockManagerImpl
import me.proton.android.drive.lock.data.provider.BiometricPromptProvider
import me.proton.android.drive.lock.data.provider.BiometricPromptProviderImpl
import me.proton.android.drive.lock.data.repository.AppLockRepositoryImpl
import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.android.drive.lock.domain.lock.Lock
import me.proton.android.drive.lock.domain.manager.AppLockManager
import me.proton.android.drive.lock.domain.manager.AutoLockManager
import me.proton.android.drive.lock.domain.repository.AppLockRepository
import me.proton.android.drive.lock.domain.usecase.GetAutoLockDuration
import me.proton.android.drive.lock.domain.usecase.LockApp
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppLockModule {
    @Provides
    @Singleton
    fun provideBiometricManager(@ApplicationContext context: Context): BiometricManager =
        BiometricManager.from(context)

    @Provides
    @Singleton
    fun provideBiometricPromptProvider(biometricManager: BiometricManager): BiometricPromptProvider =
        BiometricPromptProviderImpl(biometricManager)

    @Singleton
    @Provides
    fun provideAppLockKeyRepository(
        appLockDatabase: AppLockDatabase,
    ): AppLockRepository =
        AppLockRepositoryImpl(appLockDatabase)

    @Singleton
    @Provides
    fun provideAppLockManager(
        appLockRepository: AppLockRepository,
    ): AppLockManager =
        AppLockManagerImpl(appLockRepository, Dispatchers.Main + Job())

    @Singleton
    @Provides
    fun provideAutoLockManager(
        workManager: WorkManager,
        lockApp: LockApp,
        getAutoLockDuration: GetAutoLockDuration,
    ): AutoLockManager =
        AutoLockManagerImpl(workManager, lockApp, getAutoLockDuration)

    @MapKey
    annotation class AppLockTypeKey(val value: AppLockType)

    @Singleton
    @Provides @IntoMap
    @AppLockTypeKey(AppLockType.SYSTEM)
    fun provideSystemLock(
        @ApplicationContext appContext: Context,
        appLockRepository: AppLockRepository,
        biometricPromptProvider: BiometricPromptProvider,
        biometricManager: BiometricManager,
    ): Lock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && biometricManager.hasBiometricHardware) {
        CryptoSystemLock(
            appContext = appContext,
            appLockRepository = appLockRepository,
            biometricPromptProvider = biometricPromptProvider,
        )
    } else {
        SystemLock(
            appContext = appContext,
            appLockRepository = appLockRepository,
            biometricPromptProvider = biometricPromptProvider,
        )
    }

    private val BiometricManager.hasBiometricHardware: Boolean get() =
        canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
}
