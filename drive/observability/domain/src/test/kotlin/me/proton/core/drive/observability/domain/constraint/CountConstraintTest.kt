/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.observability.domain.constraint

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CountConstraintTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var countConstraint: CountConstraint

    private lateinit var constraint: Constraint

    @Before
    fun before() = runTest {
        driveRule.db.myFiles {}
        constraint = countConstraint(userId, TEST_KEY, 1)
    }

    @Test
    fun `Not applied constraint should be met`() = runTest {
        // Then
        assertTrue(constraint.isMet())
    }

    @Test
    fun `Applied constraint should not be met`() = runTest {
        // When
        constraint.apply()

        // Then
        assertFalse(constraint.isMet())
    }

    companion object {
        const val TEST_KEY = "test.key"
    }
}
