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

import me.proton.android.drive.ui.dialog.WhatsNewTestTag
import me.proton.test.fusion.Fusion.node
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

object WhatsNewRobot : Robot {
    private val gotItButton get() = node.withText(I18N.string.whats_new_public_sharing_action)
    private val whatsNewScreen get() = node.withTag(WhatsNewTestTag.screen)

    fun <T : Robot> clickGotIt(goesTo: T) = gotItButton.clickTo(goesTo)

    override fun robotDisplayed() {
        whatsNewScreen.await { assertIsDisplayed() }
    }
}
