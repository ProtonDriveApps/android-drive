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

package me.proton.android.drive.ui.extension

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import me.proton.core.drive.files.presentation.extension.DriveLinkSemanticsProperties
import me.proton.core.drive.files.presentation.extension.ItemType
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.SemanticsDownloadState
import me.proton.test.fusion.ui.compose.wrappers.NodeAssertions


fun NodeAssertions.assertHasThumbnail(expectedValue: Boolean) = apply {
    interaction.assert(SemanticsMatcher.expectValue(DriveLinkSemanticsProperties.HasThumbnail, expectedValue))
}

fun NodeAssertions.assertDownloadState(expectedValue: SemanticsDownloadState) = apply {
    interaction.assert(SemanticsMatcher.expectValue(DriveLinkSemanticsProperties.DownloadState, expectedValue))
}

fun NodeAssertions.assertHasLayoutType(expectedValue: LayoutType) = apply {
    interaction.assert(SemanticsMatcher.expectValue(DriveLinkSemanticsProperties.LayoutType, expectedValue))
}

fun NodeAssertions.assertHasItemType(expectedValue: ItemType) = apply {
    interaction.assert(SemanticsMatcher.expectValue(DriveLinkSemanticsProperties.ItemType, expectedValue))
}

fun NodeAssertions.assertIsSharedByLink(expectedValue: Boolean) = apply {
    interaction.assert(SemanticsMatcher.expectValue(DriveLinkSemanticsProperties.IsSharedByLink, expectedValue))
}

fun NodeAssertions.assertIsSharedWithUsers(expectedValue: Boolean) = apply {
    interaction.assert(SemanticsMatcher.expectValue(DriveLinkSemanticsProperties.IsSharedWithUsers, expectedValue))
}

fun NodeAssertions.assertIsFavorite(expectedValue: Boolean) = apply {
    interaction.assert(SemanticsMatcher.expectValue(DriveLinkSemanticsProperties.IsFavorite, expectedValue))
}

// Remove after TPE-334 is resolved
fun NodeAssertions.doesNotExist() =
    try {
        assertIsNotDisplayed()
    } catch (error: AssertionError) {
        assertDoesNotExist()
    }
