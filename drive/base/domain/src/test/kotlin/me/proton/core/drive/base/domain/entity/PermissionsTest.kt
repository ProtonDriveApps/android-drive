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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
class PermissionsTest(
    private val value: Long,
    private val rights: Rights,
) {

    @Test
    fun permission() {
        with(Permissions(value)) {
            assertEquals("canExecute", rights.canExecute, canExecute)
            assertEquals("canRead", rights.canRead, canRead)
            assertEquals("canWrite", rights.canWrite, canWrite)
            assertEquals("isAdmin", rights.isAdmin, isAdmin)
            assertEquals("isOwner", rights.isOwner, isOwner)
        }
    }

    companion object {
        data class Rights(
            val isOwner: Boolean = false,
            val isAdmin: Boolean = false,
            val canWrite: Boolean = false,
            val canRead: Boolean = false,
            val canExecute: Boolean = false,
        )

        @get:Parameterized.Parameters(name = "Permission({0}) has rights: {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(0, Rights()),
            arrayOf(1, Rights(canExecute = true)),
            arrayOf(2, Rights(canWrite = true)),
            arrayOf(3, Rights(canExecute = true, canWrite = true)),
            arrayOf(4, Rights(canRead = true)),
            arrayOf(5, Rights(canExecute = true, canRead = true)),
            arrayOf(6, Rights(canWrite = true,  canRead = true)),
            arrayOf(7, Rights(canExecute = true, canWrite = true, canRead = true)),
            arrayOf(23, Rights(canExecute = true, canWrite = true, canRead = true, isAdmin = true)),
            arrayOf(
                55,
                Rights(
                    canExecute = true,
                    canWrite = true,
                    canRead = true,
                    isAdmin = true,
                    isOwner = true
                )
            ),
        )
    }
}
