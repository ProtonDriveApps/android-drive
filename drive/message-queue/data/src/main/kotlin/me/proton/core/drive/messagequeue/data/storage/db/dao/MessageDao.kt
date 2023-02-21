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

package me.proton.core.drive.messagequeue.data.storage.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import me.proton.core.drive.messagequeue.data.storage.db.entity.MessageEntity

@Dao
interface MessageDao {

    @Insert
    suspend fun saveMessage(message: MessageEntity)

    @Transaction
    suspend fun retrieveUnsavedMessage(): List<MessageEntity> {
        val messages = getMessages()
        deleteMessages(messages)
        return messages
    }

    @Query("SELECT * FROM MessageEntity")
    suspend fun getMessages(): List<MessageEntity>

    @Delete
    suspend fun deleteMessages(messages: List<MessageEntity>)
}
