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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.component.TopAppBarComponentTestTag
import me.proton.core.drive.files.preview.presentation.component.PreviewComponentTestTag
import me.proton.core.test.android.instrumented.utils.StringUtils

object PreviewRobot : Robot {

    private val contextualButtonDesc = StringUtils.stringFromResource(R.string.content_description_more_options)
    private val previewScreen get() = node(hasTestTag(PreviewComponentTestTag.screen))
    private val contextualButton get() = node(hasContentDescription(contextualButtonDesc))

    fun clickOnContextualButton() = contextualButton.tryToClickAndGoTo(FileFolderOptionsRobot)

    fun topBarWithTextDisplayed(itemName: String) {
        node(hasTestTag(TopAppBarComponentTestTag.appBar) and hasText(text = itemName, substring = true)).assertExists()
    }

    override fun robotDisplayed() {
        previewScreen.assertIsDisplayed()
    }
}
