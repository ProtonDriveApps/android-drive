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

package me.proton.core.drive.drivelink.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.Column
import me.proton.core.drive.base.data.db.Column.SHARE_INVITATION_COUNT
import me.proton.core.drive.base.data.db.Column.SHARE_MEMBER_COUNT
import me.proton.core.drive.link.data.db.entity.LinkWithPropertiesEntity
import me.proton.core.drive.linkdownload.data.db.entity.LinkDownloadStateWithBlockEntity
import me.proton.core.drive.linkoffline.data.db.LinkOfflineEntity
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.share.user.data.db.entity.ShareMemberEntity

@Entity
data class DriveLinkEntityWithBlock(
    @ColumnInfo(name = "${BASE_PREFIX}_${Column.USER_ID}")
    val userId: UserId,
    @ColumnInfo(name = "${BASE_PREFIX}_${Column.VOLUME_ID}")
    val volumeId: String,
    @Embedded
    val linkWithPropertiesEntity: LinkWithPropertiesEntity,
    @Embedded(prefix = "${OFFLINE_PREFIX}_")
    val linkOfflineEntity: LinkOfflineEntity?,
    @Embedded(prefix = "${DOWNLOAD_PREFIX}_")
    val downloadStateWithBlock: LinkDownloadStateWithBlockEntity?,
    @ColumnInfo(name = "${TRASH_PREFIX}_${Column.STATE}")
    val trashState: TrashState?,
    @ColumnInfo(name = SHARE_INVITATION_COUNT)
    val shareInvitationCount: Int?,
    @ColumnInfo(name = SHARE_MEMBER_COUNT)
    val shareMemberCount: Int?,
    @Embedded(prefix = "${SHARE_MEMBER_PREFIX}_")
    val shareMemberEntity: ShareMemberEntity?,
) {

    companion object {
        const val BASE_PREFIX = "base"
        const val OFFLINE_PREFIX = "offline"
        const val DOWNLOAD_PREFIX = "download"
        const val TRASH_PREFIX = "trash"
        const val SELECTION_PREFIX = "selection"
        const val SHARE_MEMBER_PREFIX = "share_member"
    }
}
