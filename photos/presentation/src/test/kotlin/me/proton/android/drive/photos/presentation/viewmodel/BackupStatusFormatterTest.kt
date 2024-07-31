/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.presentation.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import me.proton.android.drive.photos.presentation.viewstate.PhotosStatusViewState
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.entity.BackupStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class BackupStatusFormatterTest {

    private val formatter = BackupStatusFormatter(
        resources = ApplicationProvider.getApplicationContext<Application>().resources,
        locale = Locale.ENGLISH
    )

    @Test
    fun disabled() {
        assertEquals(
            PhotosStatusViewState.Disabled(hasDefaultFolder = true),
            formatter.toViewState(
                backupState = BackupState(
                    isBackupEnabled = false,
                    hasDefaultFolder = true,
                    backupStatus = null
                ),
            ),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `backup enable without status should failed`() {
        formatter.toViewState(
            backupState = BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
                backupStatus = null
            ),
        )
    }

    @Test
    fun failed() {
        assertEquals(
            PhotosStatusViewState.Failed(emptyList()),
            formatter.toViewState(
                backupState = BackupState(
                    isBackupEnabled = true,
                    hasDefaultFolder = true,
                    backupStatus = BackupStatus.Failed(
                        errors = emptyList(),
                        totalBackupPhotos = 0,
                        pendingBackupPhotos = 0,
                    ),
                ),
            ),
        )
    }

    @Test
    fun preparing() {
        assertEquals(
            PhotosStatusViewState.Preparing(0F),
            formatter.toViewState(
                backupState = BackupState(
                    isBackupEnabled = true,
                    hasDefaultFolder = true,
                    backupStatus = BackupStatus.Preparing(
                        totalBackupPhotos = 10000,
                        preparingBackupPhotos = 10000,
                    )
                ),
            ),
        )
    }

    @Test
    fun `InProgress starting`() {
        assertEquals(
            PhotosStatusViewState.InProgress(0F, "10,000 items left"),
            formatter.toViewState(
                backupState = BackupState(
                    isBackupEnabled = true,
                    hasDefaultFolder = true,
                    backupStatus = BackupStatus.InProgress(
                        totalBackupPhotos = 10000,
                        pendingBackupPhotos = 10000,
                    )
                ),
            ),
        )
    }

    @Test
    fun `InProgress finishing`() {
        assertEquals(
            PhotosStatusViewState.InProgress(0.99F, "1 item left"),
            formatter.toViewState(
                backupState = BackupState(
                    isBackupEnabled = true,
                    hasDefaultFolder = true,
                    backupStatus = BackupStatus.InProgress(
                        totalBackupPhotos = 100,
                        pendingBackupPhotos = 1,
                    )
                ),
            ),
        )
    }

    @Test
    fun complete() {
        assertEquals(
            PhotosStatusViewState.Complete("10 items saved"),
            formatter.toViewState(
                backupState = BackupState(
                    isBackupEnabled = true,
                    hasDefaultFolder = true,
                    backupStatus = BackupStatus.Complete(totalBackupPhotos = 0)
                ),
                count = 10,
            ),
        )
    }

    @Test
    fun `complete without count`() {
        assertEquals(
            PhotosStatusViewState.Complete(null),
            formatter.toViewState(
                backupState = BackupState(
                    isBackupEnabled = true,
                    hasDefaultFolder = true,
                    backupStatus = BackupStatus.Complete(totalBackupPhotos = 0)
                ),
                count = null,
            ),
        )
    }

    @Test
    fun uncompleted() {
        assertEquals(
            PhotosStatusViewState.Uncompleted,
            formatter.toViewState(
                backupState = BackupState(
                    isBackupEnabled = true,
                    hasDefaultFolder = true,
                    backupStatus = BackupStatus.Uncompleted(totalBackupPhotos = 0, failedBackupPhotos = 1)
                ),
                count = 10,
            ),
        )
    }
}
