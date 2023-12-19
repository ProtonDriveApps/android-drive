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

package me.proton.android.drive.photos.presentation.state

import android.net.Uri

sealed interface LibraryFoldersState {
    data class Content(
        val title: String,
        val description: String,
        val folders: List<LibraryFolder>,
    ) : LibraryFoldersState
    data object NoPermissions : LibraryFoldersState
}

sealed interface LibraryFolder {
    val id: Int
    val name: String

    data class Entry(
        override val id: Int,
        override val name: String,
        val description: String,
        val uri: Uri?,
        val enabled: Boolean,
    ) : LibraryFolder

    data class NotFound(
        override val name: String,
        val description: String,
    ) : LibraryFolder {
        override val id: Int
            get() = name.hashCode()
    }
}
