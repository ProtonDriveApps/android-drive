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

package me.proton.android.drive.ui.test.flow.computers

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.robot.ComputersTabRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.ShareRobot
import me.proton.android.drive.ui.rules.ExternalFilesRule
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_SHARING
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.command.userCreate
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SyncedFoldersFlowTest : AuthenticatedBaseTest() {

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val externalFilesRule = ExternalFilesRule()

    @Test
    @Scenario(value = 2, isDevice = true)
    fun emptySyncedFolders() {
        FilesTabRobot
            .clickComputersTab()
            .scrollToComputer(MY_DEVICE_3)
            .clickOnComputer(MY_DEVICE_3)
            .verify {
                assertEmptySyncedFolders()
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun uploadAFile() {
        val fileName = "1kB.txt"
        val file = externalFilesRule.createFile(fileName, 1000.bytes.value)

        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWithFunction {
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        }

        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .clickPlusButton()
            .clickUploadAFile()
            .verify {
                itemIsDisplayed(fileName)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun createAFolderViaPlusButton() {
        val newFolderName = "TestFolder"

        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .clickPlusButton()
            .clickCreateFolder()
            .typeFolderName(newFolderName)
            .clickCreate(FilesTabRobot)
            .dismissFolderCreateSuccessGrowler(newFolderName, FilesTabRobot)
            .verify {
                itemIsDisplayed(newFolderName)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun moveFile() {
        val file = "file3"
        val folder = "folder"
        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER_3)
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMove()
            .scrollToItemWithName(folder)
            .clickOnFolderToMove(folder)
            .clickMoveToFolder(folder)

        ComputersTabRobot
            .verify {
                robotDisplayed()
            }
            .verify {
                itemIsNotDisplayed(file)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun moveFileToAnotherSyncedFolder() {
        val file = "file1"
        val folder = MY_DEVICE_1_SYNC_FOLDER_3
        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMove()
            .verify {
                itemIsDisplayed(file)
            }
            .clickBackFromFolder(MY_DEVICE_1_SYNC_FOLDER)
            .verify {
                assertMoveButtonIsDisabled()
                assertCreateFolderButtonDoesNotExist()
            }
            .scrollToItemWithName(folder)
            .clickOnFolderToMove(folder)
            .clickMoveToFolder(folder)

        ComputersTabRobot
            .verify {
                robotDisplayed()
            }
            .clickBack(ComputersTabRobot)
            .clickOnFolder(folder)
            .verify {
                itemIsDisplayed(file)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun renameFile() {
        val itemToBeRenamed = "presentation.pdf"
        val newItemName = "Dummy_PDF_file.pdf"

        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .scrollToItemWithName(itemToBeRenamed)
            .clickMoreOnItem(itemToBeRenamed)
            .clickRename()
            .clearName()
            .typeName(newItemName)
            .clickRename()
            .scrollToItemWithName(newItemName)
            .verify {
                itemIsDisplayed(newItemName)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    @FeatureFlag(DRIVE_SHARING, NOT_FOUND)
    fun shareFile() {
        val file = "picWithThumbnail.jpg"
        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .clickMoreOnItem(file)
            .clickGetLink()
            .verifyShareLinkFile(file)

        ShareRobot
            .clickBack(ComputersTabRobot)

        FilesTabRobot
            .clickSharedTab()
            .scrollToItemWithName(file)
            .verify {
                itemIsDisplayed(file)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    @FeatureFlag(DRIVE_SHARING, ENABLED)
    fun shareFileWithInternalUser() {
        val email = requireNotNull(quarkRule.quarkCommands.userCreate(User()).email)
        val file = "picWithThumbnail.jpg"
        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .clickMoreOnItem(file)
            .clickShare()
            .verify {
                robotDisplayed()
                assertShareFile(file)
            }
            .typeEmail(email)
            .clickSend()
            .verify {
                assertInvitationSent(1)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun moveFileToTrash() {
        val file = "picWithThumbnail.jpg"
        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .clickMoreOnItem(file)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                itemIsNotDisplayed(file)
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                itemIsDisplayed(file)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun restoreFromTrash() {
        val file = "trashedFileInDevice"
        FilesTabRobot
            .clickSidebarButton()
            .clickTrash()
            .clickMoreOnItem(file)
            .clickRestoreTrash()
            .clickBack(FilesTabRobot)
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .verify {
                itemIsDisplayed(file)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun makeAvailableOffline() {
        val file = "presentation.pdf"
        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMakeAvailableOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
            .openSidebarBySwipe()
            .clickOffline()
            .verify {
                itemIsDisplayed(file, downloadState = SemanticsDownloadState.Downloaded)
            }
    }

    @Test
    @Scenario(value = 2, isDevice = true)
    fun previewPdfFile() {
        val file = "presentation.pdf"
        FilesTabRobot
            .navigateToComputerSyncedFolder(MY_DEVICE_1, MY_DEVICE_1_SYNC_FOLDER)
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed("1 / 1")
            }
    }

    private fun FilesTabRobot.navigateToComputerSyncedFolder(
        computerName: String,
        syncedFolderName: String,
    ): FilesTabRobot =
        this
            .clickComputersTab()
            .scrollToComputer(computerName)
            .clickOnComputer(computerName)
            .clickOnFolder(syncedFolderName)

    companion object {
        private const val MY_DEVICE_1 = "MyDevice1"
        private const val MY_DEVICE_1_SYNC_FOLDER = "syncFolder"
        private const val MY_DEVICE_1_SYNC_FOLDER_3 = "syncFolder3"
        private const val MY_DEVICE_3 = "MyDevice3"
    }
}
