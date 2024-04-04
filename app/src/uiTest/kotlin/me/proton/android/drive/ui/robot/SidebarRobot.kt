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
import me.proton.android.drive.ui.extension.doesNotExist
import me.proton.android.drive.ui.robot.settings.GetMoreFreeStorageRobot
import me.proton.android.drive.ui.screen.HomeScreenTestTag
import me.proton.core.drive.navigationdrawer.presentation.NavigationDrawerTestTag
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object SidebarRobot : Robot {

    private val sidebar get() = node.withTag(HomeScreenTestTag.sidebar)
    private val content get() = node.withTag(NavigationDrawerTestTag.content)
    private val storageIndicator get() = node.withTag(NavigationDrawerTestTag.storageIndicator)
    private val trashNavigationItem get() = node.withText(I18N.string.navigation_item_trash)
    private val offlineNavigationItem get() = node.withText(I18N.string.navigation_item_offline)
    private val getMoreFreeStorageItem get() = node.withText(I18N.string.navigation_item_get_free_storage)

    private fun clickSidebarMenuItem(@StringRes menuItemName: Int) {
        node.withText(menuItemName).click()
    }

    fun scrollToItemWithName(itemName: String): SidebarRobot = apply {
        content.scrollTo(node.withText(itemName))
    }

    fun scrollToStorageIndicator(): SidebarRobot = apply {
        content.scrollTo(storageIndicator)
    }

    fun clickReportBug() {
        clickSidebarMenuItem(I18N.string.navigation_item_bug_report)
    }

    fun clickSubscription() {
        clickSidebarMenuItem(I18N.string.navigation_item_subscription)
    }

    fun clickTrash() = trashNavigationItem.clickTo(TrashRobot)

    fun clickOffline() = offlineNavigationItem.clickTo(OfflineRobot)

    fun clickSettings() = SettingsRobot.apply {
        clickSidebarMenuItem(I18N.string.navigation_item_settings)
    }

    fun clickGetMoreFreeStorage() = getMoreFreeStorageItem.clickTo(GetMoreFreeStorageRobot)

    fun assertGetMoreFreeStorageIsNotDisplayed() {
        getMoreFreeStorageItem.await { doesNotExist() }
    }

    override fun robotDisplayed() {
        sidebar.await { assertIsDisplayed() }
    }
}
