/*
 * Copyright (c) 2025 Proton AG.
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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.last
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.user.domain.usecase.LeaveShare
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class LeaveShare @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val leaveShare: LeaveShare,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(driveLink: DriveLink): Result<Boolean> = coRunCatching {
        val shareId = driveLink.sharingDetails?.shareId
        val memberId = driveLink.shareUser?.id
        if (shareId != null && memberId != null && shareId == driveLink.shareId) {
            leaveShare(driveLink.volumeId, driveLink.id, memberId).last().toResult()
                .onFailure { error ->
                    error.log(LogTag.SHARING, "Cannot leave share")
                    broadcastMessages(
                        userId = driveLink.userId,
                        message = error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        type = BroadcastMessage.Type.ERROR
                    )
                }.getOrThrow()
            true
        } else {
            CoreLogger.i(
                tag = LogTag.SHARING,
                message = """
                    Skipping leave share (DriveLink.shareId=${driveLink.shareId.id.logId()},
                    SharingDetails.shareId=${shareId?.id?.logId()}, memberId=${memberId?.logId()})
                """.trimIndent(),
            )
            false
        }
    }
}
