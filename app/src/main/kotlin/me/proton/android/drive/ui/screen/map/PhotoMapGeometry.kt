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

package me.proton.android.drive.ui.screen.map

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.tan
import kotlinx.serialization.Serializable

internal data class MapDateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(start <= end)
    }

    fun startSeconds(zone: ZoneId): Long = start.atStartOfDay(zone).toEpochSecond()
    fun endExclusiveSeconds(zone: ZoneId): Long = end.plusDays(1).atStartOfDay(zone).toEpochSecond()

    fun including(other: MapDateRange) = MapDateRange(minOf(start, other.start), maxOf(end, other.end))

    companion object {
        fun fromCaptureTimes(first: Long, last: Long, zone: ZoneId): MapDateRange {
            val firstDate = captureDate(first, zone)
            val lastDate = captureDate(last, zone)
            return MapDateRange(minOf(firstDate, lastDate), maxOf(firstDate, lastDate))
        }
    }
}

@Serializable
internal data class MapPoint(val x: Double, val y: Double)

internal data class MapCamera(val center: MapPoint, val zoom: Double) {
    fun worldSize(tileSize: Double): Double = tileSize * 2.0.pow(zoom)

    fun move(
        panX: Double,
        panY: Double,
        anchorX: Double,
        anchorY: Double,
        zoomChange: Double,
        tileSize: Double,
    ): MapCamera {
        val newZoom = (zoom + zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val before = worldSize(tileSize)
        val after = tileSize * 2.0.pow(newZoom)
        return MapCamera(
            center = MapPoint(
                x = wrapWorld(center.x + anchorX / before - (anchorX + panX) / after),
                y = (center.y + anchorY / before - (anchorY + panY) / after).coerceIn(0.0, 1.0),
            ),
            zoom = newZoom,
        )
    }

    companion object {
        val World = MapCamera(MapPoint(0.5, 0.5), 1.0)
    }
}

internal const val MIN_ZOOM = 0.0
internal const val MAX_ZOOM = 19.0
internal const val MAP_TILE_DP = 256
internal const val MAP_PIN_DP = 48
internal const val MAP_CLUSTER_DISTANCE_DP = 32

internal fun clusterZoom(zoom: Double): Double = floor(zoom * 4) / 4

internal fun mapPoint(latitude: Double, longitude: Double): MapPoint? {
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    val radians = Math.toRadians(latitude.coerceIn(-85.05112878, 85.05112878))
    return MapPoint(
        x = wrapWorld((longitude + 180.0) / 360.0),
        y = ((1.0 - asinh(tan(radians)) / PI) / 2.0).coerceIn(0.0, 1.0),
    )
}

internal fun wrapWorld(x: Double): Double = x - floor(x)

internal fun wrappedDistance(x: Double, origin: Double): Double = wrapWorld(x - origin + 0.5) - 0.5

internal fun fitMapCamera(
    points: List<MapPoint>,
    width: Double,
    height: Double,
    tileSize: Double,
    padding: Double,
): MapCamera {
    if (points.isEmpty()) return MapCamera.World
    val x = points.map(MapPoint::x).sorted()
    val widestGap = x.indices.maxBy { index ->
        val next = x.getOrNull(index + 1) ?: (x.first() + 1.0)
        next - x[index]
    }
    val left = x.getOrNull(widestGap + 1) ?: x.first()
    val right = x[widestGap] + if (widestGap < x.lastIndex) 1.0 else 0.0
    val top = points.minOf(MapPoint::y)
    val bottom = points.maxOf(MapPoint::y)
    val horizontalZoom = log2((width - 2 * padding).coerceAtLeast(1.0) / (tileSize * (right - left)))
    val verticalZoom = log2((height - 2 * padding).coerceAtLeast(1.0) / (tileSize * (bottom - top)))
    return MapCamera(
        center = MapPoint(wrapWorld((left + right) / 2), (top + bottom) / 2),
        zoom = minOf(horizontalZoom, verticalZoom, 14.0).coerceAtLeast(MIN_ZOOM),
    )
}

internal data class MapPin<T>(val point: MapPoint, val photos: List<T>)

private data class PinCell(val column: Int, val row: Int)

internal fun <T> clusterMapPins(
    photos: List<T>,
    zoom: Double,
    pointOf: (T) -> MapPoint,
): List<MapPin<T>> {
    // Groups are connected components of nearby points. As the distance
    // shrinks, a group can split but two old groups cannot join.
    val distance = MAP_CLUSTER_DISTANCE_DP / (MAP_TILE_DP * 2.0.pow(zoom))
    val columns = ceil(1.0 / distance).toInt()
    val points = photos.map(pointOf)
    val cells = points.map { PinCell((it.x * columns).toInt(), (it.y * columns).toInt()) }
    val members = points.indices.groupBy { cells[it] }
    val occupied = members.keys.toList()
    val indexByCell = occupied.withIndex().associate { (index, cell) -> cell to index }
    val parents = IntArray(occupied.size) { it }

    fun root(cellIndex: Int): Int {
        var index = cellIndex
        while (parents[index] != index) {
            parents[index] = parents[parents[index]]
            index = parents[index]
        }
        return index
    }

    // Every point in a cell is within range of every other point there. Only test pairs
    // across nearby cells, wrapping columns when crossing the antimeridian.
    for ((index, cell) in occupied.withIndex()) {
        for (dx in -2..2) {
            val column = Math.floorMod(cell.column + dx, columns)
            for (dy in -2..2) {
                val neighbor = PinCell(column, cell.row + dy)
                val neighborIndex = indexByCell[neighbor] ?: continue
                if (neighborIndex >= index || root(index) == root(neighborIndex)) continue
                val overlap = members.getValue(cell).any { first ->
                    members.getValue(neighbor).any { second ->
                        abs(wrappedDistance(points[first].x, points[second].x)) <= distance &&
                            abs(points[first].y - points[second].y) <= distance
                    }
                }
                if (overlap) parents[root(neighborIndex)] = root(index)
            }
        }
    }
    return points.indices.groupBy { root(indexByCell.getValue(cells[it])) }.values.map { group ->
        MapPin(points[group.first()], group.map { photos[it] })
    }
}

internal fun captureDate(seconds: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochSecond(seconds).atZone(zone).toLocalDate()

internal fun <T> groupMapPhotosByDate(
    photos: List<T>,
    zone: ZoneId,
    captureSeconds: (T) -> Long,
): Map<LocalDate, List<T>> = photos.sortedByDescending(captureSeconds)
    .groupBy { captureDate(captureSeconds(it), zone) }

internal data class MapPathSegment<T>(val from: T, val to: T)

internal fun <T> dailyMapPathSegments(
    photos: List<T>,
    zone: ZoneId,
    captureSeconds: (T) -> Long,
): List<MapPathSegment<T>> = groupMapPhotosByDate(photos, zone, captureSeconds)
    .values
    .flatMap { day -> day.asReversed().zipWithNext(::MapPathSegment) }
