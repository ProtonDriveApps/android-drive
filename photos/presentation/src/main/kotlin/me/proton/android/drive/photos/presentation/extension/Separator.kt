/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.presentation.extension

import me.proton.android.drive.photos.presentation.fastscroll.YearSeparators
import me.proton.core.drive.base.domain.entity.FastScrollAnchor
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger

fun YearSeparators.getFastScrollAnchors(
    anchors: Int,
    getLabel: (TimestampS) -> String,
): List<FastScrollAnchor> {
    CoreLogger.d(
        tag = LogTag.PHOTO,
        message = buildString {
            listOf(
                "Year: $year",
                "total photo items: $count",
                "number of separators: ${separators.size}",
                "number of anchors: $anchors"
            ).joinToString()

        }
    )
    if (anchors <= 0) return emptyList()
    var totalAnchors = anchors
    require(separators.isNotEmpty()) { "Unexpected empty separators list" }
    val fastScrollAnchors: MutableList<FastScrollAnchor> = mutableListOf()
    fastScrollAnchors.add(
        FastScrollAnchor(
            scrollToPosition = separators.first().separatorWithIndex.index,
            label = "$year",
            dragLabel = getLabel(separators.first().separatorWithIndex.separator.afterCaptureTime),
        )
    )
    if (--totalAnchors == 0) return fastScrollAnchors

    val positionIndex = separators.first().separatorWithIndex.index
    val step = count / anchors
    val step33 = step / 3
    for (i in 1..totalAnchors) {
        val targetPosition = positionIndex + (step * i)
        val anchorPosition = separators
            .firstOrNull { separator ->
                separator.separatorWithIndex.index in IntRange(
                    targetPosition - step33,
                    targetPosition + step33,
                )
            }
            ?.separatorWithIndex
            ?.index
            ?: targetPosition
        val dragLabel = getLabel(
            separators
                .first { separator ->
                    anchorPosition in IntRange(
                        separator.separatorWithIndex.index,
                        separator.separatorWithIndex.index + separator.count - 1,
                    )
                }
                .separatorWithIndex.separator.afterCaptureTime
        )
        fastScrollAnchors.add(
            FastScrollAnchor(
                scrollToPosition = anchorPosition,
                dragLabel = dragLabel,
            )
        )
    }
    require(anchors == fastScrollAnchors.size) {
        "Wrong fast scroll anchors size, expected: $anchors, actual: ${fastScrollAnchors.size}"
    }
    return fastScrollAnchors
}
