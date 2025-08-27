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

import me.proton.android.drive.photos.presentation.fastscroll.SeparatorWithIndex
import me.proton.android.drive.photos.presentation.fastscroll.SeparatorWithIndexAndCount
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.core.drive.base.domain.entity.FastScrollAnchor
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger

suspend fun List<PhotosItem>.getFastScrollAnchors(
    anchors: Int,
    anchorsInLabel: Int,
    getLabel: suspend (TimestampS) -> String,
): List<FastScrollAnchor> {
    CoreLogger.d(
        tag = LogTag.PHOTO,
        message = "getFastScrollAnchors(anchors=$anchors, anchorsInLabel=$anchorsInLabel) total photo items: $size",
    )
    if (anchors <= 0) return emptyList()
    val fastScrollAnchors: MutableList<FastScrollAnchor> = mutableListOf()
    val separators = this
        .asSequence()
        .withIndex()
        .filter { (_, item) -> item is PhotosItem.Separator }
        .map { (index, separator) ->
            SeparatorWithIndex(
                separator = separator as PhotosItem.Separator,
                index = index,
            )
        }
        .mapWithNext { current, next ->
            SeparatorWithIndexAndCount(
                separatorWithIndex = current,
                count = (next?.index ?: size) - current.index,
            ).also {
                CoreLogger.d(
                    tag = LogTag.PHOTO,
                    message =listOf(
                        "Triple(${it.separatorWithIndex.separator.year}",
                        "${it.separatorWithIndex.separator.month + 1}",
                        "${it.count - 1}),",
                    ).joinToString(","),
                )
            }
        }
    var availableAnchors = anchors - 1
    val separatorsToAnchors = separators.associateWith {
        if (availableAnchors > 0) {
            availableAnchors--
            1
        } else {
            0
        }
    }.toMutableMap()
    while (availableAnchors > 0) {
        separators
            .sortedByDescending { separator -> separator.count }
            .forEach { separator ->
                if (availableAnchors > 0) {
                    availableAnchors--
                    separatorsToAnchors[separator] = separatorsToAnchors.getOrDefault(separator, 0) + 1
                }
            }
    }
    var lastLabelAnchorIndex = -anchorsInLabel
    val labeledYears: MutableSet<Int> = mutableSetOf()
    var anchorIndex = 0
    separatorsToAnchors.forEach { (separatorWithIndexAndCount, separatorAnchors) ->
        for (i in 0..<separatorAnchors) {
            val label = if (!labeledYears.contains(separatorWithIndexAndCount.separatorWithIndex.separator.year) && lastLabelAnchorIndex + anchorsInLabel <= (anchorIndex + i)) {
                "${separatorWithIndexAndCount.separatorWithIndex.separator.year}".also {
                    lastLabelAnchorIndex = anchorIndex + i
                    labeledYears.add(separatorWithIndexAndCount.separatorWithIndex.separator.year)
                }
            } else {
                null
            }
            val scrollToPosition = if (i == 0) {
                separatorWithIndexAndCount.separatorWithIndex.index
            } else {
                if (isFastScrollThresholdReached(separatorWithIndexAndCount.count, anchors, anchorsInLabel)) {
                    separatorWithIndexAndCount.separatorWithIndex.index + (i * (separatorWithIndexAndCount.count / separatorAnchors.toFloat())).toInt()
                } else {
                    null
                }
            }
            val dragLabel = if (scrollToPosition == null) {
                null
            } else {
                getLabel(separatorWithIndexAndCount.separatorWithIndex.separator.afterCaptureTime)
            }
            fastScrollAnchors.add(
                FastScrollAnchor(
                    scrollToPosition = scrollToPosition,
                    label = label,
                    dragLabel = dragLabel,
                )
            )
            anchorIndex++
        }
    }
    fastScrollAnchors.add(
        FastScrollAnchor(
            scrollToPosition = size - 1,
            dragLabel = getLabel(separators.last().separatorWithIndex.separator.afterCaptureTime),
        )
    )
    return fastScrollAnchors
}

fun <T, R> Sequence<T>.mapWithNext(transform: (T, T?) -> R): Sequence<R> = sequence {
    val iterator = this@mapWithNext.iterator()
    if (!iterator.hasNext()) return@sequence

    var current = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        yield(transform(current, next))
        current = next
    }
    yield(transform(current, null))
}

fun isFastScrollThresholdReached(items: Int, anchors: Int, anchorsInLabel: Int): Boolean =
    items > (anchors / anchorsInLabel) * 10
