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

import androidx.compose.ui.semantics.SemanticsActions.PageDown
import androidx.compose.ui.semantics.SemanticsActions.PageLeft
import androidx.compose.ui.semantics.SemanticsActions.PageRight
import androidx.compose.ui.semantics.SemanticsActions.PageUp
import androidx.compose.ui.test.performSemanticsAction
import me.proton.core.drive.base.presentation.component.TopAppBarComponentTestTag
import me.proton.core.drive.files.preview.presentation.component.ImagePreviewComponentTestTag
import me.proton.core.drive.files.preview.presentation.component.PreviewComponentTestTag
import me.proton.test.fusion.Fusion.allNodes
import me.proton.test.fusion.Fusion.node
import me.proton.test.fusion.ui.common.enums.SwipeDirection
import me.proton.test.fusion.ui.compose.ComposeWaiter.waitFor
import me.proton.test.fusion.ui.compose.wrappers.NodeActions
import me.proton.core.drive.i18n.R as I18N

object PreviewRobot : NavigationBarRobot {

    private val previewScreen get() = node.withTag(PreviewComponentTestTag.screen)
    private val contextualButton get() = node.withContentDescription(I18N.string.content_description_more_options)
    private val preview get() = allNodes.withTag(ImagePreviewComponentTestTag.image).onFirst()
    private val pager get() = node.withTag(PreviewComponentTestTag.pager)
    private val mediaPreview get() = node.withTag(PreviewComponentTestTag.mediaPreview)

    fun clickOnContextualButton() = contextualButton.clickTo(FileFolderOptionsRobot)

    fun topBarWithTextDisplayed(itemName: String) {
        node.withTag(TopAppBarComponentTestTag.appBar)
            .withText(itemName)
            .await {
                assertIsDisplayed()
            }
    }

    fun assertPreviewIsDisplayed(itemName: String) {
        preview.await { assertIsDisplayed() }
        topBarWithTextDisplayed(itemName)
    }

    fun assertMediaPreviewDisplayed(itemName: String) {
        mediaPreview.await { assertIsDisplayed() }
        topBarWithTextDisplayed(itemName)
    }

    fun swipePage(direction: SwipeDirection) = apply {
        pager.swipePage(direction)
    }

    override fun robotDisplayed() {
        previewScreen.assertIsDisplayed()
    }
}

fun NodeActions.swipePage(direction: SwipeDirection) = apply {
    val action = when(direction) {
        SwipeDirection.Left -> PageRight
        SwipeDirection.Right -> PageLeft
        SwipeDirection.Up -> PageDown
        SwipeDirection.Down -> PageUp
    }
    waitFor {
        interaction.performSemanticsAction(action)
    }
}