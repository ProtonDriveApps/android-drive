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

package me.proton.android.drive.verifier.data.api

import me.proton.android.drive.verifier.data.api.response.GetVerificationDataResponse
import me.proton.android.drive.verifier.domain.entity.VerificationData
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException

class VerifierApiDataSource(private val apiProvider: ApiProvider) {

    @Throws(ApiException::class)
    suspend fun getVerificationData(
        userId: UserId,
        shareId: String,
        linkId: String,
        revisionId: String,
    ): GetVerificationDataResponse =
        apiProvider.get<VerifierApi>(userId).invoke { getVerificationData(shareId, linkId, revisionId) }.valueOrThrow
}
