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

package me.proton.core.drive.sorting.presentation

import android.Manifest
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.presentation.entity.SortingOption
import me.proton.core.drive.sorting.presentation.entity.toSortingOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import me.proton.core.drive.i18n.R as I18N

@RunWith(AndroidJUnit4::class)
class SortingListTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private val context: Context get() = composeTestRule.activity

    @Test
    fun test() {
        // region Arrange
        val options = Sorting(By.NAME, Direction.ASCENDING).toSortingOptions()
        val selectedOptions = mutableListOf<SortingOption>()
        val onSortingOption: (SortingOption) -> Unit = { option -> selectedOptions.add(option) }
        // endregion
        // region Act
        composeTestRule.setContent {
            SortingList(
                sortingOptions = options,
                onSortingOption = onSortingOption,
            )
        }
        composeTestRule.onNodeWithText(context.getString(I18N.string.common_name)).performClick()
        composeTestRule.onNodeWithText(context.getString(I18N.string.title_last_modified)).performClick()
        composeTestRule.onNodeWithText(context.getString(I18N.string.title_size)).performClick()
        composeTestRule.onNodeWithText(context.getString(I18N.string.title_file_type)).performClick()
        // endregion
        // region Assert
        assert(selectedOptions[0].toggleDirection.direction == Direction.DESCENDING)
        assert(selectedOptions[0].toggleDirection.by == By.NAME)
        assert(selectedOptions[1].toggleDirection.direction == Direction.ASCENDING)
        assert(selectedOptions[1].toggleDirection.by == By.LAST_MODIFIED)
        assert(selectedOptions[2].toggleDirection.direction == Direction.ASCENDING)
        assert(selectedOptions[2].toggleDirection.by == By.SIZE)
        assert(selectedOptions[3].toggleDirection.direction == Direction.ASCENDING)
        assert(selectedOptions[3].toggleDirection.by == By.TYPE)
        // endregion
    }
}
