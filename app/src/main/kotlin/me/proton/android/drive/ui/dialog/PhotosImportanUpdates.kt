/*
 * Copyright (c) 2025 Proton AG.
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import me.proton.android.drive.ui.component.PromoContainer
import me.proton.android.drive.ui.viewmodel.PhotosImportantUpdatesViewModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultHighlightNorm
import me.proton.core.drive.base.presentation.component.IllustratedMessage
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.i18n.R as I18N

@Composable
fun PhotosImportantUpdates(
    runAction: RunAction,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<PhotosImportantUpdatesViewModel>()
    val viewEvent = remember {
        viewModel.viewEvent(
            runAction = runAction,
        )
    }
    DisposableEffect(Unit){
        onDispose {
            viewEvent.onRemindMeLater()
        }
    }
    PhotosImportantUpdates(
        modifier = modifier
            .navigationBarsPadding()
            .systemBarsPadding(),
        onStart = viewEvent.onStart,
        onRemindMeLater = viewEvent.onRemindMeLater,
    )
}

@Composable
fun PhotosImportantUpdates(
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onRemindMeLater: () -> Unit,
) {
    Column {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.DefaultSpacing),
            text = stringResource(I18N.string.photos_important_updates_title),
            style = ProtonTheme.typography.headline,
        )
        PromoContainer(
            bodyContent = { PhotosImportantUpdatesBody() },
            actionText = stringResource(I18N.string.photos_important_updates_start_action),
            dismissActionText = stringResource(I18N.string.photos_important_updates_cancel_action),
            onAction = onStart,
            onCancel = onRemindMeLater,
            modifier = modifier,
        )
    }
}

@Composable
fun PhotosImportantUpdatesBody(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ProtonDimens.DefaultSpacing),
    ) {
        IllustratedMessage(
            imageContent = { PhotosImportantUpdatesIllustration() },
            titleContent = {
                Text(
                    text = stringResource(I18N.string.photos_important_updates_subtitle),
                    style = ProtonTheme.typography.defaultHighlightNorm.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.padding(
                        top = ProtonDimens.DefaultSpacing,
                    )
                )
            },
            description = stringResource(I18N.string.photos_important_updates_description),
        )
    }
}

@Composable
fun PhotosImportantUpdatesIllustration(
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(BasePresentation.raw.file_transfer_animation)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ProtonDimens.DefaultSpacing),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
        )
    }
}

@Preview
@Composable
private fun PhotosImportantUpdatesPreview() {
    ProtonTheme {
        PhotosImportantUpdates(
            onStart = {},
            onRemindMeLater = {},
        )
    }
}
