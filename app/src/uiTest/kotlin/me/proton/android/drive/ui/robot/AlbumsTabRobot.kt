/*
 * Copyright (c) 2023-2024 Proton AG.
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

import me.proton.android.drive.photos.presentation.extension.albumDetails
import me.proton.android.drive.ui.test.AbstractBaseTest.Companion.targetContext
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.test.fusion.Fusion.node
import me.proton.core.drive.i18n.R as I18N

object AlbumsTabRobot :
    SystemNotificationPermissionRobot,
    SystemPhotosPermissionSelectionRobot,
    SystemPhotosNoPermissionRobot,
    HomeRobot,
    LinksRobot,
    NavigationBarRobot {
    private val albumTitle get() = node.withText(I18N.string.albums_title)

    private val filterAll get() = node.withText(I18N.string.albums_filter_all)
    private val filterMyAlbums get() = node.withText(I18N.string.albums_filter_my_albums)
    private val filterSharedByMe get() = node.withText(I18N.string.albums_filter_shared_by_me)
    private val filterSharedWithMe get() = node.withText(I18N.string.albums_filter_shared_with_me)

    private val emptyTitle get() = node.withText(I18N.string.albums_empty_albums_list_screen_title)
    private val emptyDescription get() = node.withText(I18N.string.albums_empty_albums_list_screen_description)

    private val plusButton get() = node.withContentDescription(I18N.string.content_description_albums_new)


    fun clickOnFilterAll() = apply {
        filterAll.click()
    }

    fun clickOnFilterAlbums() = apply {
        filterMyAlbums.click()
    }

    fun clickOnFilterSharedByMe() = apply {
        filterSharedByMe.click()
    }

    fun clickOnFilterSharedWithMe() = apply {
        filterSharedWithMe.click()
    }

    fun clickPlusButton() = plusButton.clickTo(CreateAlbumTabRobot)

    fun assertAlbumIsDisplayed(name: String) {
        node.withText(name).await { assertIsDisplayed() }
    }

    fun assertAlbumIsDisplayed(
        name: String,
        photoCount: Long,
        isShared: Boolean = false,
        creationTime: TimestampS? = null,
    ) {
        node.withText(name)
            .hasSibling(
                node.withText(
                    albumDetails(
                        appContext = targetContext,
                        photoCount = photoCount,
                        isShared = isShared,
                        creationTime = creationTime,
                    )
                )
            )
            .await { assertIsDisplayed() }
    }

    fun assertAlbumIsNotDisplayed(name: String) {
        node.withText(name).await { assertIsNotDisplayed() }
    }

    fun assertIsEmpty() {
        emptyTitle.await { assertIsDisplayed() }
        emptyDescription.await { assertIsDisplayed() }
    }

    override fun robotDisplayed() {
        homeScreenDisplayed()
        photosTab.assertIsSelected()
        albumTitle.assertIsDisplayed()
    }
}
