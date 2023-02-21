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

package me.proton.core.drive.messagequeue.data.storage

import android.util.Base64
import me.proton.core.data.room.BuildConfig
import me.proton.core.drive.messagequeue.data.MessageQueueImpl
import me.proton.core.drive.messagequeue.domain.entity.UserAwareSerializable
import me.proton.core.drive.messagequeue.data.storage.db.MessageQueueDatabase
import me.proton.core.drive.messagequeue.data.storage.db.entity.MessageEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class RoomStorage<T : UserAwareSerializable>(
    private val db: MessageQueueDatabase,
) : MessageQueueImpl.Storage<T> {


    override suspend fun saveMessage(message: T) = message.serialize()?.let { serialized ->
        db.messageDao.saveMessage(MessageEntity(content = serialized, userId = message.userId))
    } ?: Unit

    override suspend fun retrieveAllUnsentMessages(): List<T> =
        db.messageDao.retrieveUnsavedMessage()
            .mapNotNull { message -> message.content.deserialize() }

    private fun <T : Serializable> T.serialize(): String? = try {
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { os ->
                os.writeObject(this)
                String(Base64.encode(bos.toByteArray(), Base64.NO_WRAP))
            }
        }
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            throw e
        }
        null
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> String.deserialize(): T? = try {
        ByteArrayInputStream(Base64.decode(this, Base64.NO_WRAP)).use { bis ->
            ObjectInputStream(bis).use { ois ->
                ois.readObject() as T
            }
        }
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            throw e
        }
        null
    }
}
