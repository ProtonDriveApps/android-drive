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

package me.proton.core.drive.base.data.db

class SlowDatabaseOperationException(
    val database: String,
    val operation: Operation,
    val durationMs: Long,
    val isMainThread: Boolean,
    val sql: String?,
    message: String,
) : RuntimeException(message) {

    val fingerprint: List<String> get() = listOfNotNull(
        "slow-database",
        database,
        operation.name,
        "main-thread".takeIf { isMainThread },
        sql?.let { Integer.toHexString(it.hashCode()) },
    )

    enum class Operation {
        READ,
        STATEMENT,
        LOCK_WAIT,
        WRITE_TRANSACTION,
    }
}
