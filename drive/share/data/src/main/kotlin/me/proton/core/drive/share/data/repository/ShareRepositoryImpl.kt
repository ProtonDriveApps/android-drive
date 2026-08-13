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
package me.proton.core.drive.share.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.asSuccessOrNullAsError
import me.proton.core.drive.base.domain.usecase.GetUserEmail
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.data.api.ShareApiDataSource
import me.proton.core.drive.share.data.db.ShareDao
import me.proton.core.drive.share.data.db.ShareDatabase
import me.proton.core.drive.share.data.db.ShareEntity
import me.proton.core.drive.share.data.db.ShareMembershipEntity
import me.proton.core.drive.share.data.extension.toLong
import me.proton.core.drive.share.data.extension.toShare
import me.proton.core.drive.share.data.extension.toShareEntity
import me.proton.core.drive.share.data.extension.toShareMembership
import me.proton.core.drive.share.data.extension.toShareType
import me.proton.core.drive.share.data.extension.toShareUserMember
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.entity.ShareInfo
import me.proton.core.drive.share.domain.entity.ShareMembership
import me.proton.core.drive.share.domain.repository.ShareRepository
import me.proton.core.drive.volume.data.extension.toVolumeType
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.user.domain.entity.AddressId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRepositoryImpl @Inject constructor(
    private val api: ShareApiDataSource,
    private val db: ShareDatabase,
    private val getUserEmail: GetUserEmail,
) : ShareRepository {
    private val dao: ShareDao = db.shareDao
    private val volumeTypeCache = ConcurrentHashMap<ShareId, Volume.Type>()

    override fun getSharesFlow(userId: UserId): Flow<DataResult<List<Share>>> =
        dao.getAllFlow(userId)
            .map { shareEntities ->
                shareEntities.map { shareEntity -> shareEntity.toShare(userId) }.asSuccess
            }

    override fun getSharesFlow(userId: UserId, volumeId: VolumeId): Flow<DataResult<List<Share>>> =
        dao.getAllFlow(userId, volumeId.id)
            .map { shareEntities ->
                shareEntities.map { shareEntity -> shareEntity.toShare(userId) }.asSuccess
            }

    override fun getSharesFlow(userId: UserId, shareType: Share.Type): Flow<DataResult<List<Share>>> =
        dao.getAllFlow(userId)
            .map { shareEntities ->
                shareEntities
                    .filter { shareEntity -> shareEntity.type.toShareType() == shareType }
                    .map { shareEntity -> shareEntity.toShare(userId) }
                    .asSuccess
            }

    override suspend fun hasShares(userId: UserId, shareType: Share.Type): Boolean =
        dao.hasShareEntities(userId, shareType.toLong())

    override suspend fun hasShares(userId: UserId, volumeId: VolumeId, shareType: Share.Type): Boolean =
        dao.hasShareEntities(userId, volumeId.id, shareType.toLong())

    override suspend fun fetchShares(userId: UserId, shareType: Share.Type): List<Share> {
        val localShares = dao.getAll(userId).associateBy { shareEntity -> shareEntity.id }
        return with(
            receiver = api.getShares(userId, shareType)
                .filter { share -> share.isActive }
                .map { share ->
                    val shareEntity = share.toShareEntity(userId)
                    localShares[share.id]?.editorsCanShare?.let { editorsCanShare ->
                        shareEntity.copy(
                            editorsCanShare = shareEntity.editorsCanShare ?: editorsCanShare,
                        )
                    } ?: shareEntity
                }
        ) {
            dao.insertOrUpdate(*toTypedArray())
            map { shareEntity -> shareEntity.toShare(userId) }
        }
    }

    override fun getShareFlow(shareId: ShareId): Flow<DataResult<Share>> =
        dao.getDistinctFlow(shareId.userId, shareId.id)
            .map { shareEntity: ShareEntity? -> shareEntity?.toShare(shareId.userId).asSuccessOrNullAsError() }

    override suspend fun hasShare(shareId: ShareId): Boolean =
        dao.hasShareEntity(shareId.userId, shareId.id)

    override suspend fun hasShareWithKey(shareId: ShareId): Boolean =
        dao.hasShareEntityWithKey(shareId.userId, shareId.id)

    override suspend fun fetchShare(shareId: ShareId) {
        val response = api.getShareBootstrap(shareId)
        val membershipAndEmail = response.memberships.firstOrNull()?.let { membershipDto ->
            membershipDto to getUserEmail(
                userId = shareId.userId,
                addressId = AddressId(membershipDto.addressId),
            ).getOrThrow()
        }
        db.inTransaction {
            dao.insertOrUpdate(response.toShareEntity(shareId.userId))
            if (membershipAndEmail != null) {
                val (membershipDto, email) = membershipAndEmail.first to membershipAndEmail.second
                db.shareMembershipDao.insertOrUpdate(
                    membershipDto.toShareUserMember(
                        shareId,
                        email
                    )
                )
            }
        }
    }

    override suspend fun deleteShare(shareId: ShareId, locallyOnly: Boolean, force: Boolean) {
        if (!locallyOnly) {
            api.deleteShare(shareId, force)
        }
        dao.delete(shareId.userId, shareId.id)
    }

    override suspend fun deleteShares(shareIds: List<ShareId>) =
        db.inTransaction {
            shareIds
                .groupBy({ shareId -> shareId.userId }) { shareId -> shareId.id }
                .forEach { (userId, shareIds) ->
                    dao.deleteAll(userId, shareIds)
                }
        }

    override suspend fun createShare(
        userId: UserId,
        volumeId: VolumeId,
        shareInfo: ShareInfo
    ): Result<ShareId> = coRunCatching {
        ShareId(
            userId = userId,
            id = api.createShare(userId, volumeId, shareInfo)
        )
    }

    override suspend fun hasMembership(shareId: ShareId): Boolean =
        db.shareMembershipDao.hasMembership(shareId.userId, shareId.id)

    override suspend fun getAllMembershipIds(userId: UserId): List<String> =
        db.shareMembershipDao.getAllIds(userId)

    override suspend fun getPermissions(
        shareIds: List<ShareId>,
    ): List<Permissions> = shareIds.takeUnless { ids -> ids.isEmpty() }?.let { ids ->
        db.shareMembershipDao.getPermissions(
            userId = ids.first().userId,
            shareIds = ids.map { it.id }
        ).map { value ->
            Permissions(value)
        }
    }.orEmpty()

    override suspend fun getVolumeType(shareId: ShareId): Result<Volume.Type> = coRunCatching {
        volumeTypeCache[shareId] ?: run {
            val cached = dao.get(
                userId = shareId.userId,
                shareId = shareId.id,
            )?.volumeType?.toVolumeType()

            val type = if (cached != null) {
                cached
            } else {
                fetchShare(shareId)
                dao.get(
                    userId = shareId.userId,
                    shareId = shareId.id,
                )?.volumeType?.toVolumeType()
            } ?: error("Cannot find volume type for ${shareId.id}")

            volumeTypeCache.putIfAbsent(shareId, type) ?: type
        }
    }

    override suspend fun setEditorsCanShare(
        shareId: ShareId,
        editorsCanShare: Boolean,
    ): Unit = api.setEditorsCanShare(shareId, editorsCanShare)

    override fun getMembership(shareId: ShareId): Flow<DataResult<ShareMembership>> =
        db.shareMembershipDao.getAll(
            userId = shareId.userId,
            shareId = shareId.id
        ).map { entities: List<ShareMembershipEntity> ->
            entities.firstOrNull()?.toShareMembership().asSuccessOrNullAsError()
        }
}

