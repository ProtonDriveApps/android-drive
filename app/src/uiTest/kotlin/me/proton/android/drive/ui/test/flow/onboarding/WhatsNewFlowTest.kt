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

package me.proton.android.drive.ui.test.flow.onboarding

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.FeatureFlag
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.WhatsNewRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_ANDROID_WHATS_NEW
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class WhatsNewFlowTest : BaseTest() {
    override val shouldShowWhatsNewAfterLogin get() = true

    @Test
    @FeatureFlag(DRIVE_ANDROID_WHATS_NEW, ENABLED)
    @PrepareUser(loginBefore = true)
    @Ignore("PROTON_DOCS was limited to 2024, enable the test for a new whats new")
    fun whatsNewShouldShow() {
        WhatsNewRobot
            .verify {
                robotDisplayed()
            }
            .clickGotIt(PhotosTabRobot)
            .verify {
                robotDisplayed()
            }
    }
}
