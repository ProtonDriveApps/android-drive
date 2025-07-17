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

package me.proton.android.drive.ui.test.flow.photos.list

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
import me.proton.android.drive.ui.data.ImageName
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.SettingsRobot
import me.proton.android.drive.ui.robot.settings.PhotosBackupRobot
import me.proton.android.drive.ui.rules.NetworkSimulator
import me.proton.android.drive.ui.test.PhotosBaseTest
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Before
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidTest
@UninstallModules(PhotosConfigurationModule::class)
class PhotosMigrationFlowTest : PhotosBaseTest() {

    @Inject
    lateinit var configurationProvider: ConfigurationProvider

    @Before
    fun setUp() {
        configurationProvider.debug.photosUpsellPhotoCount = Int.MAX_VALUE
        dcimCameraFolder.copyFileFromAssets("boat.jpg")
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun creatingShareShouldFailed() {
        NetworkSimulator.enabledPhotosMigration()

        PhotosTabRobot
            .verify { assertUpdateRequired() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun loadingPhotosListingShouldFailed() {
        NetworkSimulator.enabledPhotosMigration()

        PhotosTabRobot
            .verify { nodeWithTextDisplayed("Migration in progress") }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun startingBackupOnCreatedShareShouldFailed() {
        PhotosTabRobot.verify {
            assertPhotoDisplayed(ImageName.Main.fileName)
        }

        NetworkSimulator.enabledPhotosMigration()

        PhotosTabRobot
            .enableBackupWhenDisabled()
            .verify { assertUpdateRequired() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun startingBackupFromSettingShouldFailed() {
        NetworkSimulator.enabledPhotosMigration()

        PhotosTabRobot
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .clickBackupToggle(PhotosBackupRobot)
            .verify { nodeWithTextDisplayed("Migration in progress") }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2, isPhotos = true)
    fun startingBackupOnCreatedShareFromSettingShouldFailed() {
        PhotosTabRobot.verify {
            assertPhotoDisplayed(ImageName.Main.fileName)
        }

        NetworkSimulator.enabledPhotosMigration()

        PhotosTabRobot
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .clickBackupToggle(PhotosBackupRobot)
            .verify {
                assertPhotosBackupTurnOn()
            }
            .clickBack(SettingsRobot)
            .clickBack(PhotosTabRobot)
            .verify { assertMigrationInProgressDisplayed() }
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
