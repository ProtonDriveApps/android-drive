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
import me.proton.android.drive.ui.extension.tomorrow
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.ManageAccessRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.rules.Scenario
import me.proton.android.drive.ui.test.AuthenticatedBaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_SHARING_INVITATIONS
import org.junit.Test

@HiltAndroidTest
class ChangeExpirationDateManageAccessTest : AuthenticatedBaseTest() {

    @Test
    @Scenario(4)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun setExpirationDate() {
        val fileName = "image.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickManageAccess()
            .clickAllowToAnyone()
            .clickSettings()
            .clickExpirationDateTextField()
            .verify { robotDisplayed() }
            .selectDate(tomorrow)
            .clickOk()
            .clickSave()
            .clickUpdateSuccessfulGrowler()
            .clickBack(ManageAccessRobot)
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickManageAccess()
            .clickSettings()
            .verify {
                robotDisplayed()
                expirationDateToggleIsOn()
                expirationDateIsShown(tomorrow)
            }
    }

    @Test
    @Scenario(4)
    @FeatureFlag(DRIVE_SHARING_INVITATIONS, ENABLED)
    fun changeExpirationDateOfExpiredLink() {
        val fileName = "expiredSharedFile.jpg"
        PhotosTabRobot
            .clickFilesTab()
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickManageAccess()
            .clickSettings()
            .clickExpirationDateTextField()
            .verify { robotDisplayed() }
            .selectDate(tomorrow)
            .clickOk()
            .clickSave()
            .clickUpdateSuccessfulGrowler()
            .clickBack(ManageAccessRobot)
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
            .scrollToItemWithName(fileName)
            .clickMoreOnItem(fileName)
            .clickManageAccess()
            .clickSettings()
            .verify {
                robotDisplayed()
                expirationDateToggleIsOn()
                expirationDateIsShown(tomorrow)
            }
    }
}
