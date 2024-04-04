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

package me.proton.core.drive.user.presentation.storage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrong
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.toPercentString
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.currentLocale
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun StorageIndicator(
    label: String,
    usedBytes: Bytes,
    availableBytes: Bytes,
    modifier: Modifier = Modifier,
) {
    val percentage = Percentage((usedBytes / availableBytes).toFloat())
    Column(
        modifier
            .fillMaxWidth()
            .wrapContentHeight()) {
        Row {
            Icon(
                painter = painterResource(CorePresentation.drawable.ic_proton_cloud),
                tint = ProtonTheme.colors.iconWeak,
                contentDescription = null,
            )
            Text(
                text = label,
                style = ProtonTheme.typography.defaultSmallStrong,
                modifier = Modifier
                    .padding(start = SmallSpacing)
                    .weight(1f),
            )
            Text(
                style = ProtonTheme.typography.defaultSmallStrong,
                text = percentage.toPercentString(LocalContext.current.currentLocale)
            )
        }

        LinearProgressIndicator(
            progress = percentage.value,
            modifier = Modifier
                .height(LinearProgressIndicatorSize)
                .fillMaxWidth()
                .padding(vertical = DefaultSpacing)
                .clip(ProtonTheme.shapes.small),
            backgroundColor = ProtonTheme.colors.separatorNorm,
        )

        Text(
            style = ProtonTheme.typography.defaultSmallStrong,
            text = stringResource(
                I18N.string.storage_details_format,
                usedBytes.asHumanReadableString(LocalContext.current),
                availableBytes.asHumanReadableString(LocalContext.current),
            ),
        )
    }
}

@Preview
@Composable
fun PreviewStorage() {
    ProtonTheme {
        StorageIndicator(
            label = stringResource(I18N.string.storage_total_usage),
            usedBytes = Bytes(242_221_056L), // 231MiB
            availableBytes = Bytes(2_147_483_648L) // 2GiB
        )
    }
}

private val LinearProgressIndicatorSize = 42.dp
