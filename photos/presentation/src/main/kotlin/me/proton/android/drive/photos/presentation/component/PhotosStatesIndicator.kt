/*
 * Copyright (c) 2023-2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.presentation.component

import androidx.compose.animation.core.EaseInOutElastic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.android.drive.photos.presentation.R
import me.proton.android.drive.photos.presentation.viewstate.PhotosStatusViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.presentation.R as CorePresentation

@Composable
fun PhotosStatesIndicator(
    viewState: PhotosStatusViewState,
    modifier: Modifier = Modifier,
) {
    when (viewState) {
        is PhotosStatusViewState.Disabled -> if (viewState.hasDefaultFolder == false) {
            PhotosMissingFolderIndicator(modifier)
        } else {
            PhotosDisableIndicator(modifier)
        }

        is PhotosStatusViewState.Complete -> PhotosCompleteIndicator(modifier)
        is PhotosStatusViewState.Uncompleted -> PhotosUncompletedIndicator(modifier)
        is PhotosStatusViewState.Failed -> if (viewState.errors
                .any { it == BackupError.WifiConnectivity() || it == BackupError.Connectivity() }
        ) {
            PhotosFailedConnectivityIndicator(modifier)
        } else {
            PhotosFailedIndicator(modifier)
        }

        is PhotosStatusViewState.Preparing -> PhotosPreparingIndicator(modifier)
        is PhotosStatusViewState.InProgress -> PhotosProgressIndicator(modifier)
    }
}


@Composable
private fun PhotosDisableIndicator(modifier: Modifier = Modifier) {
    PhotosStatesIndicator(
        modifier = modifier,
        color = ProtonTheme.colors.iconWeak,
        icon = R.drawable.ic_proton_cloud_slash,
    )
}

@Composable
private fun PhotosMissingFolderIndicator(modifier: Modifier = Modifier) {
    PhotosStatesIndicator(
        modifier = modifier,
        color = ProtonTheme.colors.notificationWarning,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
    )
}

@Composable
private fun PhotosCompleteIndicator(modifier: Modifier = Modifier) {
    PhotosStatesIndicator(
        modifier = modifier,
        color = ProtonTheme.colors.notificationSuccess,
        icon = CorePresentation.drawable.ic_proton_checkmark,
    )
}

@Composable
private fun PhotosUncompletedIndicator(modifier: Modifier = Modifier) {
    PhotosStatesIndicator(
        modifier = modifier,
        color = ProtonTheme.colors.notificationWarning,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
    )
}

@Composable
private fun PhotosProgressIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 180F,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        )
    )

    val color = ProtonTheme.colors.brandNorm
    PhotosStateIndicatorContainer(
        modifier = modifier,
        color = color,
    ) {
        PhotosStateIndicatorIcon(
            modifier = Modifier.rotate(angle),
            color = color,
            icon = CorePresentation.drawable.ic_proton_arrows_rotate,
        )
    }
}

@Composable
private fun PhotosPreparingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hourglass-transition")
    val angle by infiniteTransition.animateFloat(
        label = "hourglass-animation",
        initialValue = 0F,
        targetValue = 180F,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutElastic)
        )
    )

    val color = ProtonTheme.colors.brandNorm
    PhotosStateIndicatorContainer(
        modifier = modifier,
        color = color,
    ) {
        PhotosStateIndicatorIcon(
            modifier = Modifier.rotate(angle),
            color = color,
            icon = CorePresentation.drawable.ic_proton_hourglass,
        )
    }
}

@Composable
private fun PhotosFailedIndicator(modifier: Modifier = Modifier) {
    PhotosStatesIndicator(
        modifier = modifier,
        color = ProtonTheme.colors.notificationError,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
    )
}

@Composable
private fun PhotosFailedConnectivityIndicator(modifier: Modifier = Modifier) {
    PhotosStatesIndicator(
        modifier = modifier,
        color = ProtonTheme.colors.notificationError,
        icon = R.drawable.ic_no_wifi,
    )
}

@Composable
private fun PhotosStateIndicatorContainer(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1F), CircleShape)
            .padding(ProtonDimens.ExtraSmallSpacing),
        content = content
    )
}

@Composable
private fun PhotosStateIndicatorIcon(
    color: Color,
    icon: Int,
    modifier: Modifier = Modifier,
) {
    Icon(
        modifier = modifier.size(ProtonDimens.DefaultIconSize),
        painter = painterResource(icon),
        contentDescription = null,
        tint = color,
    )
}

@Composable
private fun PhotosStatesIndicator(color: Color, icon: Int, modifier: Modifier = Modifier) {
    PhotosStateIndicatorContainer(color, modifier) {
        PhotosStateIndicatorIcon(
            color = color,
            icon = icon,
        )
    }
}

@Preview
@Composable
fun PhotosDisableIconPreview() {
    ProtonTheme {
        Surface {
            PhotosDisableIndicator()
        }
    }
}

@Preview
@Composable
fun PhotosMissingFolderIconPreview() {
    ProtonTheme {
        Surface {
            PhotosMissingFolderIndicator()
        }
    }
}


@Preview
@Composable
fun PhotosCompleteIconPreview() {
    ProtonTheme {
        Surface {
            PhotosCompleteIndicator()
        }
    }
}

@Preview
@Composable
fun PhotosUncompletedIconPreview() {
    ProtonTheme {
        Surface {
            PhotosUncompletedIndicator()
        }
    }
}

@Preview
@Composable
private fun PhotosPreparingIconPreview() {
    ProtonTheme {
        PhotosPreparingIndicator()
    }
}

@Preview
@Composable
fun PhotosProgressIconPreview() {
    ProtonTheme {
        Surface {
            PhotosProgressIndicator()
        }
    }
}

@Preview
@Composable
fun PhotosFailedIconPreview() {
    ProtonTheme {
        Surface {
            PhotosFailedIndicator()
        }
    }
}

@Preview
@Composable
private fun PhotosFailedConnectivityIndicatorPreview() {
    ProtonTheme {
        Surface {
            PhotosFailedConnectivityIndicator()
        }
    }
}
