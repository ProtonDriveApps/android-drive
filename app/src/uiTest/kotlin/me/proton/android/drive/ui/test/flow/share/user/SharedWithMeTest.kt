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
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.SharedTabRobot
import me.proton.android.drive.ui.robot.SharedWithMeRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.android.drive.ui.test.flow.details.DetailsFlowTest.LinkDetails
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_SHARING_INVITATIONS
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import me.proton.test.fusion.FusionConfig
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class SharedWithMeTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(2)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun emptyList() {
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .verify {
                assertEmpty()
            }
    }

    @Test
    @Scenario(6, loginWithSharingUser = true)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun browseSharedFolder() {
        val folder = "ReadWriteFolder"
        val fileInFolder = "EditableFile.txt"
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .scrollToItemWithName(folder)
            .clickOnFolder(folder)
            .verify {
                nodeWithTextDisplayed(fileInFolder)
            }
    }

    @Test
    @Scenario(6, loginWithSharingUser = true)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun previewTextFile() {
        val file = "newShareInsideLegacy.txt"
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .scrollToItemWithName(file)
            .clickOnFile(file)
            .verify {
                nodeWithTextDisplayed("Hello World!")
            }
    }

    @Test
    @Scenario(6, loginWithSharingUser = true)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun makeAvailableOffline() {
        val file = "newShareInsideLegacy.txt"
        PhotosTabRobot
            .navigateToSharedWithMeTab()
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
    @Scenario(6, loginWithSharingUser = true)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun fileDetails() {
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .scrollToItemWithName(newShareInsideLegacy.name)
            .clickMoreOnItem(newShareInsideLegacy.name)
            .clickFileDetails()
            .verify {
                robotDisplayed()
                hasHeaderTitle(newShareInsideLegacy.name)
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_name_entry),
                    value = newShareInsideLegacy.name,
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = newShareInsideLegacy.uploadedBy,
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_last_modified_entry),
                    value = newShareInsideLegacy.modified,
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_size_entry),
                    value = requireNotNull(newShareInsideLegacy.size),
                )
                hasInfoItem(
                    name = FusionConfig.targetContext.getString(I18N.string.file_info_shared_entry),
                    value = newShareInsideLegacy.isShared,
                )
            }
            .clickBack(SharedTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @Scenario(6, loginWithSharingUser = true)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun leaveShare() {
        val newShare = "newShareInsideLegacy.txt"
        PhotosTabRobot
            .navigateToSharedWithMeTab()
            .scrollToItemWithName(newShare)
            .clickMoreOnItem(newShare)
            .clickRemoveMe(SharedWithMeRobot)
            .verify {
                itemIsNotDisplayed(newShare)
            }
    }

    private val newShareInsideLegacy get() = LinkDetails(
        name = "newShareInsideLegacy.txt",
        uploadedBy = "${testUser.name}@${envConfig.host}",
        modified = TimestampS().asHumanReadableString(),
        isShared = FusionConfig.targetContext.getString(I18N.string.common_yes),
        mimeType = "text/plain",
        size = 63.bytes.asHumanReadableString(FusionConfig.targetContext),
    )

    private fun PhotosTabRobot.navigateToSharedWithMeTab(): SharedWithMeRobot =
        this
            .verify {
                robotDisplayed()
                waitUntilLoaded() // TODO: remove once old "Shared" tab is removed
            }
            .clickSharedTab()
            .clickSharedWithMeTab()
}
