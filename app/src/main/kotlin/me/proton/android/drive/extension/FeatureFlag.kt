/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.android.drive.extension

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import me.proton.android.drive.entity.UpsellPlanConfig
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.PayloadPlanEnvelope
import me.proton.core.drive.feature.flag.domain.entity.PayloadV1

fun FeatureFlag.getUpsellPlanConfig(): UpsellPlanConfig? = takeIf {
    payloadType != null && payloadType.equals("json", ignoreCase = true)
}
    ?.let {
        payloadValue?.let { payload ->
            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
            coRunCatching {
                json.decodeFromString<PayloadPlanEnvelope>(payload)
            }
                .getOrNull(LogTag.FEATURE_FLAG, "Json decode $payload into PayloadPlanEnvelope failed.")
                ?.let { envelope ->
                    when (envelope.schemaVersion) {
                        1 -> coRunCatching {
                            val payloadV1 = json.decodeFromJsonElement<PayloadV1>( envelope.payload)
                            UpsellPlanConfig(
                                freePlan = payloadV1.freePlan,
                                paidPlan = payloadV1.paidPlans.first { paidPlan -> paidPlan.type == PayloadV1.PlanType.DEFAULT },
                            )
                        }.getOrNull(LogTag.FEATURE_FLAG, "Json decode ${envelope.payload} into PayloadV1 failed.")
                        else -> null
                    }
                }
        }
    }
