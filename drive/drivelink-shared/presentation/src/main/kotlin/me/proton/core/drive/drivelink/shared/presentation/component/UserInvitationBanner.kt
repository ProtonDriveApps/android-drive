/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.drivelink.shared.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.drive.base.presentation.component.BoxWithNotificationDot
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun UserInvitationBanner(
    description: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(id = I18N.string.shared_by_me_invitation_banner_title),
    onClick: () -> Unit = {},
) {
    Column(modifier) {
        Text(
            text = title,
            style = ProtonTheme.typography.defaultSmallWeak,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(DefaultSpacing)
        )
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = DefaultSpacing, vertical = SmallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoxWithNotificationDot(
                notificationDotVisible = true,
                horizontalOffset = ExtraSmallSpacing,
                verticalOffset = -ExtraSmallSpacing,
            ) {
                Box(
                    Modifier
                        .background(
                            color = ProtonTheme.colors.backgroundSecondary,
                            shape = RoundedCornerShape(ProtonDimens.DefaultCornerRadius)
                        )
                        .padding(SmallSpacing)
                ) {
                    Icon(
                        painter = painterResource(id = CorePresentation.drawable.ic_proton_users_filled),
                        contentDescription = null,
                        tint = ProtonTheme.colors.iconNorm,
                    )
                }
            }
            Text(
                text = description,
                style = ProtonTheme.typography.defaultNorm,
                modifier = Modifier
                    .padding(start = DefaultSpacing)
                    .weight(1F)
            )
            Icon(
                painter = painterResource(id = CorePresentation.drawable.ic_proton_chevron_right),
                contentDescription = null,
                tint = ProtonTheme.colors.iconNorm,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun UserInvitationBannerLightPreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            UserInvitationBanner(
                title = stringResource(I18N.string.shared_by_me_invitation_banner_title),
                description = pluralStringResource(
                    I18N.plurals.shared_by_me_invitation_banner_description,
                    1,
                    1
                )
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun UserInvitationBannerDarkPreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            UserInvitationBanner(
                title = stringResource(I18N.string.shared_by_me_invitation_banner_title),
                description = pluralStringResource(
                    I18N.plurals.shared_by_me_invitation_banner_description,
                    1,
                    1
                )
            )
        }
    }
}
