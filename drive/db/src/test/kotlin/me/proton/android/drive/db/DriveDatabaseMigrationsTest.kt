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

package me.proton.android.drive.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DriveDatabaseMigrationsTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DriveDatabase::class.java,
    )

    @Test
    @Suppress("LongMethod")
    fun migrate38To39() {
        helper.createDatabase("migration-test", 38).apply {
            execSQL(
                """
                INSERT OR IGNORE INTO `AccountEntity` (
                `userId`,`username`,`email`,`state`,`sessionId`,`sessionState`
                ) VALUES (?,?,?,?,?,?)
            """.trimIndent(), arrayOf("userId", "user", null, "Ready", null, null)
            )
            execSQL(
                """
                INSERT OR IGNORE INTO `UserEntity` (
                `userId`,`email`,`name`,`displayName`,`currency`,`credit`,
                `createdAtUtc`,`usedSpace`,`maxSpace`,`maxUpload`,`role`,
                `private`,`subscribed`,`services`,`delinquent`,`passphrase`,
                `recovery_state`,`recovery_startTime`,`recovery_endTime`,
                `recovery_sessionId`,`recovery_reason`
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(), arrayOf(
                    "userId", null, null, null, "EUR", 0, 0, 0, 0, 0,
                    null, 0, 0, 0, null, null, null
                )
            )
            execSQL(
                """
                INSERT OR IGNORE INTO `VolumeEntity` (
                `id`,`user_id`,`share_id`,`creation_time`,`max_space`,`used_space`,`state`
                ) VALUES (?,?,?,?,?,?,?)
            """.trimIndent(), arrayOf("volumeId", "userId", "shareId", 0, null, 0, 0)
            )
            execSQL(
                """
                INSERT OR IGNORE INTO `ShareEntity` (
                `id`,`user_id`,`volume_id`,`address_id`,`flags`,`link_id`,
                `locked`,`key`,`passphrase`,`passphrase_signature`,
                `creation_time`,`type`
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(), arrayOf(
                    "shareId", "userId", "volumeId", null, 0, "parentId",
                    0, "key", "passphrase", "passphraseSignature", null, 0
                )
            )
            execSQL(
                """
                INSERT OR IGNORE INTO `LinkEntity` (
                `id`,`share_id`,`user_id`,`parent_id`,`type`,`name`,`name_signature_email`,
                `hash`,`state`,`expiration_time`,`size`,`mime_type`,`attributes`,`permissions`,
                `node_key`,`node_passphrase`,`node_passphrase_signature`,`signature_address`,
                `creation_time`,`last_modified`,`trashed_time`,`is_shared`,`number_of_accesses`,
                `share_url_expiration_time`,`x_attr`,`share_url_share_id`,`share_url_id`
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(), arrayOf(
                    "parentId", "shareId", "userId", null, 0, "parent", null, "hash", 0, null,
                    0, "", 0, 0, "", "", "", "", 0, 0, null, 0, 0, null, null, null, null
                )
            )

            execSQL(
                """
                INSERT OR IGNORE INTO `BackupFolderEntity` (
                `user_id`,`share_id`,`parent_id`,`bucket_id`,`update_time`
                ) VALUES (?,?,?,?,?)
            """.trimIndent(), arrayOf("userId", "shareId", "parentId", 0, 0)
            )

            execSQL(
                """
                INSERT OR IGNORE INTO `BackupFolderEntity` (
                `user_id`,`share_id`,`parent_id`,`bucket_id`,`update_time`
                ) VALUES (?,?,?,?,?)
            """.trimIndent(), arrayOf("userId", "shareId", "parentId", 1, 0)
            )

            execSQL(
                """
                INSERT OR IGNORE INTO `BackupFileEntity` (
                `user_id`,`bucket_id`,`uri`,`mime_type`,`name`,`hash`,`size`,`state`,`creation_time`,`priority`,`attempts`
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(), arrayOf("userId", 0, "uri1", "", "", "", 0, "ENQUEUED", 0, 0, 0)
            )

            execSQL(
                """
                INSERT OR IGNORE INTO `BackupFileEntity` (
                `user_id`,`bucket_id`,`uri`,`mime_type`,`name`,`hash`,`size`,`state`,`creation_time`,`priority`,`attempts`
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(), arrayOf("userId", 0, "uri2", "", "", "", 0, "ENQUEUED", 0, 0, 0)
            )

            execSQL(
                """
                INSERT OR IGNORE INTO `BackupFileEntity` (
                `user_id`,`bucket_id`,`uri`,`mime_type`,`name`,`hash`,`size`,`state`,`creation_time`,`priority`,`attempts`
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent(), arrayOf("userId", 1, "uri3", "", "", "", 0, "ENQUEUED", 0, 0, 0)
            )

            execSQL(
                """
                INSERT OR IGNORE INTO `BackupErrorEntity` (`user_id`,`error`,`retryable`) VALUES (?,?,?)
            """.trimIndent(), arrayOf("userId", "OTHER", 0)
            )

            close()
        }

        val db = helper.runMigrationsAndValidate(
            "migration-test",
            39,
            true,
            DriveDatabaseMigrations.MIGRATION_38_39
        )

        db.query("SELECT `user_id`,`share_id`,`parent_id`,`bucket_id`,`uri` FROM BackupFileEntity")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    assertEquals("userId", cursor.getString(0))
                    assertEquals("shareId", cursor.getString(1))
                    assertEquals("parentId", cursor.getString(2))
                    if (cursor.getString(4) in listOf("uri1", "uri2")) {
                        assertEquals(0, cursor.getInt(3))
                    } else {
                        assertEquals(1, cursor.getInt(3))
                    }
                }
            }

        db.query("SELECT `user_id`,`share_id`,`parent_id`,`error` FROM BackupErrorEntity")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    assertEquals("userId", cursor.getString(0))
                    assertEquals("shareId", cursor.getString(1))
                    assertEquals("parentId", cursor.getString(2))
                    assertEquals("OTHER", cursor.getString(3))
                }
            }
    }
}
