/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.log.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import me.proton.core.data.room.db.BaseDatabase
import me.proton.core.data.room.db.CommonConverters
import me.proton.core.drive.base.data.db.LoggingOpenHelperFactory
import me.proton.core.drive.log.data.db.entity.LogEntity
import me.proton.core.drive.log.data.db.entity.LogLevelEntity
import me.proton.core.drive.log.data.db.entity.LogOriginEntity

@Database(
    entities = [
        LogEntity::class,
        LogLevelEntity::class,
        LogOriginEntity::class,
    ],
    version = DriveLogDatabase.VERSION,
)
@TypeConverters(
    CommonConverters::class,
)
abstract class DriveLogDatabase : BaseDatabase(), LogDatabase {

    companion object {
        const val VERSION = 1
        private val migrations = listOf<Migration>()

        fun buildDatabase(context: Context): DriveLogDatabase =
            databaseBuilder<DriveLogDatabase>(context, "db-drive-log")
                .openHelperFactory(
                    factory = LoggingOpenHelperFactory(
                        delegate = FrameworkSQLiteOpenHelperFactory(),
                        clazz = DriveLogDatabase::class.java,
                    ),
                )
                .apply { migrations.forEach { addMigrations(it) } }
                .build()
    }
}
