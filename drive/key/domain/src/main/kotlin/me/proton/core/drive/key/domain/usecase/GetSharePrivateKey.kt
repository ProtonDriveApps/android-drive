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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.repository.KeyRepository
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

/**
 * Get cached share [Key] if such exists otherwise builds a fresh one.
 */
class GetSharePrivateKey @Inject constructor(
    private val keyRepository: KeyRepository,
    private val buildSharePrivateKey: BuildSharePrivateKey,
) {
    suspend operator fun invoke(
        userId: UserId,
        email: String,
        shareKey: String,
        passphrase: String,
    ): Result<Key.Node> = coRunCatching {
        val keyId = email + passphrase
        keyRepository.getKey(userId, keyId) as? Key.Node
            ?: buildSharePrivateKey(
                userId = userId,
                email = email,
                shareKey = shareKey,
                passphrase = passphrase
            ).onSuccess { key ->
                keyRepository.addKey(userId, keyId, key)
            }.getOrThrow()
    }
}
