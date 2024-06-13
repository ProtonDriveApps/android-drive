/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.base.presentation.extension

import me.proton.core.drive.base.presentation.state.ListContentState

inline fun ListContentState.onLoading(action: () -> Unit) = apply {
    if (this == ListContentState.Loading) {
        action()
    }
}

inline fun ListContentState.onEmpty(action: (ListContentState.Empty) -> Unit) = apply {
    if (this is ListContentState.Empty) {
        action(this)
    }
}

inline fun ListContentState.onError(action: (ListContentState.Error) -> Unit) = apply {
    if (this is ListContentState.Error) {
        action(this)
    }
}

inline fun ListContentState.onContent(action: (ListContentState.Content) -> Unit) = apply {
    if (this is ListContentState.Content) {
        action(this)
    }
}
