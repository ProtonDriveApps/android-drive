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

package me.proton.core.drive.base.presentation.extension

import android.content.Context
//import android.text.format.Formatter
import me.proton.core.drive.base.domain.entity.Bytes
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import kotlin.math.abs
import kotlin.math.sign

/*
fun Bytes.asHumanReadableString(context: Context): String =
    Formatter.formatFileSize(context, value).replace("\u200e", "")
        .replace("\u200f", "")
*/
@Suppress("UNUSED_PARAMETER")
fun Bytes.asHumanReadableString(context: Context, units: SizeUnits = SizeUnits.BASE2_LEGACY): String {
    val absB = if (value == Long.MIN_VALUE) Long.MAX_VALUE else abs(value)
    if (absB < 1024) return "$value B"
    var value = absB
    val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
    var i = 40
    while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
        value = value shr 10
        ci.next()
        i -= 10
    }
    value *= sign(value.toDouble()).toLong()
    val suffix = when (units) {
        SizeUnits.BASE2_LEGACY -> "B"
        SizeUnits.BASE2_IEC -> "iB"
        SizeUnits.BASE10_SI -> throw IllegalArgumentException("Base 10 SI units are not supported")
    }
    return String.format("%.2f %c$suffix", value / 1024.0, ci.current())
}

enum class SizeUnits {
    BASE2_LEGACY,
    BASE2_IEC,
    BASE10_SI,
}
