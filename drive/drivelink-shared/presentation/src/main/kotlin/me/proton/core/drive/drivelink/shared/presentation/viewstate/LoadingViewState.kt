/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.drivelink.shared.presentation.viewstate

import androidx.compose.runtime.Immutable
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Immutable
sealed class LoadingViewState {
    object Initial : LoadingViewState()
    data class Loading(val message: String) : LoadingViewState()
    sealed class Error(open val message: String, open val defer: Duration) : LoadingViewState() {
        data class Retryable(override val message: String) : Error(message, 0.seconds)
        data class NonRetryable(override val message: String, override val defer: Duration) : Error(message, defer)
    }
    data class Available(
        val driveLink: DriveLink,
    ) : LoadingViewState()
}
