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

import androidx.compose.ui.test.SemanticsMatcher
import me.proton.core.drive.file.info.presentation.FileInfoTestTag
import me.proton.test.fusion.Fusion.node
import kotlin.time.Duration.Companion.seconds

object DetailsRobot : NavigationBarRobot, Robot {
    private val detailsContent get() = node.withTag(FileInfoTestTag.screen)

    fun hasHeaderTitle(title: String) = node
        .addSemanticMatcher(SemanticsMatcher.expectValue(FileInfoTestTag.Header.Title, title))
        .await { assertIsDisplayed() }

    fun hasHeaderWithIconType(iconType: FileInfoTestTag.Header.HeaderIconType) = node
        .addSemanticMatcher(SemanticsMatcher.expectValue(FileInfoTestTag.Header.IconType, iconType))
        .await { assertIsDisplayed() }

    fun hasInfoItem(name: String, value: String) {
        detailsContent.scrollTo(node.withText(name))
        node
            .withContentDescription("$name: $value")
            .await { assertIsDisplayed() }
    }

    fun hasNotInfoItem(name: String) {
        node.withText(name).await { assertDoesNotExist() }
    }

    override fun robotDisplayed() {
        detailsContent.await(30.seconds) { assertIsDisplayed() }
    }
}
