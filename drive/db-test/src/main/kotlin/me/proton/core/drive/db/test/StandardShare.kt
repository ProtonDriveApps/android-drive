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
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.data.api.ShareDto
import me.proton.core.drive.share.data.db.ShareMembershipEntity
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.user.domain.entity.AddressId

fun standardShareId(index: Int = defaultIndex) = ShareId(userId, "standard-share-id-$index")
fun standardRootId(index: Int = defaultIndex) =
    FolderId(standardShareId(index), "standard-${index}-root-id")

suspend fun DriveDatabaseRule.standardShareByMe(
    id: String,
    block: suspend ShareContext.() -> Unit = {},
): ShareId = db.standardShareByMe(
    id = id,
    block = block
)

suspend fun DriveDatabase.standardShareByMe(
    id: String,
    block: suspend ShareContext.() -> Unit = {},
): ShareId = user {
    withKey()
    volume {
        this@volume.standardShare(
            id = id,
            inviterEmail = requireNotNull(account.email),
            inviteeEmail = requireNotNull(account.email),
            block = block
        )
    }
}

suspend fun VolumeContext.standardShareByMe(
    id: String,
    email: String = requireNotNull(account.email),
    block: suspend ShareContext.() -> Unit = {},
): ShareId = standardShare(
    id = id,
    inviterEmail = email,
    inviteeEmail = email,
    block = block
)

suspend fun DriveDatabaseRule.standardShareWithMe(
    id: String,
    inviterEmail: String = "another-user@proton.test",
    block: suspend ShareContext.() -> Unit = {},
): ShareId = db.standardShareWithMe(
    id = id,
    inviterEmail = inviterEmail,
    block = block
)

suspend fun DriveDatabase.standardShareWithMe(
    id: String,
    inviterEmail: String = "another-user@proton.test",
    block: suspend ShareContext.() -> Unit = {},
): ShareId = user {
    withKey()
    volume {
        this@volume.standardShare(
            id = id,
            inviteeEmail = requireNotNull(account.email),
            inviterEmail = inviterEmail,
            block = block
        )
    }
}

suspend fun VolumeContext.standardShareWithMe(
    id: String,
    inviteeEmail: String = requireNotNull(account.email),
    inviterEmail: String = "another-user@proton.test",
    block: suspend ShareContext.() -> Unit = {},
): ShareId = standardShare(
    id = id,
    inviterEmail = inviterEmail,
    inviteeEmail = inviteeEmail,
    block = block
)

suspend fun VolumeContext.standardShare(
    id: String,
    inviterEmail: String,
    inviteeEmail: String,
    permission: Permissions = Permissions.admin,
    block: suspend ShareContext.() -> Unit = {},
): ShareId {
    share(
        shareEntity = NullableShareEntity(
            id = id,
            linkId = "root-id-$id",
            type = ShareDto.TYPE_STANDARD,
            addressId = AddressId("address-id-$inviterEmail")
        )
    ) {
        db.shareMembershipDao.insertOrUpdate(
            ShareMembershipEntity(
                id = "member-id-$id",
                userId = userId,
                shareId = id,
                inviterEmail = inviterEmail,
                inviteeEmail = inviteeEmail,
                addressId = "address-id-$inviteeEmail",
                permissions = permission.value,
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
