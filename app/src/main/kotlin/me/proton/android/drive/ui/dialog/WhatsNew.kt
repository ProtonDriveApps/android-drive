/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.viewmodel.WhatsNewViewModel
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineSmallNorm
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.base.presentation.viewevent.WhatsNewViewEvent
import me.proton.core.drive.base.presentation.viewstate.WhatsNewViewState
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
fun WhatsNew(
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<WhatsNewViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = null)
    val viewEvent = remember(dismiss, viewModel) { viewModel.viewEvent(dismiss) }
    val whatsNewViewState = viewState ?: return

    Box(modifier = modifier
        .conditional(isPortrait) {
            navigationBarsPadding()
        }
        .testTag(WhatsNewTestTag.screen)
    ) {
        WhatsNew(
            viewState = whatsNewViewState,
            viewEvent = viewEvent,
        )
    }

}

@Composable
fun WhatsNew(
    viewState: WhatsNewViewState,
    viewEvent: WhatsNewViewEvent,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewEvent) {
        viewEvent.whatsNewShown()
    }
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1F, fill = false),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ProtonDimens.DefaultSpacing),
                text = stringResource(I18N.string.whats_new_title),
                style = ProtonTheme.typography.headline,
            )
            Box(modifier = Modifier.padding(ProtonDimens.DefaultSpacing)) {
                val shape = RoundedCornerShape(ProtonDimens.ExtraLargeCornerRadius)
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ImageRatio)
                        .background(color = ProtonTheme.colors.backgroundSecondary, shape)
                        .clip(shape = shape),
                    painter = painterResource(id = viewState.image),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.ExtraSmallSpacing),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = viewState.title,
                    style = ProtonTheme.typography.headlineSmallNorm,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = viewState.description,
                    style = ProtonTheme.typography.defaultWeak,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val buttonModifier = Modifier
                .padding(all = MediumSpacing)
                .conditional(isPortrait) {
                    fillMaxWidth()
                }
                .conditional(isLandscape) {
                    widthIn(min = ButtonMinWidth)
                }
                .heightIn(min = ListItemHeight)
            ProtonSolidButton(
                modifier = buttonModifier,
                onClick = viewEvent.onDone,
            ) {
                Text(
                    text = viewState.action,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Preview(widthDp = 600, heightDp = 360)
@Composable
private fun WhatsNewPreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            WhatsNew(
                viewState = WhatsNewViewState(
                    title = "Title",
                    description = "Very very long description that can go on two lines",
                    action = "Action",
                    image = BasePresentation.drawable.img_onboarding
                ),
                viewEvent = object : WhatsNewViewEvent {}
            )
        }
    }
}

private const val ImageRatio = 1.6F
private val ButtonMinWidth = 300.dp

object WhatsNewTestTag {
    const val screen = "whats new screen"
    const val main = "whats new screen"
}
