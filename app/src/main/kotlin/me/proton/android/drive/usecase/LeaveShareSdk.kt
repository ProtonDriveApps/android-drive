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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.entity.NodeUid
import javax.inject.Inject

class LeaveShareSdk @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val protonDriveClientProvider: ProtonDriveClientProvider,
    private val updateEventAction: UpdateEventAction,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(userId: UserId, nodeUid: NodeUid): Result<Boolean> = coRunCatching {
        val client = protonDriveClientProvider
            .getOrCreate(userId)
            .getOrThrow()
        val node = client.getNode(nodeUid)
        if (node != null && node.isShared) {
            coRunCatching {
                updateEventAction(
                    userId = userId,
                    nodeUid = nodeUid,
                ) {
                    client.leaveSharedNode(nodeUid)
                }
            }.onFailure { error ->
                error.log(LogTag.SHARING, "Cannot leave share")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage
                    ),
                    type = BroadcastMessage.Type.ERROR
                )
            }.getOrThrow()
            true
        } else {
            CoreLogger.w(
                tag = LogTag.SHARING,
                message = """
                    Skipping leave share (isShared:${node?.isShared}, isSharedByUrl:${node?.isSharedByUrl})
                """.trimIndent(),
            )
            false
        }
    }
}
