/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.migration.Migration
import me.proton.android.drive.db.entity.ClientUidEntity
import me.proton.android.drive.lock.data.db.AppLockDatabase
import me.proton.android.drive.lock.data.db.entity.AppLockEntity
import me.proton.android.drive.lock.data.db.entity.AutoLockDurationEntity
import me.proton.android.drive.lock.data.db.entity.EnableAppLockEntity
import me.proton.android.drive.lock.data.db.entity.LockEntity
import me.proton.core.data.room.db.BaseDatabase

@Database(
    entities = [
        // AppLock
        AppLockEntity::class,
        LockEntity::class,
        AutoLockDurationEntity::class,
        EnableAppLockEntity::class,
        ClientUidEntity::class,
    ],
    version = AppDatabase.VERSION,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class AppDatabase : BaseDatabase(),
    AppLockDatabase,
    ClientUidDatabase {

    companion object {
        const val VERSION = 2
        private val migrations = listOf<Migration>()

        fun buildDatabase(context: Context): AppDatabase =
            databaseBuilder<AppDatabase>(context, "db-app")
                .apply { migrations.forEach { addMigrations(it) } }
                .build()
    }
}
