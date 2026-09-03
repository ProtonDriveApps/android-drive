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

package me.proton.android.drive.usecase

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.flowOf
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@ActivityRetainedScoped
class ObserveQ3CampaignPromoEligible @Inject constructor(
    private val isQ3CampaignPromoEligible: IsQ3CampaignPromoEligible,
) {
    private val scope = CoroutineScope(SupervisorJob())
    private val cache = ConcurrentHashMap<UserId, StateFlow<Boolean?>>()

    operator fun invoke(userId: UserId): StateFlow<Boolean?> = cache.computeIfAbsent(userId) {
        flowOf { runCatching { isQ3CampaignPromoEligible(userId) }.getOrDefault(false) }
            .stateIn(scope, SharingStarted.Eagerly, null)
    }
}
