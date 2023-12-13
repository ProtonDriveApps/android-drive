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

package me.proton.core.drive.linkupload.data.db.entity

import androidx.room.ColumnInfo
import me.proton.core.drive.base.data.db.Column.TOTAL
import me.proton.core.drive.base.data.db.Column.TOTAL_UNPROCESSED_WITH_URI
import me.proton.core.drive.base.data.db.Column.TOTAL_UNPROCESSED_WITH_URI_NON_USER_PRIORITY
import me.proton.core.drive.base.data.db.Column.TOTAL_WITH_ANNOUNCE
import me.proton.core.drive.base.data.db.Column.TOTAL_WITH_URI
import me.proton.core.drive.base.data.db.Column.TOTAL_WITH_URI_NON_USER_PRIORITY

data class LinkUploadCountEntity(
    @ColumnInfo(name = TOTAL)
    val total: Int,
    @ColumnInfo(name = TOTAL_WITH_URI)
    val totalWithUri: Int,
    @ColumnInfo(name = TOTAL_UNPROCESSED_WITH_URI)
    val totalUnprocessedWithUri: Int,
    @ColumnInfo(name = TOTAL_WITH_URI_NON_USER_PRIORITY)
    val totalWithUriNonUserPriority: Int,
    @ColumnInfo(name = TOTAL_UNPROCESSED_WITH_URI_NON_USER_PRIORITY)
    val totalUnprocessedWithUriNonUserPriority: Int,
    @ColumnInfo(name = TOTAL_WITH_ANNOUNCE)
    val totalWithAnnounce: Int,
)
