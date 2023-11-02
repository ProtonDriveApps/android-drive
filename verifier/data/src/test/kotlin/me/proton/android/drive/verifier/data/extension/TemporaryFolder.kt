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

package me.proton.android.drive.verifier.data.extension

import me.proton.core.drive.base.domain.entity.Bytes
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

fun TemporaryFolder.createFile(size: Bytes): File =
    File(root, "${size.value}_byte(s)_${UUID.randomUUID()}.txt").apply {
        if (exists()) { delete() }
        createNewFile()
        appendText(
            (0 until size.value.toInt()).joinToString(separator = "") { "a" }
        )
    }
