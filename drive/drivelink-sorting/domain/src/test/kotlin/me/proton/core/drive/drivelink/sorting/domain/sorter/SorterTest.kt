/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.drivelink.sorting.domain.sorter

import junit.framework.TestCase.assertEquals
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SorterTest {

    private val drivelinks = listOf(
        file(name = "nAME", type = "image/*", lastModified = 123, size = 123), // 0
        file(name = "1NAME", type = "image/*", lastModified = 78, size = 78), // 1
        file(name = "1NAME", type = "audio/*", lastModified = 23, size = 23), // 2
        file(name = "NAME1", type = "audio/*", lastModified = 78, size = 78), // 3
        file(name = "nAMEA", type = "pdf/*", lastModified = 1, size = 1), // 4
        file(name = "ANAME", type = "image/*", lastModified = 1230, size = 1230), // 5
        file(name = "zZZ", type = "application/*", lastModified = Long.MAX_VALUE, size = Long.MAX_VALUE), // 6
        folder(name = "NAME", lastModified = 123, size = 123), // 7
        folder(name = "1NAME", lastModified = 78, size = 78), // 8
        folder(name = "nAME1", lastModified = 23, size = 23), // 9
        folder(name = "NAMEA", lastModified = 1, size = 1), //  10
        folder(name = "aNAME", lastModified = 1230, size = 1230), // 11
        folder(name = "aNAME", lastModified = 78, size = 78), // 12
        folder(name = "ZZZ", lastModified = Long.MAX_VALUE, size = Long.MAX_VALUE), // 13
        cryptedFile(name = "CRYPTO2", type = "image/*", lastModified = 456, size = 456), // 14
        cryptedFile(name = "CRYPTO1", type = "audio/*", lastModified = 123, size = 123), // 15
        cryptedFolder(name = "CRYPTO2", lastModified = 456, size = 456), // 16
        cryptedFolder(name = "CRYPTO1", lastModified = 123, size = 123), // 17
    )
    private val files = drivelinks.subList(0, 7) + drivelinks.subList(14, 16)
    private val folders = drivelinks.subList(7, 14) + drivelinks.subList(16, 18)

    // region Sort by Name
    // region ASCENDING
    @Test
    fun `sort files by name ascending`() {
        // region Given
        val sortingBy = By.NAME
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(files, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[15], // File "CRYPTO1"
                drivelinks[14], // File "CRYPTO2"
                drivelinks[1], // File "1NAME"
                drivelinks[2], // File "1NAME"
                drivelinks[5], // File "ANAME"
                drivelinks[0], // File "NAME"
                drivelinks[3], // File "NAME1"
                drivelinks[4], // File "NAMEA"
                drivelinks[6], // File "ZZZ"
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort folders by name ascending`() {
        // region Given
        val sortingBy = By.NAME
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(folders, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[8], // Folder "1NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[7], // Folder "NAME"
                drivelinks[9], // Folder "NAME1"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[13], // Folder "ZZZ"
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort files and folder by name ascending`() {
        // region Given
        val sortingBy = By.NAME
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(drivelinks, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[8], // Folder "1NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[7], // Folder "NAME"
                drivelinks[9], // Folder "NAME1"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[15], // File "CRYPTO1"
                drivelinks[14], // File "CRYPTO2"
                drivelinks[1], // File "1NAME"
                drivelinks[2], // File "1NAME"
                drivelinks[5], // File "ANAME"
                drivelinks[0], // File "NAME"
                drivelinks[3], // File "NAME1"
                drivelinks[4], // File "NAMEA"
                drivelinks[6], // File "ZZZ"
            ),
            sorted,
        )
        // endregion
    }
    // endregion

    // region  DESCENDING
    @Test
    fun `sort files by name descending`() {
        // region Given
        val sortingBy = By.NAME
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(files, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[14], // File "CRYPTO2"
                drivelinks[15], // File "CRYPTO1"
                drivelinks[6], // File "ZZZ"
                drivelinks[4], // File "NAMEA"
                drivelinks[3], // File "NAME1"
                drivelinks[0], // File "NAME"
                drivelinks[5], // File "ANAME"
                drivelinks[1], // File "1NAME"
                drivelinks[2], // File "1NAME"
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort folders by name descending`() {
        // region Given
        val sortingBy = By.NAME
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(folders, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[9], // Folder "NAME1"
                drivelinks[7], // Folder "NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[8], // Folder "1NAME"
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort files and folder by name descending`() {
        // region Given
        val sortingBy = By.NAME
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(drivelinks, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[9], // Folder "NAME1"
                drivelinks[7], // Folder "NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[8], // Folder "1NAME"
                drivelinks[14], // File "CRYPTO2"
                drivelinks[15], // File "CRYPTO1"
                drivelinks[6], // File "ZZZ"
                drivelinks[4], // File "NAMEA"
                drivelinks[3], // File "NAME1"
                drivelinks[0], // File "NAME"
                drivelinks[5], // File "ANAME"
                drivelinks[1], // File "1NAME"
                drivelinks[2], // File "1NAME"
            ),
            sorted,
        )
        // endregion
    }
    // endregion
    // endregion

    // region Sort by last modified
    // region ASCENDING
    @Test
    fun `sort files by last modified ascending`() {
        // region Given
        val sortingBy = By.LAST_MODIFIED
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(files, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[15], // File "CRYPTO2" lastModified 123
                drivelinks[14], // File "CRYPTO1" lastModified 456
                drivelinks[4], // File lastModified 1
                drivelinks[2], // File lastModified 23
                drivelinks[1], // File lastModified 78 "1NAME"
                drivelinks[3], // File lastModified 78 "NAME1"
                drivelinks[0], // File lastModified 123
                drivelinks[5], // File lastModified 1230
                drivelinks[6], // File lastModified Long.MAX_VALUE
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort folders by last modified ascending`() {
        // region Given
        val sortingBy = By.LAST_MODIFIED
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(folders, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[17], // Folder "CRYPTO2" lastModified 123
                drivelinks[16], // Folder "CRYPTO1" lastModified 456
                drivelinks[10], // Folder lastModified 1
                drivelinks[9], // Folder lastModified 23
                drivelinks[8], // Folder lastModified 78 "1NAME"
                drivelinks[12], // Folder lastModified 78 "ANAME"
                drivelinks[7], // Folder lastModified 123
                drivelinks[11], // Folder lastModified 1230
                drivelinks[13], // Folder lastModified  Long.MAX_VALUE
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort files and folders by last modified ascending`() {
        // region Given
        val sortingBy = By.LAST_MODIFIED
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(drivelinks, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[17], // Folder "CRYPTO2" lastModified 123
                drivelinks[16], // Folder "CRYPTO1" lastModified 456
                drivelinks[10], // Folder lastModified 1
                drivelinks[9], // Folder lastModified 23
                drivelinks[8], // Folder lastModified 78 "1NAME"
                drivelinks[12], // Folder lastModified 78 "ANAME"
                drivelinks[7], // Folder lastModified 123
                drivelinks[11], // Folder lastModified 1230
                drivelinks[13], // Folder lastModified  Long.MAX_VALUE
                drivelinks[15], // File "CRYPTO2" lastModified 123
                drivelinks[14], // File "CRYPTO1" lastModified 456
                drivelinks[4], // File lastModified 1
                drivelinks[2], // File lastModified 23
                drivelinks[1], // File lastModified 78 "1NAME"
                drivelinks[3], // File lastModified 78 "NAME1"
                drivelinks[0], // File lastModified 123
                drivelinks[5], // File lastModified 1230
                drivelinks[6], // File lastModified Long.MAX_VALUE
            ),
            sorted,
        )
        // endregion
    }
    // endregion

    // region DESCENDING
    @Test
    fun `sort files by last modified descending`() {
        // region Given
        val sortingBy = By.LAST_MODIFIED
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(files, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[14], // File "CRYPTO1" lastModified 456
                drivelinks[15], // File "CRYPTO2" lastModified 123
                drivelinks[6], // File lastModified Long.MAX_VALUE
                drivelinks[5], // File lastModified 1230
                drivelinks[0], // File lastModified 123
                drivelinks[3], // File lastModified 78 "NAME1"
                drivelinks[1], // File lastModified 78 "1NAME"
                drivelinks[2], // File lastModified 23
                drivelinks[4], // File lastModified 1
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort folders by last modified descending`() {
        // region Given
        val sortingBy = By.LAST_MODIFIED
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(folders, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[16], // Folder "CRYPTO1" lastModified 456
                drivelinks[17], // Folder "CRYPTO2" lastModified 123
                drivelinks[13], // Folder lastModified  Long.MAX_VALUE
                drivelinks[11], // Folder lastModified 1230
                drivelinks[7], // Folder lastModified 123
                drivelinks[12], // Folder lastModified 78 "ANAME"
                drivelinks[8], // Folder lastModified 78 "1NAME"
                drivelinks[9], // Folder lastModified 23
                drivelinks[10], // Folder lastModified 1
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort files and folders by last modified descending`() {
        // region Given
        val sortingBy = By.LAST_MODIFIED
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(drivelinks, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[16], // Folder "CRYPTO1" lastModified 456
                drivelinks[17], // Folder "CRYPTO2" lastModified 123
                drivelinks[13], // Folder lastModified  Long.MAX_VALUE
                drivelinks[11], // Folder lastModified 1230
                drivelinks[7], // Folder lastModified 123
                drivelinks[12], // Folder lastModified 78 "ANAME"
                drivelinks[8], // Folder lastModified 78 "1NAME"
                drivelinks[9], // Folder lastModified 23
                drivelinks[10], // Folder lastModified 1
                drivelinks[14], // File "CRYPTO1" lastModified 456
                drivelinks[15], // File "CRYPTO2" lastModified 123
                drivelinks[6], // File lastModified Long.MAX_VALUE
                drivelinks[5], // File lastModified 1230
                drivelinks[0], // File lastModified 123
                drivelinks[3], // File lastModified 78 "NAME1"
                drivelinks[1], // File lastModified 78 "1NAME"
                drivelinks[2], // File lastModified 23
                drivelinks[4], // File lastModified 1
            ),
            sorted,
        )
        // endregion
    }
    // endregion
    // endregion

    // region Sort by size
    // region ASCENDING
    @Test
    fun `sort files by size ascending`() {
        // region Given
        val sortingBy = By.SIZE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(files, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[15], // File "CRYPTO2" size 123
                drivelinks[14], // File "CRYPTO1" size 456
                drivelinks[4], // File size 1
                drivelinks[2], // File size 23
                drivelinks[1], // File size 78 "1NAME"
                drivelinks[3], // File size 78 "NAME1"
                drivelinks[0], // File size 123
                drivelinks[5], // File size 1230
                drivelinks[6], // File size Long.MAX_VALUE
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort folders by size ascending`() {
        // region Given
        val sortingBy = By.SIZE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(folders, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[8], // Folder "1NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[7], // Folder "NAME"
                drivelinks[9], // Folder "NAME1"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[13], // Folder "ZZZ"
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort files and folders by size ascending`() {
        // region Given
        val sortingBy = By.SIZE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(drivelinks, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[8], // Folder "1NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[7], // Folder "NAME"
                drivelinks[9], // Folder "NAME1"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[15], // File "CRYPTO2" size 123
                drivelinks[14], // File "CRYPTO1" size 456
                drivelinks[4], // File size 1
                drivelinks[2], // File size 23
                drivelinks[1], // File size 78 "1NAME"
                drivelinks[3], // File size 78 "NAME1"
                drivelinks[0], // File size 123
                drivelinks[5], // File size 1230
                drivelinks[6], // File size Long.MAX_VALUE
            ),
            sorted,
        )
        // endregion
    }
    // endregion

    // region DESCENDING
    @Test
    fun `sort files by size descending`() {
        // region Given
        val sortingBy = By.SIZE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(files, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[14], // File "CRYPTO1" size 456
                drivelinks[15], // File "CRYPTO2" size 123
                drivelinks[6], // File size Long.MAX_VALUE
                drivelinks[5], // File size 1230
                drivelinks[0], // File size 123
                drivelinks[3], // File size 78 "NAME1"
                drivelinks[1], // File size 78 "1NAME"
                drivelinks[2], // File size 23
                drivelinks[4], // File size 1
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort folders by size descending`() {
        // region Given
        val sortingBy = By.SIZE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(folders, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[9], // Folder "NAME1"
                drivelinks[7], // Folder "NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[8], // Folder "1NAME"
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort files and folders by size descending`() {
        // region Given
        val sortingBy = By.SIZE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(drivelinks, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[9], // Folder "NAME1"
                drivelinks[7], // Folder "NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[8], // Folder "1NAME"
                drivelinks[14], // File "CRYPTO1" size 456
                drivelinks[15], // File "CRYPTO2" size 123
                drivelinks[6], // File size Long.MAX_VALUE
                drivelinks[5], // File size 1230
                drivelinks[0], // File size 123
                drivelinks[3], // File size 78 "NAME1"
                drivelinks[1], // File size 78 "1NAME"
                drivelinks[2], // File size 23
                drivelinks[4], // File size 1
            ),
            sorted,
        )
        // endregion
    }
    // endregion
    // endregion

    // region Sort by type
    // region Ascending
    @Test
    fun `sort files by type ascending`() {
        // region Given
        val sortingBy = By.TYPE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(files, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[15], // File "CRYPTO2" audio
                drivelinks[14], // File "CRYPTO1" image
                drivelinks[6], // File type application
                drivelinks[2], // File "1NAME" type audio
                drivelinks[3], // File "NAME1" type audio
                drivelinks[1], // File "1NAME" type image
                drivelinks[5], // File "ANAME" type image
                drivelinks[0], // File "NAME" type image
                drivelinks[4], // File type pdf
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort folders by type ascending`() {
        // region Given
        val sortingBy = By.TYPE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(folders, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[8], // Folder "1NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[7], // Folder "NAME"
                drivelinks[9], // Folder "NAME1"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[13], // Folder "ZZZ"
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort files and folders by type ascending`() {
        // region Given
        val sortingBy = By.TYPE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(drivelinks, Direction.ASCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[8], // Folder "1NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[7], // Folder "NAME"
                drivelinks[9], // Folder "NAME1"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[15], // File "CRYPTO2" audio
                drivelinks[14], // File "CRYPTO1" image
                drivelinks[6], // File type application
                drivelinks[2], // File "1NAME" type audio
                drivelinks[3], // File "NAME1" type audio
                drivelinks[1], // File "1NAME" type image
                drivelinks[5], // File "ANAME" type image
                drivelinks[0], // File "NAME" type image
                drivelinks[4], // File type pdf
            ),
            sorted,
        )
        // endregion
    }
    // endregion

    // region DESCENDING
    @Test
    fun `sort files by type descending`() {
        // region Given
        val sortingBy = By.TYPE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(files, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[14], // File "CRYPTO1" image
                drivelinks[15], // File "CRYPTO2" audio
                drivelinks[4], // File type pdf
                drivelinks[0], // File "NAME" type image
                drivelinks[5], // File "ANAME" type image
                drivelinks[1], // File "1NAME" type image
                drivelinks[3], // File "NAME1" type audio
                drivelinks[2], // File "1NAME" type audio
                drivelinks[6], // File type application
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort folders by type descending`() {
        // region Given
        val sortingBy = By.TYPE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(folders, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[9], // Folder "NAME1"
                drivelinks[7], // Folder "NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[8], // Folder "1NAME"
            ),
            sorted,
        )
        // endregion
    }

    @Test
    fun `sort files and folders by type descending`() {
        // region Given
        val sortingBy = By.TYPE
        // endregion
        // region When
        val sorted = Sorter.Factory[sortingBy].sort(drivelinks, Direction.DESCENDING)
        // endregion
        // region Then
        assertEquals(
            listOf(
                drivelinks[16], // Folder "CRYPTO2"
                drivelinks[17], // Folder "CRYPTO1"
                drivelinks[13], // Folder "ZZZ"
                drivelinks[10], // Folder "NAMEA"
                drivelinks[9], // Folder "NAME1"
                drivelinks[7], // Folder "NAME"
                drivelinks[11], // Folder "ANAME"
                drivelinks[12], // Folder "ANAME"
                drivelinks[8], // Folder "1NAME"
                drivelinks[14], // File "CRYPTO1" image
                drivelinks[15], // File "CRYPTO2" audio
                drivelinks[4], // File type pdf
                drivelinks[0], // File "NAME" type image
                drivelinks[5], // File "ANAME" type image
                drivelinks[1], // File "1NAME" type image
                drivelinks[3], // File "NAME1" type audio
                drivelinks[2], // File "1NAME" type audio
                drivelinks[6], // File type application
            ),
            sorted,
        )
        // endregion
    }
    // endregion
    // endregion
}
