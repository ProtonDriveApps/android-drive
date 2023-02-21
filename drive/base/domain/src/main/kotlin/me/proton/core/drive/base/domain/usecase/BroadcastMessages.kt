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
package me.proton.core.drive.base.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.messagequeue.domain.MessageQueue
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import java.io.Serializable
import javax.inject.Inject

class BroadcastMessages @Inject constructor(
    private val messageQueue: MessageQueue<BroadcastMessage>,
) {

    operator fun invoke(
        userId: UserId,
        message: String,
        type: BroadcastMessage.Type = BroadcastMessage.Type.SUCCESS,
        extra: Serializable? = null,
    ) {
        messageQueue.enqueue(BroadcastMessage(userId.id, message, type, extra))
    }
}
