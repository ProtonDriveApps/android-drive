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

import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.promo.domain.entity.BlackFriday2026Offer
import me.proton.core.drive.promo.domain.usecase.GetBlackFriday2026Offer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@ActivityRetainedScoped
class BlackFriday2026OfferCache internal constructor(
    private val getBlackFriday2026Offer: GetBlackFriday2026Offer,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(
        getBlackFriday2026Offer: GetBlackFriday2026Offer,
        activityRetainedLifecycle: ActivityRetainedLifecycle,
    ) : this(getBlackFriday2026Offer, CoroutineScope(SupervisorJob())) {
        activityRetainedLifecycle.addOnClearedListener { scope.cancel() }
    }

    private val states = ConcurrentHashMap<UserId, MutableStateFlow<State>>()
    private val jobs = ConcurrentHashMap<UserId, Job>()

    operator fun invoke(userId: UserId): StateFlow<State> {
        jobs.computeIfAbsent(userId) { collect(userId) }
        return state(userId).asStateFlow()
    }

    fun availableOffer(userId: UserId): Flow<BlackFriday2026Offer?> = invoke(userId).map { state ->
        (state as? State.Loaded)?.offer
    }

    fun refresh(userId: UserId) {
        jobs.compute(userId) { _, job ->
            job?.cancel()
            state(userId).value = State.Loading
            collect(userId)
        }
    }

    private fun state(userId: UserId) = states.computeIfAbsent(userId) { MutableStateFlow(State.Loading) }

    private fun collect(userId: UserId): Job = scope.launch {
        getBlackFriday2026Offer(userId).collect { offer ->
            state(userId).value = State.Loaded(offer)
        }
    }

    sealed interface State {
        data object Loading : State
        data class Loaded(val offer: BlackFriday2026Offer?) : State
    }
}
