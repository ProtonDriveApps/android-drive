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

package me.proton.core.drive.file.base.domain.extension

import me.proton.core.drive.file.base.domain.entity.XAttr
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XAttrKtTest {

    @Language("json")
    val json =
        """{"Common": {"ModificationTime": "ModificationTime", "unknown_key": null}}"""

    @Test
    fun deserialization() {
        assertEquals(
            Result.success(
                XAttr(
                    common = XAttr.Common(
                        modificationTime = "ModificationTime"
                    )
                )
            ), json.toXAttr()
        )
    }

    @Test
    fun serialization() {
        val xAttr = XAttr(common = XAttr.Common(modificationTime = "modificationTime"))
        assertEquals(xAttr, xAttr.asJson().toXAttr().getOrNull())
    }

    @Test
    fun `fails with bad input`() {
        assertTrue("bad input".toXAttr().isFailure)
    }
}