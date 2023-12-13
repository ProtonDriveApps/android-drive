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

package me.proton.core.drive.linkupload.domain.entity

import me.proton.core.test.kotlin.assertTrue
import org.junit.Test

@Suppress("KotlinConstantConditions")
class UploadFileLinkTest {

    @Test
    fun `user priority must be less then recent backup priority`() {
        assertTrue(UploadFileLink.USER_PRIORITY < UploadFileLink.RECENT_BACKUP_PRIORITY) {
            """
                user priority ${UploadFileLink.USER_PRIORITY} must be less then
                recent backup priority ${UploadFileLink.RECENT_BACKUP_PRIORITY}
            """.trimIndent()
        }
    }

    @Test
    fun `recent backup priority must be less then backup priority`() {
        assertTrue(UploadFileLink.RECENT_BACKUP_PRIORITY < UploadFileLink.BACKUP_PRIORITY) {
            """
                recent backup priority ${UploadFileLink.RECENT_BACKUP_PRIORITY} must be less then
                backup priority ${UploadFileLink.BACKUP_PRIORITY}
            """.trimIndent()
        }
    }
}
