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
package me.proton.core.drive.linkdownload.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column.ID
import me.proton.core.drive.base.data.db.Column.LINK_ID
import me.proton.core.drive.base.data.db.Column.MANIFEST_SIGNATURE
import me.proton.core.drive.base.data.db.Column.REVISION_ID
import me.proton.core.drive.base.data.db.Column.SHARE_ID
import me.proton.core.drive.base.data.db.Column.SIGNATURE_ADDRESS
import me.proton.core.drive.base.data.db.Column.STATE
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.link.data.db.entity.LinkEntity

@Entity(
    primaryKeys = [USER_ID, SHARE_ID, LINK_ID, REVISION_ID],
    foreignKeys = [
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = [USER_ID, SHARE_ID, ID],
            childColumns = [USER_ID, SHARE_ID, LINK_ID],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = [USER_ID]),
        Index(value = [SHARE_ID]),
        Index(value = [LINK_ID]),
        Index(value = [REVISION_ID]),
        Index(value = [STATE]),
        Index(value = [USER_ID, SHARE_ID, LINK_ID]),
    ],
)
data class LinkDownloadStateEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = SHARE_ID)
    val shareId: String,
    @ColumnInfo(name = LINK_ID)
    val linkId: String,
    @ColumnInfo(name = REVISION_ID)
    val revisionId: String,
    @ColumnInfo(name = STATE)
    val state: LinkDownloadState,
    @ColumnInfo(name = MANIFEST_SIGNATURE, defaultValue = "NULL")
    val manifestSignature: String? = null,
    @ColumnInfo(name = SIGNATURE_ADDRESS, defaultValue = "NULL")
    val signatureAddress: String? = null,
)
