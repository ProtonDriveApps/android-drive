/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.ui.test.flow.photos

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import me.proton.android.drive.extension.debug
import me.proton.android.drive.photos.data.di.PhotosConfigurationModule
import me.proton.android.drive.photos.domain.provider.PhotosDefaultConfigurationProvider
import me.proton.android.drive.provider.PhotosConnectedDefaultConfigurationProvider
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.annotation.Quota
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.test.PhotosBaseTest
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Before
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidTest
@UninstallModules(PhotosConfigurationModule::class)
class UsedSpaceTest : PhotosBaseTest() {

    @Inject
    lateinit var configurationProvider: ConfigurationProvider

    @Before
    fun prepare() {
        configurationProvider.debug.photosUpsellPhotoCount = Int.MAX_VALUE
        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .enableBackup()
            .dismissBackupSetupGrowler("Camera")
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", quota = Quota(percentageFull = 100))
    fun storageFull() {
        PhotosTabRobot
            .verify {
                assertBackupFailed()
                assertStorageFull()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", quota = Quota(percentageFull = 50))
    fun storageHalfFull() {
        PhotosTabRobot
            .verify {
                assertBackupCompleteDisplayed()
                assertStorageFull(50)
            }
            .clickGetMoreStorage()
            .currentPlanIsDisplayed()
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", quota = Quota(percentageFull = 80))
    fun storage80PercentFull() {
        PhotosTabRobot
            .verify {
                assertBackupCompleteDisplayed()
                assertStorageFull(80)
            }
            .dismissQuotaBanner()
            .verify {
                assertPhotoCountEquals(1)
            }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    @Suppress("Unused")
    interface TestPhotosConfigurationModule {
        @Binds
        @Singleton
        fun bindPhotosDefaultConfigurationProvider(
            impl: PhotosConnectedDefaultConfigurationProvider,
        ): PhotosDefaultConfigurationProvider
    }
}
