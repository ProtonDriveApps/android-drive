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

import me.proton.test.fusion.Fusion.node
import androidx.annotation.StringRes
import me.proton.android.drive.ui.screen.HomeScreenTestTag
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.component.BottomNavigationComponentTestTag

interface HomeRobot : Robot {
    private val homeScreen get() = node.withTag(HomeScreenTestTag.screen)
    val filesTab get() = tabWithText(R.string.title_files)
    val sharedTab get() = tabWithText(R.string.title_shared)

    fun clickFilesTab() = filesTab.clickTo(FilesTabRobot)
    fun clickSharedTab() = sharedTab.clickTo(SharedTabRobot)

    fun homeScreenDisplayed() {
        homeScreen.assertIsDisplayed()
    }

    private fun tabWithText(@StringRes textRes: Int) =
        node
            .withTag(BottomNavigationComponentTestTag.tab)
            .hasChild(node.withText(textRes))
}
