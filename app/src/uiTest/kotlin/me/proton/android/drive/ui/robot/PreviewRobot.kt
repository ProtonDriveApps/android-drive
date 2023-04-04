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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.component.TopAppBarComponentTestTag
import me.proton.core.drive.files.preview.presentation.component.PreviewComponentTestTag

object PreviewRobot : Robot {

    private val previewScreen get() = node.withTag(PreviewComponentTestTag.screen)
    private val contextualButton get() = node.withContentDescription(R.string.content_description_more_options)

    fun clickOnContextualButton() = contextualButton.clickTo(FileFolderOptionsRobot)

    fun topBarWithTextDisplayed(itemName: String) {
        node.withTag(TopAppBarComponentTestTag.appBar).withTextSubstring(itemName).assertExists()
    }

    override fun robotDisplayed() {
        previewScreen.assertIsDisplayed()
    }
}
