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

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMapGeometryTest {
    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun `inclusive range follows local midnight through daylight saving`() {
        val date = LocalDate.of(2026, 3, 29)
        val range = MapDateRange(date, date)
        val start = range.startSeconds(zone)
        val end = range.endExclusiveSeconds(zone)

        assertEquals(23 * 3600L, end - start)
        assertEquals(date, captureDate(start, zone))
        assertEquals(date, captureDate(end - 1, zone))
        assertEquals(date.plusDays(1), captureDate(end, zone))
    }

    @Test
    fun `path segments follow capture time without joining across local midnight`() {
        val date = LocalDate.of(2026, 3, 29)
        val start = date.atStartOfDay(zone).toEpochSecond()
        val midnight = date.plusDays(1).atStartOfDay(zone).toEpochSecond()
        val photos = listOf(midnight + 1, midnight - 1, start, midnight, start + 1)

        val segments = dailyMapPathSegments(photos, zone) { it }

        assertEquals(
            listOf(
                MapPathSegment(midnight, midnight + 1),
                MapPathSegment(start, start + 1),
                MapPathSegment(start + 1, midnight - 1),
            ),
            segments,
        )
    }

    @Test
    fun `one photo on each of two days produces no path`() {
        val midnight = LocalDate.of(2026, 7, 2).atStartOfDay(zone).toEpochSecond()

        assertTrue(dailyMapPathSegments(listOf(midnight - 1, midnight), zone) { it }.isEmpty())
    }

    @Test
    fun `close pins separate with zoom even across the antimeridian`() {
        val west = requireNotNull(mapPoint(0.0, -179.9))
        val east = requireNotNull(mapPoint(0.0, 179.9))
        val points = listOf(west, east)

        assertEquals(listOf(points), clusterMapPins(points, 1.0) { it }.map { it.photos })
        assertEquals(2, clusterMapPins(points, 14.0) { it }.size)

        val camera = fitMapCamera(points, 300.0, 500.0, 256.0, 12.0)
        assertTrue(camera.zoom > 9.0)
        assertEquals(0.0, wrappedDistance(camera.center.x, 0.0), 1e-9)
    }

    @Test
    fun `pins with overlapping hit targets still group at maximum zoom`() {
        val north = requireNotNull(mapPoint(0.000001, 0.0))
        val south = requireNotNull(mapPoint(-0.000001, 0.0))

        assertEquals(2, clusterMapPins(listOf(north, south), MAX_ZOOM) { it }.single().photos.size)
    }

    @Test
    fun `zooming in splits groups without moving photos between old groups`() {
        val radius = 48.0 / (256 * 1024)
        val first = MapPoint(0.5, 0.5)
        val second = MapPoint(0.5 + 0.75 * radius, 0.5)
        val third = MapPoint(0.5 + 1.1 * radius, 0.5)
        val points = listOf(first, second, third)

        assertEquals(listOf(points), clusterMapPins(points, 10.0) { it }.map { it.photos })
        assertEquals(listOf(listOf(first), listOf(second, third)), clusterMapPins(points, 11.0) { it }.map { it.photos })
    }

    @Test
    fun `zero coordinates are valid and corrupt metadata is rejected`() {
        assertNotNull(mapPoint(0.0, 0.0))
        assertNull(mapPoint(Double.NaN, 0.0))
        assertNull(mapPoint(0.0, 181.0))
    }
}
