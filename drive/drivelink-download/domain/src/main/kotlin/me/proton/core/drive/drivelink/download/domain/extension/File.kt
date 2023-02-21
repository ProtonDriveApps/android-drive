/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.drivelink.download.domain.extension

import java.io.File

internal fun File.isParent(possibleChild: File): Boolean {
    var parent: File? = possibleChild
    while (parent != null) {
        if (parent == this) {
            return true
        }
        parent = parent.parentFile
    }
    return false
}

fun File.moveTo(target: File) {
    if (!target.exists()) {
        target.mkdirs()
    }
    copyRecursively(target, true)
    deleteRecursively()
}

fun File.changeParent(folder: File): File {
    if (!folder.exists()) {
        folder.mkdirs()
    }
    require(folder.isDirectory) { "Provided File object must be a directory" }
    if (folder.isParent(this)) {
        return this
    }
    return File(folder, name).also { newPath ->
        if (!newPath.exists()) {
            newPath.createNewFile()
        }
        moveTo(newPath)
    }
}
