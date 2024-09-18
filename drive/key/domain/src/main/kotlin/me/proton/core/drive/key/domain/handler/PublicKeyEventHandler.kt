/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.key.domain.handler

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.key.domain.usecase.MarkPublicAddressKeyAsStale
import javax.inject.Inject

class PublicKeyEventHandler @Inject constructor(
    private val accountManager: AccountManager,
    private val markPublicAddressKeyAsStale: MarkPublicAddressKeyAsStale,
) : EventHandler {

    override suspend fun onEvent(userId: UserId, event: Event) {
        if (event is Event.SignatureVerificationFailed) {
            markPublicAddressKeyAsStale(userId, event.usedPublicKeys).getOrNull(LogTag.KEY)
        }
    }

    override suspend fun onEvent(event: Event) {
        accountManager.getPrimaryUserId().firstOrNull()?.let { userId ->
            onEvent(userId, event)
        }
    }
}
