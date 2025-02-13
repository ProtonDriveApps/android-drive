/*
 * Copyright (c) 2023-2024 Proton AG.
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

@file:Suppress("MatchingDeclarationName")
package me.proton.core.drive.db.test

import me.proton.android.drive.db.DriveDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.data.api.entity.VolumeDto
import me.proton.core.drive.volume.data.db.VolumeEntity
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.user.data.entity.UserEntity

data class VolumeContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val account: AccountEntity,
    val volume: VolumeEntity,
) : BaseContext()

val volumeId = VolumeId("volume-id")
val photoVolumeId = VolumeId("photo-volume-id")
val mainShareId = ShareId(userId, "main-share-id")

suspend fun UserContext.volume(
    volume: VolumeEntity = NullableVolumeEntity(
        id = volumeId.id,
    ),
) = volume(volume) {}

suspend fun <T> UserContext.volume(
    volume: VolumeEntity = NullableVolumeEntity(
        id = volumeId.id,
    ),
    block: suspend VolumeContext.() -> T,
): T {
    db.volumeDao.insertOrUpdate(volume)
    return VolumeContext(db, user, account, volume).block()
}

suspend fun <T> UserContext.photoVolume(
    volume: VolumeEntity = NullablePhotoVolumeEntity(
        id = photoVolumeId.id,
    ),
    block: suspend VolumeContext.() -> T,
): T {
    db.volumeDao.insertOrUpdate(volume)
    return VolumeContext(db, user, account, volume).block()
}

@Suppress("FunctionName")
fun NullableVolumeEntity(
    id: String = volumeId.id,
    state: Long = 1,
    createTime: Long = 0,
) =
    VolumeEntity(
        id = id,
        userId = userId,
        shareId = mainShareId.id,
        linkId = mainRootId.id,
        createTime = createTime,
        usedSpace = 0,
        state = state,
        type = VolumeDto.TYPE_REGULAR,
    )

@Suppress("FunctionName")
fun NullablePhotoVolumeEntity(
    id: String = photoVolumeId.id,
    state: Long = 1,
    createTime: Long = 0,
) =
    VolumeEntity(
        id = id,
        userId = userId,
        shareId = photoShareId.id,
        linkId = photoRootId.id,
        createTime = createTime,
        usedSpace = 0,
        state = state,
        type = VolumeDto.TYPE_PHOTO,
    )
