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

package me.proton.core.drive.stats.domain.usecase

import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.stats.data.repository.BackupStatsRepositoryImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupStatsTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var setOrIgnoreInitialBackup: SetOrIgnoreInitialBackup
    private lateinit var isInitialBackup: IsInitialBackup

    @Before
    fun setUp() = runTest {
        folderId = database.db.photo {}
        val repository = BackupStatsRepositoryImpl(database.db)
        setOrIgnoreInitialBackup = SetOrIgnoreInitialBackup(repository)
        isInitialBackup = IsInitialBackup(repository)
    }

    @Test
    fun empty() = runTest {
        assertTrue(isInitialBackup(folderId).getOrThrow())
    }

    @Test
    fun create() = runTest {
        setOrIgnoreInitialBackup(folderId).getOrThrow()
        assertFalse(isInitialBackup(folderId).getOrThrow())
    }
}
