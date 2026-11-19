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

package me.proton.android.drive.ui.viewmodel

import kotlinx.serialization.json.Json
import me.proton.android.drive.ui.screen.map.mapPoint
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.drive.sdk.entity.AdditionalMetadataProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMapMetadataTest {
    private val share = ShareId(UserId("user"), "share")

    @Test
    fun `a decrypted SDK location can place a pin without image data`() {
        val metadata = listOf(
            metadataProperty("Media", """{"Width": 4000, "Height": 3000}"""),
            metadataProperty("Location", """{"Latitude": 40.4168, "Longitude": -3.7038, "Accuracy": 5}"""),
        )

        assertEquals(
            PhotoLocation.Located(requireNotNull(mapPoint(40.4168, -3.7038))),
            sdkPhotoLocation(metadata, hasErrors = false),
        )
    }

    @Test
    fun `a successful lookup without GPS metadata counts as missing`() {
        val cameraOnly = listOf(metadataProperty("Camera", """{"CaptureTime": "2026-11-19"}"""))

        assertEquals(PhotoLocation.Missing, sdkPhotoLocation(null, hasErrors = false))
        assertEquals(PhotoLocation.Missing, sdkPhotoLocation(emptyList(), hasErrors = false))
        assertEquals(PhotoLocation.Missing, sdkPhotoLocation(cameraOnly, hasErrors = false))
    }

    @Test
    fun `malformed or invalid location metadata is unavailable`() {
        val incomplete = listOf(metadataProperty("Location", """{"Latitude": 40}"""))
        val outOfRange = listOf(metadataProperty("Location", """{"Latitude": 100, "Longitude": -3}"""))

        assertEquals(PhotoLocation.Unavailable, sdkPhotoLocation(incomplete, hasErrors = false))
        assertEquals(PhotoLocation.Unavailable, sdkPhotoLocation(outOfRange, hasErrors = false))
    }

    @Test
    fun `an SDK error does not confirm that a photo lacks GPS`() {
        assertEquals(PhotoLocation.Unavailable, sdkPhotoLocation(null, hasErrors = true))
    }

    @Test
    fun `confirmed locations and confirmed absence round trip through the existing text cache format`() {
        val point = requireNotNull(mapPoint(40.4168, -3.7038))
        val located = CachedSdkLocation(revisionId = "revision", cachedAt = 10L, point = point, hasThumbnail = true)
        val absent = CachedSdkLocation(revisionId = "revision", cachedAt = 10L)

        assertEquals(PhotoLocation.Located(point), Json.decodeFromString<CachedSdkLocation>(Json.encodeToString(located)).location)
        assertEquals(PhotoLocation.Missing, Json.decodeFromString<CachedSdkLocation>(Json.encodeToString(absent)).location)
        assertEquals(located, Json.decodeFromString<CachedSdkLocation>(Json.encodeToString(located)))
    }

    @Test
    fun `cache requires the current revision or a fresh result when revision is unknown`() {
        val found = CachedSdkLocation(revisionId = "revision", cachedAt = 10L)

        assertTrue(found.canReuse("revision", now = 600_010L))
        assertFalse(found.canReuse("replacement", now = 11L))
        assertTrue(found.canReuse(null, now = 600_009L))
        assertFalse(found.canReuse(null, now = 600_010L))
    }

    @Test
    fun `geolocation progress counts distinct listed and examined photos`() {
        val first = FileId(share, "first")
        val second = FileId(share, "second")
        val progress = PhotoLocationProgress()

        progress.includeListings(listOf(first, first, second))
        progress.recordChecked(first)
        progress.recordChecked(first)
        progress.recordChecked(FileId(share, "outside scope"))

        assertEquals(2, progress.total)
        assertEquals(1, progress.checked)
    }

    @Test
    fun `listing refresh retains checks only for photos still in scope`() {
        val removed = FileId(share, "removed")
        val retained = FileId(share, "retained")
        val added = FileId(share, "added")
        val progress = PhotoLocationProgress()
        progress.includeListings(listOf(removed, retained))
        progress.recordChecked(removed)
        progress.recordChecked(retained)

        progress.replaceListings(setOf(retained, added))

        assertEquals(2, progress.total)
        assertEquals(1, progress.checked)
    }

    private fun metadataProperty(name: String, json: String) =
        AdditionalMetadataProperty(name, Json.parseToJsonElement(json))
}
