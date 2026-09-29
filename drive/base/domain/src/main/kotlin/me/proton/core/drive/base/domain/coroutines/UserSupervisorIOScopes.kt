/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.base.domain.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.ConcurrentHashMap

/** Per user [CoroutineScope]s that log an unhandled error, unless it is fatal and the process must die. */
class UserSupervisorIOScopes(private val logTag: String) {

    private val scopes = ConcurrentHashMap<UserId, CoroutineScope>()

    operator fun get(userId: UserId): CoroutineScope = scopes.computeIfAbsent(userId) {
        CoreLogger.d(logTag, "Creating scope for user ${userId.id.logId()}")
        CoroutineScope(
            Dispatchers.IO + SupervisorJob() + unhandledErrorHandler(logTag)
        )
    }

    fun remove(userId: UserId) {
        scopes.remove(userId)?.cancel("Scope for user ${userId.id.logId()} is removed")
    }
}
