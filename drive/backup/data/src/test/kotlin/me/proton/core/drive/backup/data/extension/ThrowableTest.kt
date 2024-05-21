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

package me.proton.core.drive.backup.data.extension

import android.system.ErrnoException
import android.system.OsConstants
import me.proton.core.drive.backup.domain.entity.BackupError
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.FileNotFoundException
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ThrowableTest {

    @Test
    fun throwable() {
        assertEquals(
            BackupError.Other(),
            Throwable().toBackupError()
        )
    }

    @Test
    fun fileNotFoundException() {
        assertEquals(
            BackupError.Other(),
            FileNotFoundException().toBackupError()
        )
    }

    @Test
    fun securityException() {
        assertEquals(
            BackupError.Permissions(),
            SecurityException().toBackupError()
        )
    }

    @Test
    fun enospcExceptionCause() {
        assertEquals(
            BackupError.LocalStorage(),
            IOException(ErrnoException("", OsConstants.ENOSPC)).toBackupError()
        )
    }

    @Test
    fun enospcExceptionCauseCause() {
        assertEquals(
            BackupError.LocalStorage(),
            IllegalStateException(
                IOException(
                    ErrnoException("", OsConstants.ENOSPC)
                )
            ).toBackupError()
        )
    }

    @Test
    fun enodataExceptionCause() {
        assertEquals(
            BackupError.Other(),
            IOException(ErrnoException("", OsConstants.ENODATA)).toBackupError()
        )
    }
}
