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
import me.proton.core.drive.folder.create.presentation.R
import me.proton.core.test.android.instrumented.utils.StringUtils
import me.proton.test.fusion.ui.compose.builders.OnNode

interface Robot {
    fun robotDisplayed()

    fun <T : Robot> OnNode.clickTo(goesTo: T): T = goesTo.apply { click() }

    /** Common actions **/
    fun <T : Robot> dismissSuccessGrowler(itemName: String, goesTo: T) =
        node
            .withText(StringUtils.stringFromResource(R.string.folder_create_successful, itemName))
            .clickTo(goesTo)

    /** Common assertions **/
    fun nodeWithTextDisplayed(text: String) =
        node.withText(text).await { assertIsDisplayed() }
}
