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

import androidx.annotation.StringRes
import me.proton.android.drive.ui.screen.HomeScreenTestTag
import me.proton.core.drive.base.presentation.component.BottomNavigationComponentTestTag
import me.proton.core.drive.base.presentation.component.TopAppBarComponentTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.ui.common.enums.SwipeDirection
import me.proton.test.fusion.ui.compose.builders.OnNode
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

interface HomeRobot : Robot {
    val homeScreen get() = node.withTag(HomeScreenTestTag.screen)
    val filesTab get() = tabWithText(I18N.string.title_files)
    val photosTab get() = tabWithText(I18N.string.photos_title)
    val sharedTab get() = tabWithText(I18N.string.title_shared)

    fun clickFilesTab() = filesTab.clickTo(FilesTabRobot)

    fun clickPhotosTab() = photosTab.clickTo(PhotosTabRobot)
    fun clickSharedTab() = sharedTab.clickTo(SharedTabRobot)

    fun openSidebarBySwipe() = SidebarRobot.apply {
        homeScreen.swipe(SwipeDirection.Right)
    }

    private fun tabWithText(@StringRes textRes: Int) =
        node
            .withTag(BottomNavigationComponentTestTag.tab)
            .hasChild(node.withText(textRes))

    fun homeScreenDisplayed() {
        homeScreen.await(50.seconds) { assertIsDisplayed() }
    }
}
