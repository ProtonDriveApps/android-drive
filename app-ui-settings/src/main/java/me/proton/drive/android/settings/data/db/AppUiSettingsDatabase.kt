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

package me.proton.drive.android.settings.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.recreateTable
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column.HOME_TAB
import me.proton.core.drive.base.data.db.Column.LAYOUT_TYPE
import me.proton.core.drive.base.data.db.Column.THEME_STYLE
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.drive.android.settings.data.db.dao.UiSettingsDao
import me.proton.drive.android.settings.domain.entity.HomeTab

interface AppUiSettingsDatabase : Database {
    val uiSettingsDao: UiSettingsDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `UiSettingsEntity` ADD COLUMN $HOME_TAB TEXT NOT NULL DEFAULT ${HomeTab.DEFAULT}
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.recreateTable(
                    table = "UiSettingsEntity",
                    createTable = {
                        execSQL("""
                            CREATE TABLE IF NOT EXISTS `UiSettingsEntity` (
                              `user_id` TEXT NOT NULL,
                              `layout_type` TEXT NOT NULL,
                              `theme_style` TEXT NOT NULL,
                              `home_tab` TEXT DEFAULT NULL,
                              PRIMARY KEY(`user_id`),
                              FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                        """.trimIndent())
                    },
                    createIndices = {},
                    columns = listOf(USER_ID, LAYOUT_TYPE, THEME_STYLE, HOME_TAB)
                )
            }
        }
    }
}
