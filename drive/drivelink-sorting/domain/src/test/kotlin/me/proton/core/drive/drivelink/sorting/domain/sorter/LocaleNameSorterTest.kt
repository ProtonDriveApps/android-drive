/*
 * Copyright (c) 2023 Proton AG.
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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Test.None
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
class LocaleNameSorterTest {

    private val File1 = file("File1")
    private val file2 = file("file2")
    private val file22 = file("file22")
    private val file3 = file("file3")
    private val file4 = file("file4")
    private val file45 = file("file45")
    private val file5 = file("file5")

    private val files = listOf(
        file2,
        File1,
        file4,
        file22,
        file3,
        file45,
        file5,
    )

    @Test
    @Config(sdk = [24])
    fun `sort files by locale names ascending`() {

        val sorted = Sorter.Factory[By.NAME].sort(files, Direction.ASCENDING)

        assertEquals(
            listOf(
                File1,
                file2,
                file3,
                file4,
                file5,
                file22,
                file45,
            ).map { it.name },
            sorted.map { it.name },
        )
    }

    @Test
    @Config(sdk = [24])
    fun `sort files by locale names descending`() {

        val sorted = Sorter.Factory[By.NAME].sort(files, Direction.DESCENDING)

        assertEquals(
            listOf(
                file45,
                file22,
                file5,
                file4,
                file3,
                file2,
                File1,
            ).map { it.name },
            sorted.map { it.name },
        )
    }

    @Test
    @Config(sdk = [23])
    fun `sort files by names ascending`() {

        val sorted = Sorter.Factory[By.NAME].sort(files, Direction.ASCENDING)

        assertEquals(
            listOf(
                File1,
                file2,
                file22,
                file3,
                file4,
                file45,
                file5,
            ).map { it.name },
            sorted.map { it.name },
        )
    }

    @Test
    @Config(sdk = [23])
    fun `sort files by names descending`() {

        val sorted = Sorter.Factory[By.NAME].sort(files, Direction.DESCENDING)

        assertEquals(
            listOf(
                file5,
                file45,
                file4,
                file3,
                file22,
                file2,
                File1,
            ).map { it.name },
            sorted.map { it.name },
        )
    }

    @Test(expected = None::class)
    @Config(sdk = [23, 24])
    fun `async sort names ascending`() = runTest {
        val driveLinks = names.map { name ->
            file(name = name)
        }

        val deferred = (1..50).map {
            async {
                Sorter.Factory[By.NAME].sort(driveLinks, Direction.ASCENDING)
            }
        }
        val cancelled = async {
            (1..5).forEach { _ ->
                val index = Random.nextInt(50)
                delay(index.milliseconds)
                deferred[index].cancel()
            }
        }

        (deferred + cancelled).awaitAll()
    }

    companion object {
        private val names get() = listOf(
            "0_216.jpg",
            "0_216.jpeg",
            "0_216.gif",
            "0_216.bmp",
            "0_215.png",
            "0_215 (1).gif",
            "0_215.jpg",
            "0_215.jpeg",
            "0_215.bmp",
            "0_214.jpg",
            "0_214.png",
            "0_214.jpeg",
            "0_214.bmp",
            "0_214.gif",
            "0_213.png",
            "0_213.jpg",
            "0_213.jpeg",
            "0_213.gif",
            "0_213.bmp",
            "0_212.png",
            "0_212.jpg",
            "0_212.jpeg",
            "0_212.bmp",
            "0_212.gif",
            "0_211.png",
            "0_211.jpeg",
            "0_211.jpg",
            "0_211.gif",
            "0_210.png",
            "0_211.bmp",
            "0_210.jpg",
            "0_210.jpeg",
            "0_210.gif",
            "0_210.bmp",
            "0_21.jpg",
            "0_21.png",
            "0_21.jpeg",
            "0_21.gif",
            "0_209.png",
            "0_21.bmp",
            "0_209.jpeg",
            "0_209.jpg",
            "0_209.gif",
            "0_209.bmp",
            "0_208.jpg",
            "0_208.png",
            "0_208.jpeg",
            "0_208.gif",
            "0_208.bmp",
            "0_207.png",
            "0_207.jpeg",
            "0_207.jpg",
            "0_207.bmp",
            "0_207.gif",
            "0_206.png",
            "0_206.jpg",
            "0_206.gif",
            "0_206.jpeg",
            "0_206.bmp",
            "0_205.png",
            "0_205.jpg",
            "0_205.jpeg",
            "0_205.gif",
            "0_205.bmp",
            "0_204.png",
            "0_204.jpg",
            "0_204.jpeg",
            "0_204.gif",
            "0_204.bmp",
            "0_203.png",
            "0_203.jpg",
            "0_203.jpeg",
            "0_203.gif",
            "0_203.bmp",
            "0_202.png",
            "0_202.jpg",
            "0_202.jpeg",
            "0_202.gif",
            "0_202.bmp",
            "0_201.png",
            "0_201.jpg",
            "0_201.jpeg",
            "0_201.bmp",
            "0_201.gif",
            "0_200.png",
            "0_200.jpg",
            "0_200.jpeg",
            "0_200.gif",
            "0_20.png",
            "0_200.bmp",
            "0_20.jpg",
            "0_20.jpeg",
            "0_20.gif",
            "0_20.bmp",
            "0_2.png",
            "0_2.jpg",
            "0_2.gif",
            "0_2.jpeg",
            "0_2.bmp",
            "0_199.png",
            "0_0.bmp",
            "0_0.gif",
            "0_0.jpeg",
            "0_0.jpg",
            "0_0.png",
            "0_1.bmp",
            "0_1.gif",
            "0_1.jpeg",
            "0_1.jpg",
            "0_1.png",
            "0_10.bmp",
            "0_10.gif",
            "0_10.jpeg",
            "0_10.jpg",
            "0_10.png",
            "0_100.bmp",
            "0_100.gif",
            "0_100.jpeg",
            "0_100.jpg",
            "0_100.png",
            "0_101.bmp",
            "0_101.gif",
            "0_101.jpeg",
            "0_101.jpg",
            "0_101.png",
            "0_102.bmp",
            "0_102.gif",
            "0_102.jpeg",
            "0_102.jpg",
            "0_102.png",
            "0_103.bmp",
            "0_103.gif",
            "0_103.jpeg",
            "0_103.jpg",
            "0_103.png",
            "0_104.bmp",
            "0_104.gif",
            "0_104.jpeg",
            "0_104.jpg",
            "0_104.png",
            "0_105.bmp",
            "0_105.gif",
            "0_105.jpeg",
            "0_105.jpg",
            "0_105.png",
            "0_106.bmp",
            "0_106.gif",
            "0_106.jpeg",
            "0_106.jpg",
            "0_106.png",
        )
    }
}
