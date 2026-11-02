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

package me.proton.core.drive.user.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.usersettings.domain.usecase.ObserveUserSettings
import javax.inject.Inject

class GetInAppNotificationsEnabled @Inject constructor(
    private val observeUserSettings: ObserveUserSettings,
) {

    operator fun invoke(userId: UserId): Flow<Boolean> = observeUserSettings(userId)
        .map { userSettings ->
            val news = userSettings?.news ?: return@map true
            news and IN_APP_NOTIFICATIONS != 0
        }
        .distinctUntilChanged()

    private companion object {
        const val IN_APP_NOTIFICATIONS = 1 shl 14
    }
}
