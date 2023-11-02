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

import me.proton.android.drive.verifier.domain.repository.VerifierRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class CleanupVerifier @Inject constructor(
    private val repository: VerifierRepository,
) {
    suspend operator fun invoke(userId: UserId, shareId: String, linkId: String, revisionId: String) =
        repository.removeVerificationData(userId, shareId, linkId, revisionId)
}
