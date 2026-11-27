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
package me.proton.core.drive.test.log

import me.proton.core.drive.base.domain.log.ItemLifecycle
import javax.inject.Inject
import javax.inject.Singleton

/** Records lifecycle lines in the format of the real implementation, so tests can assert them. */
@Singleton
class TestItemLifecycle @Inject constructor() : ItemLifecycle {

    val lines = mutableListOf<String>()

    override fun accepted(flow: ItemLifecycle.Flow, id: String, operationId: String?) {
        lines += "accepted flow=${flow.value} id=$id" + operationId?.let { " op=$it" }.orEmpty()
    }

    override fun finished(flow: ItemLifecycle.Flow, id: String, outcome: ItemLifecycle.Outcome) {
        lines += "finished flow=${flow.value} id=$id outcome=${outcome.value}"
    }

    override fun backupKey(uriString: String): String = "key($uriString)"
}
