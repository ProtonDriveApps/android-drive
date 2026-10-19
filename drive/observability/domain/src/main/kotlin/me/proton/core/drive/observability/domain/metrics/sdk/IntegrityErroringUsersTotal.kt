/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.observability.domain.metrics.sdk

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import me.proton.core.drive.observability.domain.metrics.common.Plan
import me.proton.core.drive.observability.domain.metrics.common.VolumeType
import me.proton.core.drive.observability.domain.metrics.sdk.IntegrityErroringUsersTotal.Companion.SCHEMA_ID
import me.proton.core.drive.observability.domain.metrics.sdk.common.CausedBy
import me.proton.core.observability.domain.entity.SchemaId

@Serializable
@Schema(description = "How many users experienced decryption or verification error in the past 5 minutes")
@SchemaId(SCHEMA_ID)
data class IntegrityErroringUsersTotal(
    override val Labels: LabelsData,
    @Required override val Value: Long = 1,
) : DriveObservabilityData() {

    @Serializable
    data class LabelsData(
        val volumeType: VolumeType,
        val userPlan: Plan,
        val causedBy: CausedBy,
    )

    companion object {
        const val SCHEMA_ID = "https://proton.me/drive_sdk_integrity_erroring_users_total_v1.schema.json"
    }
}
