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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMapGeometryTest {
    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun `relative date shortcuts include today and follow calendar month and year boundaries`() {
        val today = LocalDate.of(2024, 3, 31)

        assertEquals(MapDateRange(LocalDate.of(2024, 3, 25), today), MapDatePreset.LAST_WEEK.range(today))
        assertEquals(MapDateRange(LocalDate.of(2024, 3, 1), today), MapDatePreset.LAST_MONTH.range(today))
        assertEquals(MapDateRange(LocalDate.of(2023, 4, 1), today), MapDatePreset.LAST_YEAR.range(today))
        assertNull(MapDatePreset.ALL_TIME.range(today))
    }

    @Test
    fun `custom dates stay fixed even when they originally match a shortcut`() {
        val today = LocalDate.of(2026, 4, 18)
        val preset = MapDateSelection.Preset(MapDatePreset.LAST_WEEK)
        val original = requireNotNull(preset.range(today))
        val custom = MapDateSelection.Custom(original)

        assertEquals(original, custom.range(today.plusDays(1)))
        assertEquals(MapDateRange(original.start.plusDays(1), original.end.plusDays(1)), preset.range(today.plusDays(1)))
    }

    @Test
    fun `date bounds include the first and last photo across several years`() {
        val first = LocalDate.of(1968, 9, 3)
        val last = LocalDate.of(2026, 4, 18)
        val range = MapDateRange.fromCaptureTimes(
            first.atStartOfDay(zone).toEpochSecond(),
            last.atTime(23, 59).atZone(zone).toEpochSecond(),
            zone,
        )

        assertEquals(MapDateRange(first, last), range)
        assertEquals(first, captureDate(range.startSeconds(zone), zone))
        assertEquals(last, captureDate(range.endExclusiveSeconds(zone) - 1, zone))
    }

    @Test
    fun `date bounds support clocks moving back across midnight`() {
        val zone = ZoneId.of("America/St_Johns")
        val first = Instant.parse("2009-11-01T02:30:00Z").epochSecond
        val last = Instant.parse("2009-11-01T02:31:00Z").epochSecond

        assertEquals(
            MapDateRange(LocalDate.of(2009, 10, 31), LocalDate.of(2009, 11, 1)),
            MapDateRange.fromCaptureTimes(first, last, zone),
        )
    }

    @Test
    fun `editing one endpoint preserves the other`() {
        val today = LocalDate.of(2026, 4, 18)
        val range = MapDateRange(today.minusDays(29), today)
        val bounds = MapDateRange(today.minusYears(5), today.plusYears(1))

        assertEquals(MapDateRange(bounds.start, range.end), MapDateEndpoint.START.allowedDates(range, bounds))
        assertEquals(MapDateRange(range.start, bounds.end), MapDateEndpoint.END.allowedDates(range, bounds))
        assertEquals(range.copy(start = today.minusDays(7)), MapDateEndpoint.START.withDate(range, today.minusDays(7)))
        assertEquals(range.copy(end = today.plusDays(1)), MapDateEndpoint.END.withDate(range, today.plusDays(1)))
        assertEquals(MapDateRange(today, today), MapDateEndpoint.START.withDate(range, today.plusDays(1)))
    }

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
    fun `paths follow group pins and restore individual links when zooming in`() {
        val pin = MapPoint(0.5, 0.5)
        val nearby = MapPoint(0.5 + 36.0 / (MAP_TILE_DP * 1024), 0.5)
        val far = MapPoint(0.6, 0.6)
        val photos = listOf(pin, nearby, far)
        val paths = listOf(MapPathSegment(pin, nearby), MapPathSegment(nearby, far), MapPathSegment(far, nearby))

        val grouped = clusterMapPins(photos, 9.0) { it }
        val separate = clusterMapPins(photos, 10.0) { it }

        assertEquals(listOf(MapPathSegment(pin, far), MapPathSegment(far, pin)), mapPathSegmentsToPins(paths, grouped))
        assertEquals(paths, mapPathSegmentsToPins(paths, separate))
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
    fun `nearly identical locations stay grouped at maximum zoom`() {
        val north = requireNotNull(mapPoint(0.000001, 0.0))
        val south = requireNotNull(mapPoint(-0.000001, 0.0))

        assertEquals(2, clusterMapPins(listOf(north, south), MAX_ZOOM) { it }.single().photos.size)
    }

    @Test
    fun `nearby pins split before their markers stop overlapping`() {
        val points = listOf(MapPoint(0.5, 0.5), MapPoint(0.5 + 36.0 / (MAP_TILE_DP * 1024), 0.5))

        assertEquals(listOf(points), clusterMapPins(points, 9.0) { it }.map { it.photos })
        assertEquals(2, clusterMapPins(points, 10.0) { it }.size)
    }

    @Test
    fun `zooming in splits groups without moving photos between old groups`() {
        val radius = MAP_CLUSTER_DISTANCE_DP.toDouble() / (MAP_TILE_DP * 1024)
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
