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

package me.proton.core.drive.messagequeue.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.messagequeue.domain.MessageQueue
import java.io.Serializable

class MessageQueueImpl<T : Serializable>(
    private val storage: Storage<T>,
) : MessageQueue<T> {

    private val mutex = Mutex()

    private val scope = CoroutineScope(Dispatchers.Main)

    private val mutableSharedFlow = MutableSharedFlow<T>()

    override val queue: Flow<T> = mutableSharedFlow.onSubscription {
        mutex.withLock {
            storage.retrieveAllUnsentMessages().forEach { message -> emit(message) }
        }
    }

    override fun enqueue(message: T) {
        scope.launch {
            mutex.withLock {
                runCatching {
                    val subscribers = mutableSharedFlow.subscriptionCount.firstOrNull() ?: 0
                    if (subscribers > 0) {
                        mutableSharedFlow.emit(message)
                    } else {
                        storage.saveMessage(message)
                    }
                }.onFailure { error ->
                    error.log(LogTag.EVENTS, "Cannot enqueue message ${message.javaClass.simpleName}")
                }
            }
        }
    }

    interface Storage<T> {
        suspend fun saveMessage(message: T)
        suspend fun retrieveAllUnsentMessages(): List<T>
    }
}
