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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.presentation.state.toSortingViewState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import me.proton.core.drive.sorting.domain.entity.Sorting as SortingEntity

@RunWith(AndroidJUnit4::class)
class SortingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun clickOnSorting() {
        // region Arrange
        var wasClicked = false
        val onSorting = { wasClicked = true }
        assert(!wasClicked)
        // endregion
        // region Act
        composeTestRule.setContent {
            Sorting(
                viewState = SortingEntity(
                    By.NAME,
                    Direction.ASCENDING,
                ).toSortingViewState(),
                onSorting = onSorting,
                modifier = Modifier.testTag("sorting")
            )
        }
        composeTestRule.onNodeWithTag("sorting").performClick()
        // endregion
        // region Assert
        assert(wasClicked)
        // endregion
    }
}
