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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.manager.BackupPermissionsManagerImpl
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupPermissionsManagerTest {

    private val permissionsManager: BackupPermissionsManager = BackupPermissionsManagerImpl(
        ApplicationProvider.getApplicationContext<Application>()
    )

    @Test
    fun `given permission granted when get permissions status then should be granted`() = runTest {
        val permissionsFlow = permissionsManager.backupPermissions

        permissionsManager.onPermissionChanged(BackupPermissions.Granted)

        assertEquals(
            BackupPermissions.Granted,
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
