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

package me.proton.android.drive.ui.test.flow.share.user

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.robot.ConfirmStopSharingRobot
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.PreviewRobot
import me.proton.android.drive.ui.robot.SharedByMeRobot
import me.proton.android.drive.ui.robot.SharedTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.android.drive.ui.test.flow.details.DetailsFlowTest.LinkDetails
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import me.proton.core.test.rule.annotation.PrepareUser
import me.proton.test.fusion.FusionConfig
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class SharedByMeTest : BaseTest() {

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 2)
    fun emptySharedByMe() {
        PhotosTabRobot
            .navigateToSharedByMeTab()
            .verify {
                assertEmpty()
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 6)
    fun previewTextFile() {
        val file = "newShare.txt"
        PhotosTabRobot
            .navigateToSharedByMeTab()
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                PreviewRobot.nodeWithTextDisplayed("Hello World!")
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 6)
    fun renameFile() {
        val itemToBeRenamed = "newShare.txt"
        val newItemName = "renamedShare.txt"

        PhotosTabRobot
            .navigateToSharedByMeTab()
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
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 6)
    fun moveFileToTrashAndRestoreIt() {
        val file = "newShare.txt"
        PhotosTabRobot
            .navigateToSharedByMeTab()
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickMoveToTrash()
            .dismissMoveToTrashSuccessGrowler(1, FilesTabRobot)
            .verify {
                //itemIsNotDisplayed(file)//TODO: uncomment this once fixed on BE
            }
            .openSidebarBySwipe()
            .clickTrash()
            .verify {
                itemIsDisplayed(file)
            }
            .clickMoreOnItem(file)
            .clickRestoreTrash()
            .clickBack(SharedTabRobot)
            .clickSharedByMeTab()
            .verify {
                itemIsDisplayed(file)
            }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 6)
    fun makeAvailableOffline() {
        val file = "newShare.txt"
        PhotosTabRobot
            .navigateToSharedByMeTab()
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
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 6)
    fun fileDetails() {
        PhotosTabRobot
            .navigateToSharedByMeTab()
            .scrollToItemWithName(newShareText.name)
            .clickMoreOnItem(newShareText.name)
            .clickFileDetails()
            .verify {
                robotDisplayed()
                hasHeaderTitle(newShareText.name)
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_name_entry),
                    value = newShareText.name,
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = newShareText.uploadedBy,
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_location_entry),
                    value = requireNotNull(newShareText.location),
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_last_modified_entry),
                    value = newShareText.modified,
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_mime_type_entry),
                    value = requireNotNull(newShareText.mimeType),
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_size_entry),
                    value = requireNotNull(newShareText.size),
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_shared_entry),
                    value = newShareText.isShared,
                )
            }
            .clickBack(SharedTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(loginBefore = true)
    @Scenario(forTag = "main", value = 6)
    fun stopSharing() {
        val file = "newShare.txt"
        PhotosTabRobot
            .navigateToSharedByMeTab()
            .scrollToItemWithName(file)
            .clickMoreOnItem(file)
            .clickManageAccess()
            .clickStopSharing(ConfirmStopSharingRobot)
            .confirmStopSharing(SharedTabRobot)
            .pullToRefresh(SharedTabRobot)
            .verify {
                itemIsNotDisplayed(file)
            }
    }

    private val newShareText
        get() = LinkDetails(
            name = "newShare.txt",
            uploadedBy = "${protonRule.testDataRule.mainTestUser!!.name}@${envConfig.host}",
            location = "/" + FusionConfig.targetContext.getString(I18N.string.title_my_files),
            modified = TimestampS().asHumanReadableString(),
            isShared = FusionConfig.targetContext.getString(I18N.string.common_yes),
            mimeType = "text/plain",
            size = 63.bytes.asHumanReadableString(FusionConfig.targetContext),
        )

    private fun PhotosTabRobot.navigateToSharedByMeTab(): SharedByMeRobot =
        this
            .verify {
                robotDisplayed()
            }
            .clickSharedTab()
            .clickSharedByMeTab()
}
