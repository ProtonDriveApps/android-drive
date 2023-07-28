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

package me.proton.core.drive.file.info.presentation.extension

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.semantics.semantics
import coil.compose.ImagePainter
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.file.info.presentation.FileInfoTestTag

fun Modifier.headerSemantics(driveLink: DriveLink, painter: Painter, mergeDescendants: Boolean = true) =
    semantics(mergeDescendants = mergeDescendants) {
        this[FileInfoTestTag.Header.Title] = driveLink.name
        this[FileInfoTestTag.Header.IconType] = when (painter) {
            is VectorPainter -> FileInfoTestTag.Header.HeaderIconType.PLACEHOLDER
            is ImagePainter -> FileInfoTestTag.Header.HeaderIconType.THUMBNAIL
            else -> FileInfoTestTag.Header.HeaderIconType.UNKNOWN
        }
    }
