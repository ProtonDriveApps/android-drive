/*
 * Copyright (c) 2021-2023 Proton AG.
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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultIconSize
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmall
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.TopAppBarComponentTestTag.navigationButton

@Composable
fun TopAppBar(
    navigationIcon: Painter?,
    onNavigationIcon: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    isTitleEncrypted: Boolean = false,
    backgroundColor: Color = ProtonTheme.colors.backgroundNorm,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        navigationIcon = navigationIcon,
        onNavigationIcon = onNavigationIcon,
        title = { modifier ->
            Title(
                title = title,
                isTitleEncrypted = isTitleEncrypted,
                modifier = modifier,
            )
        },
        modifier = modifier,
        backgroundColor = backgroundColor,
        actions = actions,
    )
}

@Composable
fun TopAppBar(
    navigationIcon: Painter?,
    onNavigationIcon: () -> Unit,
    title: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ProtonTheme.colors.backgroundNorm,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = { title(Modifier.testTag(TopAppBarComponentTestTag.appBar)) },
        navigationIcon = {
            if (navigationIcon != null) {
                IconButton(
                    onClick = { onNavigationIcon() },
                    modifier = Modifier.testTag(navigationButton),
                ) {
                    Icon(
                        painter = navigationIcon,
                        contentDescription = null,
                    )
                }
            }
        },
        actions = actions,
        backgroundColor = backgroundColor,
        contentColor = ProtonTheme.colors.textNorm,
        elevation = 0.dp
    )
}
@Composable
private fun Title(
    title: String,
    isTitleEncrypted: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isTitleEncrypted) {
        EncryptedItem(modifier = modifier.height(LARGE_HEIGHT))
    } else {
        Text(
            text = title,
            style = ProtonTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

@Composable
fun ActionButton(
    @DrawableRes icon: Int,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
    iconTintColor: Color = IconTintColor,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier
            .size(ActionButtonSize)
            .padding(ActionIconPadding)
            .clip(shape = CircleShape),
        onClick = { onClick() }
    ) {
        Icon(
            modifier = Modifier.size(ActionIconSize),
            painter = painterResource(id = icon),
            contentDescription = stringResource(id = contentDescription),
            tint = iconTintColor,
        )
    }
}

@Composable
fun TopBarActions(
    actionFlow: Flow<Set<Action>>,
) {
    val actions by rememberFlowWithLifecycle(flow = actionFlow).collectAsState(initial = emptySet())
    actions.forEach { action ->
        ActionButton(
            icon = action.iconResId,
            contentDescription = action.contentDescriptionResId,
            onClick = action.onAction
        )
    }
}

private val IconTintColor @Composable get() = ProtonTheme.colors.iconNorm
private val ActionButtonSize = DefaultButtonMinHeight
private val ActionIconSize = DefaultIconSize
private val ActionIconPadding = ExtraSmallSpacing
// Actual value is private in androidx.compose.material.AppBarKt
val TopAppBarHeight = 56.dp

object TopAppBarComponentTestTag {
    const val appBar = "top app bar"
    const val navigationButton = "top app bar navigation button"
    const val actionButton = "top app bar action button"
}
