/*
 * Copyright (c) 2023-2024 Proton AG.
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

import android.os.Build
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.sorting.domain.entity.Direction
import java.text.Collator
import android.icu.text.Collator as IcuCollator
import android.icu.text.RuleBasedCollator as IcuRuleBasedCollator

data class LocaleNameSorter internal constructor(
    private val comparator: Comparator<Any>
) : Sorter() {

    override fun sort(driveLinks: List<DriveLink>, direction: Direction): List<DriveLink> =
        driveLinks.sortedWith(
            compareBy<DriveLink> { driveLink -> if (driveLink is DriveLink.Folder) 0 else 1 }
                .thenBy { driveLink -> if (driveLink.isNameEncrypted) 0 else 1 }
                .thenComparator { a, b ->
                    when (direction) {
                        Direction.ASCENDING -> comparator.compare(a.name, b.name)
                        Direction.DESCENDING -> comparator.compare(b.name, a.name)
                    }
                }
        )

    companion object {
        operator fun invoke(): LocaleNameSorter =
            LocaleNameSorter(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                (IcuCollator.getInstance() as IcuRuleBasedCollator).apply {
                    numericCollation = true
                }
            } else {
                Collator.getInstance()
            })
    }
}
