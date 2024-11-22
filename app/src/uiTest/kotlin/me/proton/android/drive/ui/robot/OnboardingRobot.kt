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

package me.proton.android.drive.ui.robot

import me.proton.android.drive.ui.dialog.OnboardingTestTag
import me.proton.core.drive.base.presentation.component.TopAppBarComponentTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.ui.common.enums.SwipeDirection
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

object OnboardingRobot : SystemPhotosNoPermissionRobot, SystemPhotosPermissionSelectionRobot, Robot {
    private val notNowButton get() = node.withText(I18N.string.onboarding_action_skip)
    private val primaryActionButton get() = node.withText(I18N.string.onboarding_action_primary)
    private val secondaryActionButton get() = node.withText(I18N.string.onboarding_action_secondary)
    private val doneButton get() = node.withText(I18N.string.onboarding_action_done)
    private val onboardingScreen get() = node.withTag(OnboardingTestTag.screen)
    private val mainOnboardingScreen get() = node.withTag(OnboardingTestTag.main)
    private val photoBackupOnboardingScreen get() = node.withTag(OnboardingTestTag.photoBackup)

    fun clickNotNow() = notNowButton.clickTo(PhotosTabRobot)
    fun <T : Robot> clickEnablePhotoBackup(goesTo: T): T = primaryActionButton.clickTo(goesTo)
    fun clickMoreOptions() = secondaryActionButton.clickTo(this)
    fun <T : Robot> clickDone(goesTo: T): T = doneButton.clickTo(goesTo)

    fun dismissBySwipe() = PhotosTabRobot.apply {
        onboardingScreen.swipe(SwipeDirection.Down)
    }

    fun assertMainDisplayed() = mainOnboardingScreen.await { assertIsDisplayed() }
    fun assertPhotoBackupDisplayed() = photoBackupOnboardingScreen.await { assertIsDisplayed() }

    fun clickBack(): OnboardingRobot = apply {
        node
            .withTag(TopAppBarComponentTestTag.navigationButton)
            .hasAncestor(photoBackupOnboardingScreen)
            .click()
    }
    override fun robotDisplayed() {
        onboardingScreen.await(50.seconds) { assertIsDisplayed() }
    }
}
