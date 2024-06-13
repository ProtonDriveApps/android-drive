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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class PermissionsRoleTest {

    @Test
    fun viewer() = with(Permissions.viewer) {
        assertFalse("canExecute", canExecute)
        assertTrue("canRead", canRead)
        assertFalse("canWrite", canWrite)
        assertFalse("isAdmin", isAdmin)
        assertFalse("isOwner", isOwner)
    }

    @Test
    fun editor() = with(Permissions.editor) {
        assertFalse("canExecute", canExecute)
        assertTrue("canRead", canRead)
        assertTrue("canWrite", canWrite)
        assertFalse("isAdmin", isAdmin)
        assertFalse("isOwner", isOwner)
    }

    @Test
    fun admin() = with(Permissions.admin) {
        assertFalse("canExecute", canExecute)
        assertTrue("canRead", canRead)
        assertTrue("canWrite", canWrite)
        assertTrue("isAdmin", isAdmin)
        assertFalse("isOwner", isOwner)
    }

    @Test
    fun owner() = with(Permissions.owner) {
        assertTrue("canExecute", canExecute)
        assertTrue("canRead", canRead)
        assertTrue("canWrite", canWrite)
        assertTrue("isAdmin", isAdmin)
        assertTrue("isOwner", isOwner)
    }
}
