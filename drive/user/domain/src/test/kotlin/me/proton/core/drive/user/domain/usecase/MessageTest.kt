/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.user.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.user.data.repository.QuotaRepositoryImpl
import me.proton.core.drive.user.domain.entity.QuotaLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MessageTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var hasCanceledQuotaMessages: HasCanceledQuotaMessages
    private lateinit var cancelQuotaMessage: CancelQuotaMessage

    @Before
    fun setUp() = runTest {
        database.db.user {}
        val repository = QuotaRepositoryImpl(database.db)
        // TODO: Use hilt to build the real implementation
        val userManager = MockUserManager(database.db)
        hasCanceledQuotaMessages =
            HasCanceledQuotaMessages(userManager, repository)
        cancelQuotaMessage =
            CancelQuotaMessage(GetUserMaxSpace(userManager), repository)
    }

    @Test
    fun empty() = runTest {
        assertFalse(hasCanceledQuotaMessages(userId,QuotaLevel.INFO).first())
    }

    @Test
    fun canceledSameLevel() = runTest {
        cancelQuotaMessage(userId,QuotaLevel.INFO).getOrThrow()
        assertTrue(hasCanceledQuotaMessages(userId,QuotaLevel.INFO).first())
    }

    @Test
    fun canceledAnotherLevel() = runTest {
        cancelQuotaMessage(userId,QuotaLevel.INFO).getOrThrow()
        assertFalse(hasCanceledQuotaMessages(userId,QuotaLevel.WARNING).first())
    }

}
