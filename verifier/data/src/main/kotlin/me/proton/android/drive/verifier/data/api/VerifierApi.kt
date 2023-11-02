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
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.GET
import retrofit2.http.Path

interface VerifierApi : BaseRetrofitApi {

    @GET("drive/shares/@{enc_shareID}/links/@{enc_linkID}/revisions/@{enc_revisionID}/verification")
    suspend fun getVerificationData(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String,
        @Path("enc_revisionID") revisionId: String,
    ): GetVerificationDataResponse
}
