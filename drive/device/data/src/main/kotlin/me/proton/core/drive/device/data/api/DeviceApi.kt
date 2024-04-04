/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.device.data.api

import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.device.data.api.request.UpdateDeviceRequest
import me.proton.core.drive.device.data.api.response.GetDevicesResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface DeviceApi : BaseRetrofitApi {

    @GET("drive/devices")
    suspend fun getDevices(): GetDevicesResponse

    @PUT("drive/devices/{deviceID}")
    suspend fun updateDevice(
        @Path("deviceID") deviceId: String,
        @Body request: UpdateDeviceRequest,
    ): CodeResponse

    @DELETE("drive/devices/{deviceID}")
    suspend fun deleteDevice(
        @Path("deviceID") deviceId: String,
    ): CodeResponse
}
