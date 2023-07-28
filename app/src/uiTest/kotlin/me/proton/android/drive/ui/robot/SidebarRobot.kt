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
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object SidebarRobot : Robot {

    private val sidebar get() = node.withTag(HomeScreenTestTag.sidebar)
    private val trashNavigationItem get() = node.withText(I18N.string.navigation_item_trash)

    private fun clickSidebarMenuItem(@StringRes menuItemName: Int) {
        node.withText(menuItemName).click()
    }

    fun clickReportBug() {
        clickSidebarMenuItem(I18N.string.navigation_item_bug_report)
    }

    fun clickSubscription() {
        clickSidebarMenuItem(I18N.string.navigation_item_subscription)
    }

    fun clickTrash() = trashNavigationItem.clickTo(TrashRobot)

    fun clickSettings() = SettingsRobot.apply {
        clickSidebarMenuItem(I18N.string.navigation_item_settings)
    }

    override fun robotDisplayed() {
        sidebar.await { assertIsDisplayed() }
    }
}
