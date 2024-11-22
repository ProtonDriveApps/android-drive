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

package me.proton.core.drive.observability.domain.metrics

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import me.proton.core.drive.observability.domain.metrics.common.BooleanStatus
import me.proton.core.drive.observability.domain.metrics.common.Initiator
import me.proton.core.drive.observability.domain.metrics.common.ResultStatus
import me.proton.core.drive.observability.domain.metrics.common.ShareType
import me.proton.core.observability.domain.entity.SchemaId

@Serializable
@Schema(description = "Measures unique successful or failed uploads and number of retries")
@SchemaId("https://proton.me/drive_upload_success_rate_total_v2.schema.json")
data class UploadSuccessRateTotal(
    override val Labels: LabelsData,
    @Required override val Value: Long = 1,
) : DriveObservabilityData() {

    @Serializable
    data class LabelsData(
        val status: ResultStatus,
        val retry: BooleanStatus,
        val shareType: ShareType,
        val initiator: Initiator,
    )
}
