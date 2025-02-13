/*
 * Copyright (c) 2025 Proton AG.
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultCornerRadius
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.extension.firstCodePointAsStringOrNull
import me.proton.core.drive.base.presentation.extension.iconResId

@Composable
fun BoxScope.LetterBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset(x = ExtraSmallSpacing)
            .align(Alignment.BottomEnd)
            .size(MediumSpacing)
            .clip(CircleShape)
            .background(ProtonTheme.colors.shade40),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.firstCodePointAsStringOrNull?.uppercase() ?: "?",
            style = ProtonTheme.typography.captionNorm,
        )
    }
}

@Preview
@Composable
fun LetterBadge() {
    ProtonTheme {
        Box {
            Image(
                modifier = Modifier
                    .size(DefaultButtonMinHeight)
                    .clip(RoundedCornerShape(DefaultCornerRadius)),
                painter = painterResource(FileTypeCategory.Unknown.iconResId),
                contentDescription = null,
            )
            LetterBadge("Name")
        }
    }
}
