/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.base.domain.util

/**
 * Counterpart to `use` for handing a resource off: closes the receiver only when [block] fails.
 *
 * On success the resource is left open because ownership has moved to whatever [block] returned,
 * which becomes responsible for closing it.
 */
@Suppress("TooGenericExceptionCaught")
inline fun <T : AutoCloseable, R> T.closeOnFailure(block: (T) -> R): R = try {
    block(this)
} catch (throwable: Throwable) {
    try {
        close()
    } catch (closeThrowable: Throwable) {
        throwable.addSuppressed(closeThrowable)
    }
    throw throwable
}
