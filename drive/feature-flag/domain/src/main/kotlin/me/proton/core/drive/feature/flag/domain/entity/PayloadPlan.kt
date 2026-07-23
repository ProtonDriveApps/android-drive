/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.feature.flag.domain.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import me.proton.core.drive.base.domain.entity.Bytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Serializable
data class PayloadPlanEnvelope(
    val schemaVersion: Int,
    val payload: JsonElement,
)

@Serializable
data class PayloadV1(
    val freePlan: FreePlan,
    val paidPlans: List<PaidPlan>,
) {

    @Serializable
    data class PaidPlan(
        val type: PlanType,
        val name: String,
        val shortTitle: String,
        val cycleMonths: Long,
        @SerialName("storageBytes")
        val storage: Bytes,
        val storageIncreaseFactor: Long,
        @Serializable(with = DaysDurationSerializer::class)
        @SerialName("historyDays")
        val history: Duration,
        val shareWithEditAccess: Boolean,
    )

    @Serializable
    data class FreePlan(
        val shortTitle: String,
        @SerialName("storageBytes")
        val storage: Bytes,
        @Serializable(with = DaysDurationSerializer::class)
        @SerialName("historyDays")
        val history: Duration,
        val shareWithEditAccess: Boolean,
    )

    enum class PlanType {
        DEFAULT,
        USA,
    }

    object DaysDurationSerializer : KSerializer<Duration> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Duration", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Duration) {
            encoder.encodeInt(value.inWholeDays.toInt())
        }

        override fun deserialize(decoder: Decoder): Duration {
            return decoder.decodeInt().days
        }
    }
}
