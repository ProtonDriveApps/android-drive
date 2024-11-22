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

package me.proton.core.drive.base.presentation.component

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.ProtonSecondaryButton
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.compose.theme.headlineSmallNorm
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.base.presentation.viewevent.OnboardingViewEvent
import me.proton.core.drive.base.presentation.viewstate.OnboardingViewState
import me.proton.core.drive.i18n.R as I18N

@Composable
fun Onboarding(
    viewState: OnboardingViewState,
    viewEvent: OnboardingViewEvent,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        viewEvent.onboardingShown()
    }
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SmallSpacing),
            contentAlignment = Alignment.CenterEnd,
        ) {
            ProtonTextButton(
                onClick = viewEvent.onSkip,
            ) {
                Text(
                    text = viewState.skipButtonTitle,
                    style = ProtonTheme.typography.headlineSmallNorm,
                    color = ProtonTheme.colors.interactionNorm,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_onboarding),
                contentDescription = null,
            )
            Text(
                text = viewState.title,
                textAlign = TextAlign.Center,
                style = ProtonTheme.typography.headlineNorm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MediumSpacing)
                    .padding(top = MediumSpacing, bottom = SmallSpacing),
            )
            Text(
                text = buildDescription(
                    descriptionPart1 = stringResource(id = me.proton.core.drive.i18n.R.string.onboarding_description_part_one),
                    descriptionPart2 = stringResource(id = me.proton.core.drive.i18n.R.string.onboarding_description_part_two),
                    freeUserSuffix = stringResource(id = me.proton.core.drive.i18n.R.string.onboarding_description_free),
                    availableStorage = viewState.availableStorage,
                    isFreeUser = viewState.isFreeUser,
                    LocalContext.current,
                    color = ProtonTheme.colors.textAccent,
                ),
                textAlign = TextAlign.Center,
                style = ProtonTheme.typography.body1Regular,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MediumSpacing)
                    .padding(bottom = SmallSpacing),
            )
        }
        BottomActions(
            primaryActionTitle = viewState.primaryButtonTitle,
            secondaryActionTitle = viewState.secondaryButtonTitle,
            onPrimaryAction = viewEvent.onPrimaryAction,
            onSecondaryAction = viewEvent.onSecondaryAction,
        )
    }
}

internal fun buildDescription(
    descriptionPart1: String,
    descriptionPart2: String,
    freeUserSuffix: String,
    availableStorage: Bytes,
    isFreeUser: Boolean,
    context: Context,
    color: Color,
): AnnotatedString =
    buildAnnotatedString {
        append(descriptionPart1)
        append(" ")
        withStyle(style = SpanStyle(color = color, fontWeight = FontWeight.W500)) {
            append(availableStorage.asHumanReadableString(context))
            if (isFreeUser) {
                append(" ")
                append(freeUserSuffix)
            }
        }
        append(" ")
        append(descriptionPart2)
    }

@Composable
internal fun BottomActions(
    primaryActionTitle: String,
    secondaryActionTitle: String,
    modifier: Modifier = Modifier,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    val buttonModifier = Modifier
        .conditional(isPortrait) {
            fillMaxWidth()
        }
        .conditional(isLandscape) {
            widthIn(min = ButtonMinWidth)
        }
        .heightIn(min = ProtonDimens.ListItemHeight)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = MediumSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProtonSolidButton(
            modifier = buttonModifier,
            onClick = onPrimaryAction,
        ) {
            Text(
                text = primaryActionTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MediumSpacing)
        )
        ProtonSecondaryButton(
            modifier = buttonModifier,
            onClick = onSecondaryAction,
        ) {
            Text(
                text = secondaryActionTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val ButtonMinWidth = 300.dp

@Preview
@Composable
fun OnboardingPreview() {
    ProtonTheme {
        Surface {
            Onboarding(
                viewState = OnboardingViewState(
                    title = stringResource(id = I18N.string.onboarding_title),
                    availableStorage = 5.GiB,
                    isFreeUser = true,
                    skipButtonTitle = stringResource(id = I18N.string.onboarding_action_skip),
                    primaryButtonTitle = stringResource(id = I18N.string.onboarding_action_primary),
                    secondaryButtonTitle = stringResource(id = I18N.string.onboarding_action_secondary),
                    doneButtonTitle = stringResource(id = I18N.string.onboarding_action_done),
                ),
                viewEvent = object : OnboardingViewEvent {},
            )
        }
    }
}
