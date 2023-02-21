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
package me.proton.core.drive.base.domain.entity

import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource

// There are two classes of errors: permanent and temporary
sealed class DriveError(open val message: String? = null) {
    sealed class Permanent(override val message: String? = null) : DriveError() {
        object NotFound : Permanent()
        class InsufficientScope(message: String?) : Permanent(message)
    }
    sealed class Temporary(override val message: String? = null) : DriveError() {
        class Unknown(message: String?) : Temporary(message)
    }
}

inline fun <T> DataResult<T>.onProcessing(action: () -> Unit): DataResult<T> {
    if (this is DataResult.Processing) action()
    return this
}

inline fun <T> DataResult<T>.onError(action: (driveError: DriveError) -> Unit): DataResult<T> {
    if (this is DataResult.Error) {
        val driveError = when (this) {
            is DataResult.Error.Local -> DriveError.Permanent.NotFound
            is DataResult.Error.Remote -> when (this.httpCode) {
                403 -> DriveError.Permanent.InsufficientScope(this.message)
                404 -> DriveError.Permanent.NotFound
                else -> DriveError.Temporary.Unknown(this.message)
            }
            else -> DriveError.Temporary.Unknown(this.message)
        }
        action(driveError)
    }
    return this
}
