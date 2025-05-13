/*
 * Copyright (c) 2025 Proton AG.
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

import androidx.compose.ui.test.assertCountEquals
import me.proton.android.drive.ui.extension.withItemType
import me.proton.android.drive.ui.extension.withLayoutType
import me.proton.android.drive.ui.extension.withLinkName
import me.proton.android.drive.ui.screen.AlbumScreenTestTag
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.test.fusion.Fusion.allNodes
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.FusionConfig.targetContext
import me.proton.core.drive.i18n.R as I18N
import me.proton.android.drive.photos.presentation.component.ProtonMediaItemTestTags
import me.proton.test.fusion.ui.compose.ComposeWaiter.waitFor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object AlbumRobot : LinksRobot, NavigationBarRobot {
    private val albumScreen get() = node.withTag(AlbumScreenTestTag.screen)
    private val moreButton get() = node.withContentDescription(I18N.string.common_more)
    private val addButton get() = node.withText(I18N.string.common_add_action)
    private val saveAllButton get() = node.withText(I18N.string.common_save_all_action)
    private val shareButton get() = node.withText(I18N.string.common_share)
    private val emptyAlbumText get() = node.withText(I18N.string.albums_empty_album_screen_title)
    private val mediaItems get() = allNodes.withTag(ProtonMediaItemTestTags.mediaItemPreviewBox)
    private val favoriteFromForeignVolume
        get() = node.withText(I18N.string.files_add_to_favorite_from_foreign_volume_action_success)
    private val addToAlbumStartMessage
        get() = node.withText(I18N.string.albums_add_to_album_start_message)

    fun clickOnMoreButton() = moreButton.clickTo(AlbumOptionsRobot)

    fun clickOnAdd() = addButton.clickTo(PickerPhotosTabRobot)

    fun clickOnSaveAll() = saveAllButton.clickTo(AlbumRobot)

    fun clickOnShare() = shareButton.clickTo(ShareUserRobot)

    fun clickOnPhoto(name: String) =
        photoWithName(name).clickTo(PreviewRobot)

    fun <T : Robot> clickOnPhoto(name: String, goesTo: T) =
        photoWithName(name).clickTo(goesTo)

    fun longClickOnPhoto(fileName: String) =
        photoWithName(fileName).longClickTo(this)

    private fun photoWithName(name: String) = linkWithName(name)
        .withItemType(ItemType.File)
        .withLayoutType(LayoutType.Grid)

    fun dismissPhotoSavedToDrive(count: Int) = node.withText(
        targetContext.resources.getQuantityString(
            I18N.plurals.in_app_notification_add_to_stream_success,
            count
        ).format(count)
    ).clickTo(AlbumsTabRobot)

    fun assertMoreButtonIsDisplayed() = moreButton.await { assertIsDisplayed() }

    fun assertAlbumNameIsDisplayed(name: String) = node.withText(name)
        .await { assertIsDisplayed() }

    fun assertItemsInAlbum(count: Int) = node.withTextSubstring(
        targetContext.resources.getQuantityString(
            I18N.plurals.albums_photo_count,
            count
        ).format(count)
    ).await { assertIsDisplayed() }

    fun assertVisibleMediaItemsInAlbum(count: Int) {
        waitUntilMediaItemsCountEquals(count)
    }

    private fun waitUntilMediaItemsCountEquals(
        expectedCount: Int,
        timeout: Duration = 10.seconds,
        interval: Duration = 1.seconds,
    ) {
        val nodes = mediaItems
            .withItemType(ItemType.File)
            .withLayoutType(LayoutType.Grid)
        nodes.waitFor(timeout, interval) {
            nodes.interaction.assertCountEquals(expectedCount)
        }
    }

    fun assertCoverAlbum(name: String) = node.withLinkName(name)
        .withItemType(ItemType.File)
        .withLayoutType(LayoutType.Cover)
        .await { assertIsDisplayed() }

    fun assertEmptyAlbum() = emptyAlbumText.await { assertIsDisplayed() }

    fun assertAddToFavoriteFromForeignVolume() = favoriteFromForeignVolume.await { assertIsDisplayed() }

    fun dismissAddToAlbumStartMessage() = addToAlbumStartMessage.clickTo(AlbumRobot)

    override fun robotDisplayed() {
        albumScreen.await { assertIsDisplayed() }
    }
}
