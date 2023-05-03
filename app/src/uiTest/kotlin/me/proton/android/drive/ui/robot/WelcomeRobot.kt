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

import me.proton.android.drive.ui.screen.WelcomeScreenTestTag
import me.proton.test.fusion.Fusion.allNodes
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.ui.common.enums.SwipeDirection
import me.proton.core.drive.i18n.R as I18N

object WelcomeRobot : Robot {
    private val skipButton get() = allNodes.withText(I18N.string.common_skip_action).onFirst()
    private val nextButton get() = allNodes.withText(I18N.string.common_next_action).onFirst()
    private val getStartedButton get() = node.withText(I18N.string.welcome_get_started_action)
    private val welcomeLabel get() = node.withText(I18N.string.welcome_to_description)
    private val filesTitle get() = node.withText(I18N.string.title_welcome_files)
    private val titleSharing get() = node.withText(I18N.string.title_welcome_sharing)
    private val filesDescription get() = node.withText(I18N.string.welcome_files_description)
    private val sharingDescription get() = node.withText(I18N.string.welcome_sharing_description)
    private val welcomeScreen get() = node.withTag(WelcomeScreenTestTag.screen)

    fun clickNext() = nextButton.clickTo(this)
    fun clickSkip() = skipButton.clickTo(FilesTabRobot)
    fun clickGetStarted() = getStartedButton.clickTo(FilesTabRobot)
    fun swipeLeft() = apply { welcomeScreen.swipe(SwipeDirection.Left) }
    fun swipeRight() = apply { welcomeScreen.swipe(SwipeDirection.Right) }

    override fun robotDisplayed() {
        welcomeScreen.await { assertIsDisplayed() }
    }
}
