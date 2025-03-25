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

package me.proton.core.drive.backup.domain.manager

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class BackupPermissionsManagerTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var permissionsManager: BackupPermissionsManager

    @Test
    fun `given permission granted when get permissions status then should be granted`() = runTest {
        val permissionsFlow = permissionsManager.backupPermissions

        permissionsManager.onPermissionChanged(BackupPermissions.Granted())

        assertEquals(
            BackupPermissions.Granted(),
            permissionsFlow.first(),
        )
    }

    @Test
    fun `given permission denied when get permissions status then should be denied`() = runTest {
        val permissionsFlow = permissionsManager.backupPermissions

        permissionsManager.onPermissionChanged(BackupPermissions.Denied(false))

        assertEquals(
            BackupPermissions.Denied(false),
            permissionsFlow.first(),
        )
    }
}
