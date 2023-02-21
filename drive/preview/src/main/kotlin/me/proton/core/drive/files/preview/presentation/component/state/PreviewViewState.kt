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
package me.proton.core.drive.files.preview.presentation.component.state

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.base.presentation.entity.FileTypeCategory

@Immutable
data class PreviewViewState(
    val navigationIconResId: Int,
    val isFullscreen: Flow<Boolean>,
    val previewContentState: PreviewContentState,
    val items: List<Item>,
    val currentIndex: Int,
) {

    @Immutable
    data class Item(
        val key: String,
        val title: String,
        val category: FileTypeCategory,
        val contentState: Flow<ContentState>,
        val isTitleEncrypted: Boolean = false,
    )
}

sealed class ZoomEffect {
    object Reset : ZoomEffect()
}

sealed interface PreviewContentState {
    object Loading : PreviewContentState
    object Empty : PreviewContentState
    object Content : PreviewContentState
}