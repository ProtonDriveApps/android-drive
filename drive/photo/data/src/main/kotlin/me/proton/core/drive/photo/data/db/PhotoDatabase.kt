/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.photo.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.extension.addTableColumn
import me.proton.core.data.room.db.extension.dropTableColumn
import me.proton.core.data.room.db.migration.DatabaseMigration
import me.proton.core.drive.base.data.db.Column.MAIN_PHOTO_LINK_ID
import me.proton.core.drive.base.data.db.Column.MIME_TYPE
import me.proton.core.drive.photo.data.db.dao.AlbumListingDao
import me.proton.core.drive.photo.data.db.dao.AlbumPhotoListingDao
import me.proton.core.drive.photo.data.db.dao.PhotoListingDao
import me.proton.core.drive.photo.data.db.dao.AlbumRelatedPhotoDao
import me.proton.core.drive.photo.data.db.dao.RelatedPhotoDao
import me.proton.core.drive.photo.data.db.dao.TaggedPhotoListingDao
import me.proton.core.drive.photo.data.db.dao.TaggedRelatedPhotoDao
import me.proton.core.drive.photo.data.db.dao.TagsMigrationFileDao
import me.proton.core.drive.photo.data.db.dao.TagsMigrationFileTagDao
import me.proton.core.drive.photo.data.db.entity.TagsMigrationFileEntity

interface PhotoDatabase : Database {
    val photoListingDao: PhotoListingDao
    val relatedPhotoDao: RelatedPhotoDao
    val taggedPhotoListingDao: TaggedPhotoListingDao
    val taggedRelatedPhotoDao: TaggedRelatedPhotoDao
    val albumListingDao: AlbumListingDao
    val albumPhotoListingDao: AlbumPhotoListingDao
    val albumRelatedPhotoDao: AlbumRelatedPhotoDao
    val tagsMigrationFileDao: TagsMigrationFileDao
    val tagsMigrationFileTagDao: TagsMigrationFileTagDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `PhotoListingEntity` (
                    `user_id` TEXT NOT NULL,
                    `volume_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `capture_time` INTEGER NOT NULL,
                    `hash` TEXT,
                    `content_hash` TEXT,
                    `main_photo_link_id` TEXT,
                    PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_user_id` ON `PhotoListingEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_volume_id` ON `PhotoListingEntity` (`volume_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_share_id` ON `PhotoListingEntity` (`share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_id` ON `PhotoListingEntity` (`id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_user_id_volume_id_share_id` ON
                    `PhotoListingEntity` (`user_id`, `volume_id`, `share_id`)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_1 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `AlbumListingEntity` (
                    `user_id` TEXT NOT NULL,
                    `volume_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `locked` INTEGER NOT NULL,
                    `count` INTEGER NOT NULL,
                    `last_activity_time` INTEGER NOT NULL,
                    `link_id` TEXT,
                    `is_shared` INTEGER NOT NULL,
                    PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumListingEntity_user_id` ON `AlbumListingEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumListingEntity_volume_id` ON `AlbumListingEntity` (`volume_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumListingEntity_share_id` ON `AlbumListingEntity` (`share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumListingEntity_id` ON `AlbumListingEntity` (`id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumListingEntity_user_id_volume_id` ON
                    `AlbumListingEntity` (`user_id`, `volume_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumListingEntity_user_id_volume_id_share_id` ON
                    `AlbumListingEntity` (`user_id`, `volume_id`, `share_id`)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_2 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `AlbumPhotoListingEntity` (
                    `user_id` TEXT NOT NULL,
                    `volume_id` TEXT NOT NULL,
                    `share_id` TEXT NOT NULL,
                    `album_id` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `capture_time` INTEGER NOT NULL,
                    `added_time` INTEGER NOT NULL,
                    `is_child_of_album` INTEGER NOT NULL,
                    `hash` TEXT,
                    `content_hash` TEXT,
                    PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `album_id`, `id`),
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingEntity_user_id` ON
                    `AlbumPhotoListingEntity` (`user_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingEntity_volume_id` ON
                    `AlbumPhotoListingEntity` (`volume_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingEntity_share_id`
                    ON `AlbumPhotoListingEntity` (`share_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingEntity_album_id`
                    ON `AlbumPhotoListingEntity` (`album_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingEntity_id`
                    ON `AlbumPhotoListingEntity` (`id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingEntity_user_id_volume_id` ON
                    `AlbumPhotoListingEntity` (`user_id`, `volume_id`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_AlbumPhotoListingEntity_user_id_volume_id_share_id` ON
                    `AlbumPhotoListingEntity` (`user_id`, `volume_id`, `share_id`, `album_id`)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_3 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `TaggedPhotoListingEntity` (
                        `user_id` TEXT NOT NULL, 
                        `volume_id` TEXT NOT NULL, 
                        `share_id` TEXT NOT NULL, 
                        `tag` INTEGER NOT NULL, 
                        `id` TEXT NOT NULL, 
                        `capture_time` INTEGER NOT NULL, 
                        `hash` TEXT, 
                        `content_hash` TEXT, 
                        `main_photo_link_id` TEXT, 
                        PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `tag`, `id`), 
                        FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                        ON UPDATE NO ACTION ON DELETE CASCADE , 
                        FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                        ON UPDATE NO ACTION ON DELETE CASCADE )
                        """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_user_id` ON `TaggedPhotoListingEntity` (`user_id`)
                        """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_volume_id` ON `TaggedPhotoListingEntity` (`volume_id`)
                        """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_share_id` ON `TaggedPhotoListingEntity` (`share_id`)
                        """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_id` ON `TaggedPhotoListingEntity` (`id`)
                        """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_user_id_volume_id_share_id_tag` 
                        ON `TaggedPhotoListingEntity` (`user_id`, `volume_id`, `share_id`, `tag`)
                        """.trimIndent()
                )
            }
        }

        val MIGRATION_4 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.dropTableColumn(
                    table = "PhotoListingEntity",
                    column = MAIN_PHOTO_LINK_ID,
                    createTable = {
                        execSQL("""
                            CREATE TABLE IF NOT EXISTS `PhotoListingEntity` (
                            `user_id` TEXT NOT NULL,
                            `volume_id` TEXT NOT NULL,
                            `share_id` TEXT NOT NULL,
                            `id` TEXT NOT NULL,
                            `capture_time` INTEGER NOT NULL,
                            `hash` TEXT,
                            `content_hash` TEXT,
                            PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `id`),
                            FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE )
                        """.trimIndent())
                    },
                    createIndices = {
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_user_id` ON `PhotoListingEntity` (`user_id`)
                        """.trimIndent())
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_volume_id` ON `PhotoListingEntity` (`volume_id`)
                        """.trimIndent())
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_share_id` ON `PhotoListingEntity` (`share_id`)
                        """.trimIndent())
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_id` ON `PhotoListingEntity` (`id`)
                        """.trimIndent())
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_PhotoListingEntity_user_id_volume_id_share_id` ON `PhotoListingEntity` (`user_id`, `volume_id`, `share_id`)
                        """.trimIndent())
                    }
                )

                database.dropTableColumn(
                    table = "TaggedPhotoListingEntity",
                    column = MAIN_PHOTO_LINK_ID,
                    createTable = {
                        execSQL("""
                            CREATE TABLE IF NOT EXISTS `TaggedPhotoListingEntity` (
                            `user_id` TEXT NOT NULL,
                            `volume_id` TEXT NOT NULL,
                            `share_id` TEXT NOT NULL,
                            `tag` INTEGER NOT NULL,
                            `id` TEXT NOT NULL,
                            `capture_time` INTEGER NOT NULL,
                            `hash` TEXT,
                            `content_hash` TEXT,
                            PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `tag`, `id`),
                            FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE ,
                            FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE )
                        """.trimIndent())
                    },
                    createIndices = {
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_user_id` ON `TaggedPhotoListingEntity` (`user_id`)
                        """.trimIndent())
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_volume_id` ON `TaggedPhotoListingEntity` (`volume_id`)
                        """.trimIndent())
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_share_id` ON `TaggedPhotoListingEntity` (`share_id`)
                        """.trimIndent())
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_id` ON `TaggedPhotoListingEntity` (`id`)
                        """.trimIndent())
                        execSQL("""
                            CREATE INDEX IF NOT EXISTS `index_TaggedPhotoListingEntity_user_id_volume_id_share_id_tag` ON `TaggedPhotoListingEntity` (`user_id`, `volume_id`, `share_id`, `tag`)
                        """.trimIndent())
                    }
                )

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `RelatedPhotoEntity` (
                    `user_id` TEXT NOT NULL, 
                    `volume_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `id` TEXT NOT NULL, 
                    `main_photo_link_id` TEXT NOT NULL, 
                    `capture_time` INTEGER NOT NULL, 
                    `hash` TEXT, 
                    `content_hash` TEXT, 
                    PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `id`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `volume_id`, `share_id`, `main_photo_link_id`) 
                    REFERENCES `PhotoListingEntity`(`user_id`, `volume_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_RelatedPhotoEntity_user_id` ON `RelatedPhotoEntity` (`user_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_RelatedPhotoEntity_volume_id` ON `RelatedPhotoEntity` (`volume_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_RelatedPhotoEntity_share_id` ON `RelatedPhotoEntity` (`share_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_RelatedPhotoEntity_id` ON `RelatedPhotoEntity` (`id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_RelatedPhotoEntity_user_id_volume_id_share_id` ON `RelatedPhotoEntity` 
                    (`user_id`, `volume_id`, `share_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_RelatedPhotoEntity_user_id_volume_id_main_photo_link_id` ON `RelatedPhotoEntity`
                     (`user_id`, `volume_id`, `main_photo_link_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `TaggedRelatedPhotoEntity` (
                    `user_id` TEXT NOT NULL, 
                    `volume_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `tag` INTEGER NOT NULL, 
                    `id` TEXT NOT NULL,
                    `main_photo_link_id` TEXT NOT NULL, 
                    `capture_time` INTEGER NOT NULL, 
                    `hash` TEXT, 
                    `content_hash` TEXT, 
                    PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `tag`, `id`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE ,
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `volume_id`, `share_id`, `tag`, `main_photo_link_id`) 
                    REFERENCES `TaggedPhotoListingEntity`(`user_id`, `volume_id`, `share_id`, `tag`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TaggedRelatedPhotoEntity_user_id` ON `TaggedRelatedPhotoEntity` (`user_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TaggedRelatedPhotoEntity_volume_id` ON `TaggedRelatedPhotoEntity` (`volume_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TaggedRelatedPhotoEntity_share_id` ON `TaggedRelatedPhotoEntity` (`share_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TaggedRelatedPhotoEntity_id` ON `TaggedRelatedPhotoEntity` (`id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TaggedRelatedPhotoEntity_user_id_volume_id_share_id` ON `TaggedRelatedPhotoEntity`
                     (`user_id`, `volume_id`, `share_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TaggedRelatedPhotoEntity_user_id_volume_id_main_photo_link_id` ON `TaggedRelatedPhotoEntity`
                     (`user_id`, `volume_id`, `main_photo_link_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `AlbumRelatedPhotoEntity` (
                    `user_id` TEXT NOT NULL, 
                    `volume_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `album_id` TEXT NOT NULL, 
                    `id` TEXT NOT NULL,
                    `main_photo_link_id` TEXT NOT NULL,
                    `capture_time` INTEGER NOT NULL, 
                    `hash` TEXT, `content_hash` TEXT, 
                    PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `album_id`, `id`), 
                    FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`user_id`, `volume_id`, `share_id`, `album_id`, `main_photo_link_id`) 
                    REFERENCES `AlbumPhotoListingEntity`(`user_id`, `volume_id`, `share_id`, `album_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_AlbumRelatedPhotoEntity_user_id` ON `AlbumRelatedPhotoEntity` (`user_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_AlbumRelatedPhotoEntity_volume_id` ON `AlbumRelatedPhotoEntity` (`volume_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_AlbumRelatedPhotoEntity_share_id` ON `AlbumRelatedPhotoEntity` (`share_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_AlbumRelatedPhotoEntity_id` ON `AlbumRelatedPhotoEntity` (`id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_AlbumRelatedPhotoEntity_user_id_volume_id_share_id` 
                    ON `AlbumRelatedPhotoEntity` (`user_id`, `volume_id`, `share_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_AlbumRelatedPhotoEntity_user_id_volume_id_album_id_main_photo_link_id` ON `AlbumRelatedPhotoEntity`
                     (`user_id`, `volume_id`, `album_id`, `main_photo_link_id`)
                """.trimIndent())
            }
        }
        val MIGRATION_5 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `TagsMigrationFileEntity` (
                    `user_id` TEXT NOT NULL, 
                    `volume_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `id` TEXT NOT NULL, 
                    `capture_time` INTEGER NOT NULL, 
                    `state` TEXT NOT NULL, 
                    `uri` TEXT, 
                    PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `id`), 
                    FOREIGN KEY(`user_id`, `share_id`) REFERENCES `ShareEntity`(`user_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TagsMigrationFileEntity_user_id_volume_id` ON `TagsMigrationFileEntity` (`user_id`, `volume_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TagsMigrationFileEntity_user_id_share_id` ON `TagsMigrationFileEntity` (`user_id`, `share_id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TagsMigrationFileEntity_user_id_volume_id_share_id_id` ON `TagsMigrationFileEntity` (`user_id`, `volume_id`, `share_id`, `id`)
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `TagsMigrationFileTagEntity` (
                    `user_id` TEXT NOT NULL, 
                    `volume_id` TEXT NOT NULL, 
                    `share_id` TEXT NOT NULL, 
                    `id` TEXT NOT NULL, 
                    `tag` INTEGER NOT NULL, 
                    PRIMARY KEY(`user_id`, `volume_id`, `share_id`, `id`, `tag`), 
                    FOREIGN KEY(`user_id`, `volume_id`, `share_id`, `id`) 
                    REFERENCES `TagsMigrationFileEntity`(`user_id`, `volume_id`, `share_id`, `id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_TagsMigrationFileTagEntity_user_id_volume_id_share_id_id` ON `TagsMigrationFileTagEntity` (`user_id`, `volume_id`, `share_id`, `id`)
                """.trimIndent())
            }
        }

        val MIGRATION_6 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addTableColumn("TagsMigrationFileEntity", MIME_TYPE, "TEXT")
            }
        }
    }
}
