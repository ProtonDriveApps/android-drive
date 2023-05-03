/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.files.presentation.component.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.compose.component.bottomsheet.BottomSheetEntry
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.i18n.R as I18N

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MultipleOptions(
    count: Int,
    entries: List<OptionEntry<Unit>>,
    modifier: Modifier = Modifier,
) {
    BottomSheetContent(
        modifier = modifier,
        header = {
            OptionsHeader(
                title = pluralStringResource(
                    id = I18N.plurals.common_selected,
                    count = count,
                    count,
                )
            )
        },
        content = {
            entries.map { entry ->
                BottomSheetEntry(
                    icon = entry.icon,
                    title = stringResource(id = entry.label),
                    onClick = { entry.onClick(Unit) }
                )
            }
        }
    )
}
