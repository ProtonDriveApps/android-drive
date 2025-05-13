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

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.ui.extension.assertDownloadState
import me.proton.android.drive.ui.extension.assertHasItemType
import me.proton.android.drive.ui.extension.assertHasLayoutType
import me.proton.android.drive.ui.extension.assertHasThumbnail
import me.proton.android.drive.ui.extension.assertIsFavorite
import me.proton.android.drive.ui.extension.assertIsSharedByLink
import me.proton.android.drive.ui.extension.assertIsSharedWithUsers
import me.proton.android.drive.ui.extension.doesNotExist
import me.proton.android.drive.ui.extension.withItemType
import me.proton.android.drive.ui.extension.withLayoutType
import me.proton.android.drive.ui.extension.withLinkName
import me.proton.core.drive.files.presentation.component.FilesTestTag
import me.proton.core.drive.files.presentation.component.files.FilesListHeaderTestTag
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import me.proton.test.fusion.Fusion.allNodes
import me.proton.test.fusion.Fusion.node
import org.junit.Assert.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N

@Suppress("TooManyFunctions")
interface LinksRobot : PullToRefreshRobot, Robot {

    val filesContent get() = node.withTag(FilesTestTag.content)

    private val layoutSwitcher get() = node.withTag(FilesListHeaderTestTag.layoutSwitcher)
    private val letsFillThisFolderMessage get() = node.withText(I18N.string.title_empty_folder)
    private val tapTheButtonToStartAddingFilesMessage get() = node.withText(I18N.string.description_empty_my_files)
    private val uploadButton get() = node.withText(I18N.string.action_empty_files_add_files)
    private val selectAll get() = node.withContentDescription(I18N.string.content_description_select_all)

    private val selectedOptionsButton
        get() = node
            .withContentDescription(I18N.string.content_description_selected_options)

    fun linkWithName(name: String) = node.withLinkName(name).isClickable()

    private fun moreButton(name: String) =
        node.withTag(FilesTestTag.moreButton).hasAncestor(linkWithName(name))

    fun clickLayoutSwitcher() = layoutSwitcher.clickTo(FilesTabRobot)

    fun clickSelectAll() = selectAll.clickTo(FilesTabRobot)

    fun scrollToItemWithName(itemName: String): LinksRobot = apply {
        filesContent.scrollTo(linkWithName(itemName))
    }

    fun clickOptions() =
        selectedOptionsButton
            .clickTo(FileFolderOptionsRobot)

    fun clickMultipleOptions() =
        selectedOptionsButton
            .clickTo(MultipleFileFolderOptionsRobot)

    fun <T : Robot> clickOnItem(
        name: String,
        layoutType: LayoutType,
        itemType: ItemType,
        goesTo: T
    ) =
        linkWithName(name)
            .withLayoutType(layoutType)
            .withItemType(itemType)
            .clickTo(goesTo)

    fun clickOnFolder(name: String, layoutType: LayoutType = LayoutType.List): FilesTabRobot =
        clickOnItem(name, layoutType, ItemType.Folder, FilesTabRobot)

    fun <T : Robot> clickOnFolder(
        name: String,
        layoutType: LayoutType = LayoutType.List,
        goesTo: T,
    ): T =
        clickOnItem(name, layoutType, ItemType.Folder, goesTo)

    fun clickOnFolderToMove(name: String) =
        clickOnItem(name, LayoutType.List, ItemType.Folder, MoveToFolderRobot)

    fun clickMoreOnItem(name: String) =
        moreButton(name).clickTo(FileFolderOptionsRobot)

    fun clickOnFile(name: String, layoutType: LayoutType = LayoutType.List) =
        clickOnItem(name, layoutType, ItemType.File, PreviewRobot)

    fun clickOnAlbum(name: String, layoutType: LayoutType = LayoutType.Grid) =
        clickOnItem(name, layoutType, ItemType.Album, AlbumRobot)

    fun <T : Robot> clickOnAlbum(name: String, goesTo: T, layoutType: LayoutType = LayoutType.Grid) =
        clickOnItem(name, layoutType, ItemType.Album, goesTo)

    fun <T : Robot> clickOnUndo(after: Duration = 0.seconds, goesTo: T): T = runBlocking {
        delay(after)
        node.withText(I18N.string.common_undo_action).clickTo(goesTo)
    }

    fun longClickOnItem(name: String) =
        linkWithName(name).longClickTo(FilesTabRobot)

    fun <T : Robot> longClickOnItem(name: String, goesTo: T) =
        linkWithName(name).longClickTo(goesTo)

    fun itemIsDisplayed(
        name: String,
        layoutType: LayoutType? = null,
        itemType: ItemType? = null,
        hasThumbnail: Boolean? = null,
        isSharedByLink: Boolean? = null,
        isSharedWithUsers: Boolean? = null,
        isFavorite: Boolean? = null,
        downloadState: SemanticsDownloadState? = null
    ) {
        linkWithName(name)
            .await {
                layoutType?.let { assertHasLayoutType(it) }
                itemType?.let { assertHasItemType(it) }
                hasThumbnail?.let { assertHasThumbnail(it) }
                isSharedByLink?.let { assertIsSharedByLink(it) }
                isSharedWithUsers?.let { assertIsSharedWithUsers(it) }
                isFavorite?.let { assertIsFavorite(it) }
                downloadState?.let { assertDownloadState(it) }
                assertIsDisplayed()
            }
    }

    fun letsFillThisFolderMessageIsDisplayed() {
        letsFillThisFolderMessage.await { assertIsDisplayed() }
        tapTheButtonToStartAddingFilesMessage.await { assertIsDisplayed() }
        uploadButton.await { assertIsDisplayed() }
    }

    fun itemIsNotDisplayed(vararg names: String) {
        names.forEach { name ->
            linkWithName(name)
                .await {
                    doesNotExist()
                }
        }
    }

    fun swipeUpContent(): LinksRobot = apply {
        node.withTag(FilesTestTag.content).interaction.performTouchInput {
            swipe(start = center, end = center.copy(y = 0f), durationMillis = 500)
        }
    }

    fun getItemsInTrash(): MutableSet<String> {
        val collectedTexts = mutableSetOf<String>()
        var canScroll = true
        val maxScrolls = 5
        var scrolls = 0

        while (canScroll && scrolls < maxScrolls) {
            val beforeScrollContents = captureVisibleTexts()
            collectedTexts.addAll(beforeScrollContents)

            swipeUpContent()

            val afterScrollContents = captureVisibleTexts()
            collectedTexts.addAll(afterScrollContents)

            if (afterScrollContents == beforeScrollContents) {
                canScroll = false
            }

            scrolls++
        }
        return collectedTexts
    }

    fun numberOfItemsInTrash(itemsExpectedInTrashList: Int) {
        assertEquals(itemsExpectedInTrashList, getItemsInTrash().count())
    }

    private fun captureVisibleTexts(): Set<String> {
        return allNodes
            .withTag(FilesTestTag.listDetailsTitle)
            .interaction.fetchSemanticsNodes()
            .mapNotNull {
                it.config.getOrNull(SemanticsProperties.Text)?.joinToString("")
            }
            .toSet()
    }
}
