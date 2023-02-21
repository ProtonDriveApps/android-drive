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
package me.proton.core.drive.key.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyId
import me.proton.core.drive.key.domain.repository.KeyRepository
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

/**
 * Get cached share [Key] if such exists otherwise builds a fresh one.
 */
class GetShareKey @Inject constructor(
    private val getShare: GetShare,
    private val keyRepository: KeyRepository,
    private val buildShareKey: BuildShareKey,
) {
    suspend operator fun invoke(share: Share): Result<Key.Node> = coRunCatching {
        val userId = share.id.userId
        keyRepository.getKey(userId, share.keyId) as? Key.Node
            ?: buildShareKey(share)
                .onSuccess { key -> keyRepository.addKey(userId, share.keyId, key) }
                .getOrThrow()
    }

    suspend operator fun invoke(shareId: ShareId): Result<Key.Node> = coRunCatching {
        invoke(
            share = getShare(shareId).toResult().getOrThrow(),
        ).getOrThrow()
    }
}
