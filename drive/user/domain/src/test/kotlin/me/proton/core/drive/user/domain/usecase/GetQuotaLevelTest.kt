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
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.NullableUserEntity
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.user.domain.entity.QuotaLevel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GetQuotaLevelTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var getQuotaLevel: GetQuotaLevel

    @Before
    fun setUp() = runTest {
        database.db.user(NullableUserEntity(maxSpace = 500.MiB.value)) {}
        // TODO: Use hilt to build the real implementation
        val userManager = MockUserManager(database.db)
        getQuotaLevel = GetQuotaLevel(
            userManager,
            object : ConfigurationProvider {
                override val host: String = ""
                override val baseUrl: String = ""
                override val appVersionHeader: String = ""
                override val backupLeftSpace: Bytes = 25.MiB
            })
    }

    @Test
    fun nothing() = runTest {
        assertEquals(QuotaLevel.NULL, getQuotaLevel(userId).first())
    }

    @Test
    fun info() = runTest {
        database.db.userDao().setUsedSpace(userId, 250.MiB.value)

        assertEquals(QuotaLevel.INFO, getQuotaLevel(userId).first())
    }

    @Test
    fun warning() = runTest {
        database.db.userDao().setUsedSpace(userId, 400.MiB.value)

        assertEquals(QuotaLevel.WARNING, getQuotaLevel(userId).first())
    }

    @Test
    fun error() = runTest {
        database.db.userDao().setUsedSpace(userId, 490.MiB.value)

        assertEquals(QuotaLevel.ERROR, getQuotaLevel(userId).first())
    }

    @Test
    fun errorWithStorageSplit() = runTest {
        database.db.user(NullableUserEntity(maxDriveSpace = 300.MiB.value)) {}
        database.db.userDao().setUsedDriveSpace(userId, 290.MiB.value)

        assertEquals(QuotaLevel.ERROR, getQuotaLevel(userId).first())
    }
}
