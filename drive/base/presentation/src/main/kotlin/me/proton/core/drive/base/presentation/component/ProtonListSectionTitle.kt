/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.base.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak

@Composable
fun ProtonListSectionTitle(
    @StringRes title: Int,
    modifier: Modifier = Modifier
) = ProtonListSectionTitle(stringResource(title), modifier)

@Composable
fun ProtonListSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = title
                heading()
            }
            .padding(top = ProtonDimens.DefaultSpacing)
            .fillMaxWidth(),
    ) {
        HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
        Text(
            text = title,
            modifier = Modifier.padding(
                vertical = ProtonDimens.DefaultSpacing,
                horizontal = SectionHeaderHorizontalPadding,
            ),
            style = ProtonTheme.typography.defaultSmallWeak,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val SectionHeaderHorizontalPadding = 20.dp
