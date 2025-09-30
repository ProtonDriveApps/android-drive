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

package me.proton.core.drive.observability.domain.metrics

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.AppLoadType
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.DataSource
import me.proton.core.drive.observability.domain.metrics.common.mobile.performance.PageType
import me.proton.core.observability.domain.entity.SchemaId

@Serializable
@Schema(description = "Tracks the time to load first content after opening a tab.")
@SchemaId("https://proton.me/drive_mobile_performance_tabToFirstItem_histogram_v1.schema.json")
data class MobilePerformanceToFirstItemHistogram(
    override val Labels: LabelsData,
    @Required override val Value: Long,
) : DriveObservabilityData() {

    @Serializable
    data class LabelsData(
        val pageType: PageType,
        val appLoadType: AppLoadType,
        val dataSource: DataSource,
    )
}
