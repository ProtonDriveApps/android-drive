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

import me.proton.android.drive.ui.dialog.ConfirmStopSharingDialogTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object ConfirmStopSharingRobot : Robot {
    private val stopSharingMessage
        get() = node.withText(I18N.string.description_files_stop_sharing)
            .hasAncestor(node.isDialog())
    private val stopSharingButton
        get() = node.withTag(ConfirmStopSharingDialogTestTag.confirmStopSharing)

    fun confirmStopSharing() = stopSharingButton.clickTo(FilesTabRobot)

    fun <T : Robot> confirmStopSharing(goesTo: T) = stopSharingButton.clickTo(goesTo)

    override fun robotDisplayed() {
        stopSharingMessage.await { assertIsDisplayed() }
    }

}
