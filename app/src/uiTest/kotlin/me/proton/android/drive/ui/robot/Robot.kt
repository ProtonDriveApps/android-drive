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
import androidx.compose.ui.test.longClick
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig
import me.proton.test.fusion.ui.compose.builders.OnNode
import me.proton.test.fusion.ui.compose.wrappers.NodeActions

interface Robot {

    fun robotDisplayed()

    fun <T : Robot> NodeActions.clickTo(goesTo: T): T = goesTo.apply { click() }

    fun <T : Robot> OnNode.longClickTo(goesTo: T): T = goesTo.apply {
        sendGesture {
            longClick()
        }
    }

    /** Common assertions **/
    fun nodeWithTextDisplayed(text: String) =
        node.withText(text).await { assertIsDisplayed() }

    fun nodeWithTextDisplayed(@StringRes stringRes: Int) =
        node.withText(stringRes).await { assertIsDisplayed() }

    fun nodeWithTextDisplayed(@StringRes stringRes: Int, vararg formatArgs: Any) =
        node
            .withText(
                FusionConfig.targetContext.getString(stringRes, *formatArgs)
            )
            .await { assertIsDisplayed() }

    fun nodeWithContentDescriptionDisplayed(contentDescription: String) =
        node.withContentDescription(contentDescription).await { assertIsDisplayed() }

    fun nodeWithContentDescriptionDisplayed(@StringRes stringRes: Int) =
        node.withContentDescription(stringRes).await { assertIsDisplayed() }
}
