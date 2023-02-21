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
package me.proton.core.drive.link.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.proton.core.account.data.db.AccountDao
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.data.room.db.CommonConverters
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkFolderPropertiesEntity
import me.proton.core.drive.share.data.db.ShareDao
import me.proton.core.drive.share.data.db.ShareEntity
import me.proton.core.user.data.db.UserConverters

@Database(
    entities = [
        AccountEntity::class,
        SessionEntity::class,
        LinkEntity::class,
        LinkFilePropertiesEntity::class,
        LinkFolderPropertiesEntity::class,
        ShareEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(
    CommonConverters::class,
    UserConverters::class,
)
abstract class TestDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun accountDao(): AccountDao
    abstract fun shareDao(): ShareDao
}

fun buildDatabase(context: Context): TestDatabase =
    Room.inMemoryDatabaseBuilder(
        context, TestDatabase::class.java
    ).build()
