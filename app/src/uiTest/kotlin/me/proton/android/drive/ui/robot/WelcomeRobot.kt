/*
 * Copyright (c) 2023 Proton AG.
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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import me.proton.android.drive.ui.screen.WelcomeScreenTestTag
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

object WelcomeRobot : Robot {
    private val skipButton get() = nodes(hasTextResource(CorePresentation.string.presentation_skip)).onFirst()
    private val nextButton get() = nodes(hasTextResource(BasePresentation.string.common_next_action)).onFirst()
    private val getStartedButton get() = node(hasTextResource(BasePresentation.string.welcome_get_started_action))
    private val welcomeLabel get() = node(hasTextResource(BasePresentation.string.welcome_to_description))
    private val filesTitle get() = node(hasTextResource(BasePresentation.string.title_welcome_files))
    private val titleSharing get() = node(hasTextResource(BasePresentation.string.title_welcome_sharing))
    private val filesDescription get() = node(hasTextResource(BasePresentation.string.welcome_files_description))
    private val sharingDescription get() = node(hasTextResource(BasePresentation.string.welcome_sharing_description))
    private val welcomeScreen get() = node(hasTestTag(WelcomeScreenTestTag.screen))

    fun clickNext() = nextButton.tryToClickAndGoTo(this)
    fun clickSkip() = skipButton.tryToClickAndGoTo(FilesTabRobot)
    fun clickGetStarted() = getStartedButton.tryToClickAndGoTo(FilesTabRobot)
    fun swipeLeft() = welcomeScreen.tryPerformTouchInputAndGoTo(this) { this.swipeLeft() }
    fun swipeRight() = welcomeScreen.tryPerformTouchInputAndGoTo(this) { this.swipeRight() }

    override fun robotDisplayed() {
        welcomeScreen.assertIsDisplayed()
    }
}
