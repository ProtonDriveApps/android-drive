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

import me.proton.core.drive.base.presentation.component.TopAppBarComponentTestTag
import me.proton.test.fusion.Fusion.node

interface NavigationBarRobot : Robot {
    /** Ideally, different selectors should be used for 'back' and 'hamburger' icons **/
    private val navigationButton
        get() = node.withTag(TopAppBarComponentTestTag.navigationButton).isClickable()
    val navigationHamburgerButton get() = navigationButton
    val navigationBackButton get() = navigationButton
    val navigationCloseButton get() = navigationButton

    fun <T : Robot> clickBack(goesTo: T) = navigationBackButton.clickTo(goesTo)

    fun <T : Robot> close(goesTo: T) = navigationCloseButton.clickTo(goesTo)

    fun clickSidebarButton() = navigationHamburgerButton.clickTo(SidebarRobot)
}
