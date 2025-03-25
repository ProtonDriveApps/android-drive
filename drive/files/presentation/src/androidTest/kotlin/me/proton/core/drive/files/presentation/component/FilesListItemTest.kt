/*
 * Copyright (c) 2021-2023 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.files.presentation.component

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import me.proton.core.drive.files.presentation.component.files.FilesListItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilesListItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val links = listOf(
        BASE_FILE_LINK.copy(id = BASE_FILE_LINK.id.copy(id = "abc"), name = "Short name"),
        BASE_FILE_LINK.copy(
            id = BASE_FILE_LINK.id.copy(id = "def"),
            name = "Lets put here some very long text that should be ellipsized to fit into single line"
        )
    )

    @Test
    fun singleLineTitle() {
        composeTestRule.setContent {
            Column {
                links.forEach { link ->
                    FilesListItem(
                        link.toDriveLink(),
                        onClick = {},
                        onLongClick = {},
                        onMoreOptionsClick = {},
                        isTextEnabled = { true },
                        isClickEnabled = { true },
                        modifier = Modifier.testTag(link.id.id),
                    )
                }
            }
        }

        val shortHeight = composeTestRule.onNode(hasTestTag("abc"), true)
            .fetchSemanticsNode()
            .size.height
        val longHeight = composeTestRule.onNode(hasTestTag("def"), true)
            .fetchSemanticsNode()
            .size.height
        assert(shortHeight != 0) { "Short name height is 0" }
        assert(shortHeight == longHeight) { "Short name height ($shortHeight) does not match long name height ($longHeight)" }
    }
}
