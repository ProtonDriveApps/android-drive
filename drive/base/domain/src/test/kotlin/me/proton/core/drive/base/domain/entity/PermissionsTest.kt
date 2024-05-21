/*
 * Copyright (c) 2022-2024 Proton AG.
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

package me.proton.core.drive.base.domain.entity

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class PermissionsTest {

    @Test
    fun `permission value 0`() {
        // region Given
        val value = 0L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertFalse(canRead)
        assertFalse(canWrite)
        assertFalse(canExecute)
        assertFalse(isAdmin)
        // endregion
    }

    @Test
    fun `permission value 1`() {
        // region Given
        val value = 1L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertFalse(canRead)
        assertFalse(canWrite)
        assertTrue(canExecute)
        assertFalse(isAdmin)
        // endregion
    }

    @Test
    fun `permission value 2`() {
        // region Given
        val value = 2L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertFalse(canRead)
        assertTrue(canWrite)
        assertFalse(canExecute)
        assertFalse(isAdmin)
        // endregion
    }

    @Test
    fun `permission value 3`() {
        // region Given
        val value = 3L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertFalse(canRead)
        assertTrue(canWrite)
        assertTrue(canExecute)
        assertFalse(isAdmin)
        // endregion
    }

    @Test
    fun `permission value 4`() {
        // region Given
        val value = 4L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertTrue(canRead)
        assertFalse(canWrite)
        assertFalse(canExecute)
        assertFalse(isAdmin)
        // endregion
    }

    @Test
    fun `permission value 5`() {
        // region Given
        val value = 5L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertTrue(canRead)
        assertFalse(canWrite)
        assertTrue(canExecute)
        assertFalse(isAdmin)
        // endregion
    }

    @Test
    fun `permission value 6`() {
        // region Given
        val value = 6L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertTrue(canRead)
        assertTrue(canWrite)
        assertFalse(canExecute)
        assertFalse(isAdmin)
        // endregion
    }

    @Test
    fun `permission value 7`() {
        // region Given
        val value = 7L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertTrue(canRead)
        assertTrue(canWrite)
        assertTrue(canExecute)
        assertFalse(isAdmin)
        // endregion
    }

    @Test
    fun `permission value 23`() {
        // region Given
        val value = 23L
        val permissions = Permissions(value)
        // endregion
        // region When
        val canRead = permissions.canRead
        val canWrite = permissions.canWrite
        val canExecute = permissions.canExecute
        val isAdmin = permissions.isAdmin
        // endregion
        // region Then
        assertTrue(canRead)
        assertTrue(canWrite)
        assertTrue(canExecute)
        assertTrue(isAdmin)
        // endregion
    }
}
