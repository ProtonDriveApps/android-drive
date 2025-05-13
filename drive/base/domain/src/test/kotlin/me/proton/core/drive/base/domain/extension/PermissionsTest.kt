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

package me.proton.core.drive.base.domain.extension

import me.proton.core.drive.base.domain.entity.Permissions
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
class PermissionsTest(
    private val value: Long,
    private val isViewerOrEditorOnly: Boolean,
) {

    @Test
    fun permission() {
        assertEquals(
            "canReadOrWriteOnly",
            isViewerOrEditorOnly,
            Permissions(value).isViewerOrEditorOnly
        )
    }

    companion object {

        @get:Parameterized.Parameters(name = "Permission({0}) is Viewer or Editor only: {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(0, false),
            arrayOf(1, false),
            arrayOf(2, false),
            arrayOf(3, false),
            arrayOf(4, true),
            arrayOf(5, true),
            arrayOf(6, true),
            arrayOf(7, true),
            arrayOf(23, false),
            arrayOf(55, false),
        )
    }
}
