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

package me.proton.core.drive.share.data.test.repository
/*
 * Copyright (c) 2023 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.share.data.test.nullable.NullableShare
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.entity.ShareInfo
import me.proton.core.drive.share.domain.repository.ShareRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class StubbedShareRepository @Inject constructor() : ShareRepository {

    private val sharesFlow = MutableStateFlow(
        listOf(
            NullableShare(
                mainShareId,
                isMain = true,
                key = "key",
                passphrase = "passphrase",
                passphraseSignature = "passphraseSignature",
            )
        )
    )

    override fun getSharesFlow(userId: UserId): Flow<DataResult<List<Share>>> =
        filterShares { share -> share.id.userId == userId }.map { it.asSuccess }

    override fun getSharesFlow(userId: UserId, volumeId: VolumeId): Flow<DataResult<List<Share>>> =
        filterShares { share -> share.id.userId == userId && share.volumeId == volumeId }.map { it.asSuccess }

    override suspend fun hasShares(userId: UserId): Boolean {
        return fetchShares(userId).isNotEmpty()
    }

    override suspend fun hasShares(userId: UserId, volumeId: VolumeId): Boolean {
        return filterShares { share -> share.id.userId == userId }.first().isNotEmpty()
    }

    override suspend fun fetchShares(userId: UserId): List<Share> {
        return filterShares { share -> share.id.userId == userId }.first()
    }

    override fun getShareFlow(shareId: ShareId): Flow<DataResult<Share>> {
        return filterShares { share -> share.id == shareId }.map { it.first().asSuccess }
    }

    override suspend fun hasShare(shareId: ShareId): Boolean {
        return filterShares { share -> share.id == shareId }.firstOrNull() != null
    }

    override suspend fun hasShareWithKey(shareId: ShareId): Boolean {
        return filterShares { share -> share.id == shareId && share.key.isNotEmpty() }.firstOrNull() != null
    }

    override suspend fun fetchShare(shareId: ShareId) {
        sharesFlow.value = sharesFlow.value + NullableShare(shareId)
    }

    override suspend fun deleteShare(shareId: ShareId, locallyOnly: Boolean) {
        deleteShares(listOf(shareId))
    }

    override suspend fun deleteShares(shareIds: List<ShareId>) {
        sharesFlow.value = sharesFlow.value.filterNot { share -> share.id in shareIds }
    }

    override suspend fun createShare(
        userId: UserId,
        volumeId: VolumeId,
        shareInfo: ShareInfo
    ): Result<ShareId> {
        val id = ShareId(userId, "share-${shareInfo.name}")
        sharesFlow.value = sharesFlow.value + NullableShare(
            id = id,
            volumeId = volumeId,
            addressId = shareInfo.addressId,
            key = shareInfo.shareKey,
            passphrase = shareInfo.sharePassphrase,
            passphraseSignature = shareInfo.sharePassphraseSignature,
            rootLinkId = shareInfo.rootLinkId,
        )
        return Result.success(id)
    }

    private fun filterShares(
        filter: (Share) -> Boolean
    ) = sharesFlow.filter { shares -> shares.any(filter) }

    companion object {
        val mainShareId = ShareId(UserId("user-id"), "share-main")
    }
}