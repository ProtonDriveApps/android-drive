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
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.SettingsRobot
import me.proton.android.drive.ui.robot.settings.PhotosBackupRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.PhotosBaseTest
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_PHOTOS_UPLOAD_DISABLED
import org.junit.Before
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidTest
@UninstallModules(PhotosConfigurationModule::class)
class PhotosSyncFlowTest : PhotosBaseTest() {

    @Inject lateinit var configurationProvider: ConfigurationProvider

    @Before
    fun setUp(){
        configurationProvider.debug.photosUpsellPhotoCount = Int.MAX_VALUE
    }

    @Test
    @Scenario(2)
    fun syncMultipleFolders() {
        pictureCameraFolder.copyDirFromAssets("images/basic")
        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .enableBackup()
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoDisplayed("boat.jpg")
                assertPhotoCountEquals(5)
            }
    }

    @Test
    fun syncNewPhotos() {
        dcimCameraFolder.copyDirFromAssets("images/basic")

        PhotosTabRobot
            .enableBackup()
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoCountEquals(4)
            }

        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .verify {
                assertPhotoDisplayed("boat.jpg")
                assertPhotoCountEquals(5)
            }
    }

    @Test
    fun addPhotoWhileUploading() {
        dcimCameraFolder.copyDirFromAssets("images/basic")
        dcimCameraFolder.copyFileFromAssets("boat.mp4")

        PhotosTabRobot
            .enableBackup()

        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoDisplayed("boat.jpg")
                assertPhotoDisplayed("boat.mp4")
                assertPhotoCountEquals(6)
            }
    }

    @Test
    fun backupVideoAndMakeAvailableOffline() {
        val videoFile = "boat.mp4"
        pictureCameraFolder.copyFileFromAssets(videoFile)

        PhotosTabRobot
            .enableBackup()
            .verify {
                assertPhotoDisplayed(videoFile)
            }
            .longClickOnPhoto(videoFile)
            .clickOptions()
            .clickMakeAvailableOffline(goesTo = PhotosTabRobot)
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(videoFile)
            }
    }

    @Test
    @Scenario(2)
    fun turnOnBackupWithFilesInCameraFolderFromSettings() {
        dcimCameraFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .verify {
                // wait photo share to be created
                assertEnableBackupDisplayed()
            }
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .verify {
                assertFoldersList()
            }
            .clickBackupToggle(SettingsRobot)
            .verify {
                robotDisplayed()
            }
            .clickBack(FilesTabRobot)
            .clickPhotosTab()
            .verify {
                assertPhotoDisplayed("boat.jpg")
            }
    }

    @Test
    fun noPhotoInCameraFolder() {
        PhotosTabRobot
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .verify {
                assertFolderNotFound("Camera")
            }
    }

    @Test
    fun photosFolderEnableFromSettings() {
        picturePhotosFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .verify {
                // wait photo share to be created
                assertNoBackupsDisplayed()
            }
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .verify {
                assertFoldersList()
            }
            .clickFolder("Photos").verify {
                assertFolderEnable("Photos")
            }
            .clickBack(SettingsRobot)
            .clickBack(FilesTabRobot)
            .clickPhotosTab()
            .verify {
                assertPhotoDisplayed("boat.jpg")
            }
    }

    @Test
    fun photosFolderEnableFromPhoto() {
        picturePhotosFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .verify {
                assertMissingFolderDisplayed()
            }
            .clickOnMore()
            .verify {
                assertFoldersList()
            }
            .clickFolder("Photos").verify {
                assertFolderEnable("Photos")
            }
            .clickBack(PhotosTabRobot)
            .verify {
                assertPhotoDisplayed("boat.jpg")
            }
    }

    @Test
    fun photosFolderEnableAndDisableFromSettings() {
        picturePhotosFolder.copyFileFromAssets("boat.jpg")

        PhotosTabRobot
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .verify {
                assertFoldersList()
            }
            .clickFolder("Photos").verify {
                assertFolderEnable("Photos")
            }
            .clickFolder("Photos")
            .clickConfirm()
            .verify {
                assertFolderDisable("Photos")
            }
            .clickBack(SettingsRobot)
            .clickBack(FilesTabRobot)
            .clickPhotosTab()
            .verify {
                assertMissingFolderDisplayed()
            }
    }

    @Test
    fun backupDifferentVideoFormatFiles() {
        pictureCameraFolder.copyDirFromAssets("videos/formats")
        val videoFiles = arrayOf("3gp.3gp", "mov.mov", "mp4.mp4")

        PhotosTabRobot
            .enableBackup()
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoCountEquals(3)
            }

        videoFiles.forEach {
            PhotosTabRobot
                .clickOnPhoto(it)
                .verify {
                    assertMediaPreviewDisplayed(it)
                }
                .clickBack(FilesTabRobot)
        }
    }

    @Test
    fun deleteFileWhileBackupIsInProgress() {
        pictureCameraFolder.copyDirFromAssets("images/basic/1")
        pictureCameraFolder.copyDirFromAssets("videos/formats")

        PhotosTabRobot
            .enableBackup()
            .verify {
                assertLeftToBackup(5)
            }

        dcimCameraFolder.deleteFile("mp4.mp4")

        PhotosTabRobot
            .verify {
                assertBackupCompleteDisplayed()
                assertPhotoCountEquals(4)
                assertPhotoDisplayed("1_0.jpg")
                assertPhotoDisplayed("1_0.png")
                assertPhotoDisplayed("3gp.3gp")
                assertPhotoDisplayed("mov.mov")
            }
    }

    @Test
    @FeatureFlag(DRIVE_PHOTOS_UPLOAD_DISABLED, ENABLED)
    fun featureDisabled() {
        PhotosTabRobot
            .verify {
                assertPhotosBackupDisabled()
            }
    }

    @Test
    @FeatureFlag(DRIVE_PHOTOS_UPLOAD_DISABLED, ENABLED)
    fun featureDisabledFromSettings() {
        PhotosTabRobot
            .openSidebarBySwipe()
            .clickSettings()
            .clickPhotosBackup()
            .clickBackupToggle(PhotosBackupRobot)
            .verify {
                assertPhotosBackupDisabled()
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
