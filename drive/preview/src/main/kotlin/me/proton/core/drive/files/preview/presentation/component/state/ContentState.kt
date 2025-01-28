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

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO

sealed class ContentState {
    object NotFound : ContentState()
    data class Downloading(val progress: Flow<Percentage>?, val thumbnail: ThumbnailVO? = null) : ContentState()
    data class Decrypting(val thumbnail: ThumbnailVO? = null) : ContentState()
    data class Available(val source: Any, val thumbnailSource: Any? = null) : ContentState()
    sealed class Error : ContentState() {
        data class Retryable(val messageResId: Int, val actionResId: Int, val action: () -> Unit) : Error()
        data class NonRetryable(val message: String?, val messageResId: Int) : Error()
    }

    override fun toString(): String {
        return "${javaClass.simpleName}${when(this) {
            is Downloading -> "(${if (progress == null) "null" else "flow"})"
            is Available -> when(source) {
                is Uri -> "(${source.path})"
                is ThumbnailVO -> "(${source.revisionId}_${source.thumbnailId.type})"
                else -> error("Unhandled source")
            }
            else -> ""
        }}"
    }
}
