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
package me.proton.core.drive.base.domain.log

/**
 * Records when a critical flow accepts an item and when the item reaches a terminal state, so the
 * invariant check after an end-to-end run can tell that no accepted item was lost
 * (.safeguards/extensions/invariant-checks/invariants.md).
 *
 * Lines carry ids only, never a uri, name or path. Implementations write nothing unless
 * [me.proton.core.drive.base.domain.provider.ConfigurationProvider.itemLifecycleLogEnabled].
 */
interface ItemLifecycle {

    fun accepted(flow: Flow, id: String, operationId: String? = null)

    fun finished(flow: Flow, id: String, outcome: Outcome)

    /** A key for [uriString] that is stable within this process and reveals nothing outside it. */
    fun backupKey(uriString: String): String

    enum class Flow(val value: String) {
        UPLOAD("upload"),
        BACKUP("backup"),
        DOWNLOAD("download"),
        MOVE("move"),
        TRASH("trash"),
        RESTORE("restore"),
        DELETE("delete"),
        EMPTY_TRASH("empty-trash"),
    }

    enum class Outcome(val value: String) {
        SUCCEEDED("succeeded"),
        FAILED("failed"),
        CANCELLED("cancelled"),
        /** The source of a backup file no longer exists. */
        SOURCE_GONE("source-gone"),
        /** The item was removed without being processed and without telling the user. */
        DROPPED("dropped"),
    }
}
