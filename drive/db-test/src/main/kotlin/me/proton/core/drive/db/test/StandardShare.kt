/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.db.test

import me.proton.android.drive.db.DriveDatabase
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.data.api.ShareDto
import me.proton.core.drive.share.data.db.ShareMembershipEntity
import me.proton.core.drive.share.domain.entity.ShareId

fun standardShareId(index: Int = defaultIndex) = ShareId(userId, "standard-share-id-$index")
fun standardRootId(index: Int = defaultIndex) =
    FolderId(standardShareId(index), "standard-${index}-root-id")

suspend fun DriveDatabaseRule.standardShare(
    id: String,
    block: suspend ShareContext.() -> Unit = {},
): ShareId = db.standardShare(id, block)

suspend fun DriveDatabase.standardShare(
    id: String,
    block: suspend ShareContext.() -> Unit = {},
): ShareId = user {
    withKey()
    volume {
        standardShare(id, block)
    }
}

suspend fun VolumeContext.standardShare(
    id: String,
    block: suspend ShareContext.() -> Unit = {},
): ShareId {
    share(
        shareEntity = NullableShareEntity(
            id = id,
            userId = user.userId,
            volumeId = volumeId.id,
            linkId = "root-id-$id",
            type = ShareDto.TYPE_STANDARD,
        )
    ) {
        db.shareMembershipDao.insertOrUpdate(
            ShareMembershipEntity(
                id = "member-id-$id",
                userId = userId,
                shareId = id,
                inviterEmail = account.email!!,
                inviteeEmail = account.email!!,
                permissions = 55,
                keyPacket = "key-packet-$id",
                keyPacketSignature = null,
                sessionKeySignature = null,
                createTime = 0,
            )
        )
        withKey()
        ShareContext(db, user, account, volume, share).block()
    }
    return ShareId(user.userId, id)
}

private val defaultIndex = 1
