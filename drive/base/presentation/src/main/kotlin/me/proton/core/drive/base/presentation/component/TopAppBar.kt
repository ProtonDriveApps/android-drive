/*
 * Copyright (c) 2021-2024 Proton AG.
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultIconSize
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmallNorm
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
    notificationDotVisible: Boolean = false,
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
        notificationDotVisible = notificationDotVisible,
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
    contentColor: Color = ProtonTheme.colors.textNorm,
    notificationDotVisible: Boolean = false,
    elevation: Dp = 0.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = { title(Modifier.testTag(TopAppBarComponentTestTag.appBar)) },
        navigationIcon = {
            if (navigationIcon != null) {
                NavigationIconButton(
                    navigationIcon = navigationIcon,
                    onNavigationIcon = onNavigationIcon,
                    notificationDotVisible = notificationDotVisible
                )
            }
        },
        actions = actions,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
    )
}
@Composable
fun Title(
    title: String,
    isTitleEncrypted: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = ProtonTheme.typography.headlineSmallNorm,
) {
    if (isTitleEncrypted) {
        EncryptedItem(modifier = modifier.height(LARGE_HEIGHT))
    } else {
        Text(
            text = title,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

@Composable
private fun NavigationIconButton(
    navigationIcon: Painter,
    onNavigationIcon: () -> Unit,
    notificationDotVisible: Boolean
) {
    IconButton(
        onClick = { onNavigationIcon() },
        modifier = Modifier.testTag(navigationButton),
    ) {
        BoxWithNotificationDot(
            notificationDotVisible = notificationDotVisible,
        ) { modifier ->
            Icon(
                painter = navigationIcon,
                contentDescription = null,
                modifier = modifier.padding(4.dp),
            )
        }
    }
}

@Composable
fun ActionButton(
    @DrawableRes icon: Int,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconTintColor: Color = IconTintColor,
    notificationDotVisible: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier
            .size(ActionButtonSize)
            .padding(ActionIconPadding)
            .clip(shape = CircleShape),
        onClick = { onClick() },
        enabled = enabled,
    ) {
        BoxWithNotificationDot(notificationDotVisible = notificationDotVisible) { modifier ->
            Icon(
                modifier = modifier
                    .padding(4.dp)
                    .size(ActionIconSize),
                painter = painterResource(id = icon),
                contentDescription = stringResource(id = contentDescription),
                tint = iconTintColor,
            )
        }
    }
}

@Composable
fun ActionButton(
    @DrawableRes image: Int,
    @StringRes contentDescription: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(ActionButtonSize)
            .clip(shape = CircleShape)
            .clickable { if (enabled) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = image),
            contentDescription = stringResource(id = contentDescription),
        )
    }
}

@Composable
fun TopBarActions(
    actionFlow: Flow<Set<Action>>,
    iconTintColor: Color = IconTintColor,
) {
    val actions by rememberFlowWithLifecycle(flow = actionFlow).collectAsState(initial = emptySet())
    actions.forEach { action ->
        when (action) {
            is Action.Icon -> ActionButton(
                icon = action.iconResId,
                iconTintColor = iconTintColor,
                contentDescription = action.contentDescriptionResId,
                notificationDotVisible = action.notificationDotVisible,
                onClick = action.onAction
            )
            is Action.Image -> ActionButton(
                image = action.imageResId,
                contentDescription = action.contentDescriptionResId,
                onClick = action.onAction
            )
        }
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
