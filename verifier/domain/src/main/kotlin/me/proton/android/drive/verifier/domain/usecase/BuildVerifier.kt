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

package me.proton.android.drive.verifier.domain.usecase

import me.proton.android.drive.verifier.domain.entity.Verifier
import me.proton.android.drive.verifier.domain.exception.VerifierException
import me.proton.android.drive.verifier.domain.factory.VerifierFactory
import me.proton.android.drive.verifier.domain.repository.VerifierRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.BuildContentKey
import javax.inject.Inject

class BuildVerifier @Inject constructor(
    private val repository: VerifierRepository,
    private val buildContentKey: BuildContentKey,
    private val factory: VerifierFactory,
) {
    suspend operator fun invoke(
        userId: UserId,
        shareId: String,
        linkId: String,
        revisionId: String,
        fileKey: Key.Node,
    ): Result<Verifier> = try {
        val verificationData = repository.getVerificationData(userId, shareId, linkId, revisionId)
        val contentKey = buildContentKey(
            userId = userId,
            contentKeyPacket = verificationData.contentKeyPacket,
            contentKeyPacketSignature = "",
            fileKey = fileKey
        ).getOrThrow()
        Result.success(factory.create(
            userId = userId,
            contentKey = contentKey,
            verificationCode = verificationData.verificationCode,
        ))
    } catch (t: Throwable) {
        Result.failure(VerifierException.Initialize(t))
    }
}
