/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.verifier.data.repository

import android.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.android.drive.verifier.data.api.VerifierApiDataSource
import me.proton.android.drive.verifier.domain.entity.VerificationData
import me.proton.android.drive.verifier.domain.repository.VerifierRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class VerifierRepositoryImpl @Inject constructor(
    private val api: VerifierApiDataSource,
) : VerifierRepository {
    internal val verificationDataCache: MutableMap<VerificationDataKey, VerificationData> = mutableMapOf()
    private val mutex = Mutex()

    override suspend fun getVerificationData(
        userId: UserId,
        shareId: String,
        linkId: String,
        revisionId: String,
    ): VerificationData = getOrFetch(VerificationDataKey(userId, shareId, linkId, revisionId))

    override suspend fun removeVerificationData(
        userId: UserId,
        shareId: String,
        linkId: String,
        revisionId: String,
    ) {
        remove(
            VerificationDataKey(userId, shareId, linkId, revisionId)
        )
    }

    private suspend fun getOrFetch(key: VerificationDataKey): VerificationData = get(key) ?: fetchAndStore(key)

    private suspend fun fetchAndStore(key: VerificationDataKey): VerificationData =
        api.getVerificationData(key.userId, key.shareId, key.linkId, key.revisionId).let { response ->
            VerificationData(
                contentKeyPacket = response.contentKeyPacket,
                verificationCode = Base64.decode(response.verificationCode, Base64.NO_WRAP),
            )
        }.also { verificationData ->
            put(key, verificationData)
        }

    internal suspend fun get(key: VerificationDataKey): VerificationData? = mutex.withLock {
        verificationDataCache[key]
    }

    internal suspend fun put(key: VerificationDataKey, verificationData: VerificationData) = mutex.withLock {
        verificationDataCache[key] = verificationData
    }

    internal suspend fun remove(key: VerificationDataKey) = mutex.withLock {
        verificationDataCache.remove(key)
    }

    data class VerificationDataKey(
        val userId: UserId,
        val shareId: String,
        val linkId: String,
        val revisionId: String,
    )
}
