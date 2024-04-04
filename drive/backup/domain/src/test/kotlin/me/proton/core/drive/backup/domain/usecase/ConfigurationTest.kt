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

package me.proton.core.drive.backup.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupConfiguration
import me.proton.core.drive.backup.domain.entity.BackupNetworkType
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.manager.started
import me.proton.core.drive.backup.domain.manager.stopped
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ConfigurationTest {
    @get:Rule
    var driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var getConfiguration: GetConfiguration

    @Inject
    lateinit var updateConfiguration: UpdateConfiguration

    @Inject
    lateinit var observeConfigurationChanges: ObserveConfigurationChanges

    @Inject
    lateinit var backupManager: BackupManager

    private lateinit var connectedConfiguration: BackupConfiguration
    private lateinit var unmeteredConfiguration: BackupConfiguration

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
        connectedConfiguration = BackupConfiguration(folderId, BackupNetworkType.CONNECTED)
        unmeteredConfiguration = BackupConfiguration(folderId, BackupNetworkType.UNMETERED)
    }

    @Test
    fun nothing() = runTest {
        assertNull(getConfiguration(folderId).first())
    }

    @Test
    fun update() = runTest {
        updateConfiguration(connectedConfiguration).getOrThrow()

        assertEquals(connectedConfiguration, getConfiguration(folderId).first())
    }

    @Test
    fun `Given no configuration when observes a new configuration should do nothing`() =
        runTest {
            val job = observeConfigurationChanges(userId).take(1).launchIn(this)

            updateConfiguration(connectedConfiguration).getOrThrow()
            job.join()

            assertFalse(backupManager.stopped)
            assertFalse(backupManager.started)
        }


    @Test
    fun `Given configuration when observes a new configuration should stop and start the backup`() =
        runTest {
            val job = observeConfigurationChanges(userId).take(2).launchIn(this)

            updateConfiguration(connectedConfiguration).getOrThrow()
            // make sure connected was observe, else the test is too fast and will skip it.
            assertEquals(connectedConfiguration, getConfiguration(folderId).first())
            updateConfiguration(unmeteredConfiguration).getOrThrow()
            job.join()

            assertTrue(backupManager.stopped)
            assertTrue(backupManager.started)
        }

    @Test
    fun `Given configuration when observes the same configuration should no nothing`() = runTest {
        val job = observeConfigurationChanges(userId).take(1).launchIn(this)

        updateConfiguration(connectedConfiguration).getOrThrow()
        updateConfiguration(connectedConfiguration).getOrThrow()
        job.join()

        assertFalse(backupManager.stopped)
        assertFalse(backupManager.started)
    }

}
