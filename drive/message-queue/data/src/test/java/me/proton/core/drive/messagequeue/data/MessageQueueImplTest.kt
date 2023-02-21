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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
class MessageQueueImplTest {

    private val mainThreadSurrogate = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }


    @Test
    fun `queue message with collectors won't save the message`() = runInSingleThread {
        // region Given
        var wasAskedToSave = false
        val storage = object : MessageQueueImpl.Storage<String> {
            override suspend fun saveMessage(message: String) {
                wasAskedToSave = true
            }

            override suspend fun retrieveAllUnsentMessages(): List<String> = emptyList()
        }
        val messageQueue = MessageQueueImpl(storage)
        var value: String? = null
        messageQueue.queue.onEach { message ->
            value = message
        }.launchIn(this)
        // endregion
        // region When
        messageQueue.enqueue("This is a test")
        // endregion
        // region Then
        assert(value == "This is a test") { "value was not 'This is a test' but was '$value'" }
        assert(!wasAskedToSave) { "Storage.saveMessage was invoked" }
        // endregion
    }

    @Test
    fun `queue message without collectors saves the message`() = runInSingleThread {
        // region Given
        var value: String? = null
        var wasAskedToSave = false
        val storage = object : MessageQueueImpl.Storage<String> {
            override suspend fun saveMessage(message: String) {
                wasAskedToSave = true
                value = message
            }

            override suspend fun retrieveAllUnsentMessages(): List<String> = emptyList()
        }
        val messageQueue = MessageQueueImpl(storage)
        // endregion
        // region When
        messageQueue.enqueue("This is a test")
        // endregion
        // region Then
        assert(value == "This is a test") { "value was not 'This is a test' but was '$value'" }
        assert(!wasAskedToSave) { "Storage.saveMessage was invoked" }
        // endregion
    }


    @Test
    fun `not empty message queue are delivered on subscription`() = runInSingleThread {
        // region Given
        val storage = object : MessageQueueImpl.Storage<String> {
            override suspend fun saveMessage(message: String) = Unit
            override suspend fun retrieveAllUnsentMessages(): List<String> =
                listOf("This is a test")
        }
        val messageQueue = MessageQueueImpl(storage)
        // endregion
        // region When
        val value = messageQueue.queue.first()
        // endregion
        // region Then
        assert(value == "This is a test") { "value was not 'This is a test' but was '$value'" }
        // endregion
    }

    private inline fun runInSingleThread(crossinline block: suspend CoroutineScope.() -> Unit) {
        CoroutineScope(Dispatchers.Main).launch { block() }
    }
}
