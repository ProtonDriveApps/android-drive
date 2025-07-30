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

package me.proton.core.drive.eventmanager.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.SignOut
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.DeleteShare
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class OnResetAllEvent @Inject constructor(
    private val getShare: GetShare,
    private val deleteShare: DeleteShare,
    private val signOut: SignOut,
    private val announceEvent: AnnounceEvent,
    private val getMainShare: GetMainShare,
) {
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Main)

    operator fun invoke(shareId: ShareId) {
        coroutineScope.launch {
            CoreLogger.d(LogTag.EVENTS, "onResetAll: shareId ${shareId.id.logId()}")
            getShare(shareId, flowOf(false)).toResult()
                .getOrNull(LogTag.EVENTS, "Cannot get share")?.let { share ->
                    if (share.isMain) {
                        signOutUser(shareId.userId)
                    } else {
                        deleteShare(shareId, locallyOnly = true)
                    }
                }
        }
    }

    operator fun invoke(userId: UserId, volumeId: VolumeId) {
        coroutineScope.launch {
            CoreLogger.d(LogTag.EVENTS, "onResetAll: volumeId ${volumeId.id.logId()}")
            getMainShare(userId).toResult()
                .getOrNull(LogTag.EVENTS, "Cannot get main share")?.let { mainShare ->
                    if (mainShare.volumeId == volumeId) {
                        signOutUser(userId)
                    } else {
                        CoreLogger.d(
                            tag = LogTag.EVENTS,
                            message = "onResetAll: do nothing as volume (${volumeId.id.logId()}) is not main share volume (${mainShare.volumeId.id.logId()})",
                        )
                    }
                }
        }
    }

    private suspend fun signOutUser(userId: UserId) {
        CoreLogger.e(
            tag = LogTag.EVENTS,
            e = RuntimeException("Sign out due to reset all event"),
            message = "Sign out due to reset all event",
        )
        signOut(userId)
        announceEvent(Event.ForcedSignOut)
    }
}
