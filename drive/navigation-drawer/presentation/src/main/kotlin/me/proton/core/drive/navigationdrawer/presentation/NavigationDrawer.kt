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
package me.proton.core.drive.navigationdrawer.presentation

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonListSectionTitle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.presentation.component.NavigationDrawerAppVersion
import me.proton.core.drive.base.presentation.component.ProtonListItem
import me.proton.core.drive.base.presentation.extension.driveCustomCitrusGreen
import me.proton.core.drive.user.presentation.storage.StorageIndicator
import me.proton.core.drive.user.presentation.user.PREVIEW_USER
import me.proton.core.drive.user.presentation.user.UserSelector
import me.proton.core.drive.user.presentation.user.extension.getStorageIndicatorData
import me.proton.core.plan.presentation.compose.component.UpgradeStorageInfo
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun NavigationDrawer(
    drawerState: DrawerState,
    viewState: NavigationDrawerViewState,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
    onCloseOnActionStarted: (() -> Unit)? = null,
    onCloseOnActionCompleted: (() -> Unit)? = null,
) {
    ProtonTheme(
        colors = ProtonTheme.colors.sidebarColors ?: ProtonTheme.colors
    ) {
        val scope = rememberCoroutineScope()
        val closeDrawerAction: (() -> Unit) -> Unit = remember(viewState.closeOnActionEnabled) {
            if (viewState.closeOnActionEnabled) {
                { onClose ->
                    scope.launch {
                        onCloseOnActionStarted?.invoke()
                        drawerState.close()
                        onClose()
                        onCloseOnActionCompleted?.invoke()
                    }
                }
            } else {
                { onClose -> onClose() }
            }
        }
        BackHandler(enabled = viewState.closeOnBackEnabled && drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(modifier) {
                if (viewState.currentUser != null) {
                    UserSelector(
                        currentUser = viewState.currentUser,
                        modifier = Modifier.padding(SmallSpacing),
                        canChangeUser = false,
                    ) {
                        // Not implemented yet
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(top = DefaultSpacing)
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                        .testTag(NavigationDrawerTestTag.content),
                    verticalArrangement = Arrangement.Top
                ) {
                    if (viewState.isBlackFridayPromoEnabled) {
                        BlackFridayPromo(closeDrawerAction, viewEvent)
                    } else {
                        UpgradeStorageInfo(
                            onUpgradeClicked = { viewEvent.onSubscription() },
                            withTopDivider = true,
                            withBottomDivider = true
                        )
                    }

                    MyFilesListItem(closeDrawerAction, viewEvent)

                    TrashListItem(closeDrawerAction, viewEvent)

                    OfflineListItem(closeDrawerAction, viewEvent)

                    ProtonListSectionTitle(title = I18N.string.navigation_more_section_header)

                    SettingsListItem(closeDrawerAction, viewEvent)

                    if (viewState.showSubscription) {
                        SubscriptionListItem(closeDrawerAction, viewEvent)
                    }

                    ReportBugListItem(closeDrawerAction, viewEvent)

                    SignOutListItem(closeDrawerAction, viewEvent)

                    if (viewState.currentUser != null) {
                        ProtonListSectionTitle(
                            title = stringResource(I18N.string.navigation_storage_section_header),
                            modifier = Modifier.padding(bottom = SmallSpacing)
                        )

                        Crossfade(
                            targetState = viewState.showGetFreeStorage,
                            label = "GetMoreFreeStorage",
                        ) { showGetFreeStorage ->
                            if (showGetFreeStorage) {
                                GetFreeStorageListItem(closeDrawerAction, viewEvent)
                            }
                        }

                        val (usedSpace, availableSpace, label) = viewState.currentUser.getStorageIndicatorData()

                        StorageIndicator(
                            label = stringResource(id = label),
                            usedBytes = Bytes(usedSpace),
                            availableBytes = Bytes(availableSpace),
                            modifier = Modifier
                                .padding(horizontal = DefaultSpacing)
                                .testTag(NavigationDrawerTestTag.storageIndicator)
                        )
                    }

                    NavigationDrawerAppVersion(
                        name = stringResource(id = viewState.appNameResId),
                        version = viewState.appVersion
                    )
                }
            }
        }
    }
}

@Composable
private fun BlackFridayPromo(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        title = I18N.string.bf_promo_drawer_title,
        icon = BasePresentation.drawable.ic_black_friday_promo,
        iconTintColor = driveCustomCitrusGreen,
        textStyle = ProtonTheme.typography.defaultNorm.copy(color = driveCustomCitrusGreen),
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
    ) {
        viewEvent.onBlackFridayPromo()
    }
}

@Composable
private fun MyFilesListItem(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        title = I18N.string.navigation_item_home,
        icon = CorePresentation.drawable.ic_proton_drive,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
    ) {
        viewEvent.onMyFiles()
    }
}

@Composable
private fun TrashListItem(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        title = I18N.string.navigation_item_trash,
        icon = CorePresentation.drawable.ic_proton_trash,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
    ) {
        viewEvent.onTrash()
    }
}

@Composable
private fun OfflineListItem(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        title = I18N.string.navigation_item_offline,
        icon = CorePresentation.drawable.ic_proton_arrow_down_line,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
    ) {
        viewEvent.onOffline()
    }
}

@Composable
private fun SettingsListItem(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        title = I18N.string.navigation_item_settings,
        icon = CorePresentation.drawable.ic_proton_cog_wheel,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
    ) {
        viewEvent.onSettings()
    }
}

@Composable
private fun SubscriptionListItem(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        title = I18N.string.navigation_item_subscription,
        icon = BasePresentation.drawable.ic_proton_arrow_up_circle_line,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
    ) {
        viewEvent.onSubscription()
    }
}

@Composable
private fun ReportBugListItem(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        title = I18N.string.navigation_item_bug_report,
        icon = CorePresentation.drawable.ic_proton_bug,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
    ) {
        viewEvent.onBugReport()
    }
}

@Composable
private fun SignOutListItem(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        icon = CorePresentation.drawable.ic_proton_arrow_out_from_rectangle,
        title = I18N.string.navigation_item_sign_out,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
    ) {
        viewEvent.onSignOut()
    }
}

@Composable
private fun GetFreeStorageListItem(
    closeDrawerAction: (() -> Unit) -> Unit,
    viewEvent: NavigationDrawerViewEvent,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerListItem(
        icon = CorePresentation.drawable.ic_proton_gift,
        iconTintColor = ProtonTheme.colors.iconNorm,
        title = I18N.string.navigation_item_get_free_storage,
        textStyle = ProtonTheme.typography.defaultStrongNorm,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier.padding(bottom = DefaultSpacing),
    ) {
        viewEvent.onGetFreeStorage()
    }
}

@Composable
fun NavigationDrawerListItem(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    closeDrawerAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    iconTintColor: Color = ProtonTheme.colors.iconWeak,
    textStyle: TextStyle = ProtonTheme.typography.defaultNorm,
    onClick: () -> Unit,
) {
    ProtonListItem(
        icon = icon,
        iconTintColor = iconTintColor,
        title = title,
        textStyle = textStyle,
        modifier = modifier
            .clickable {
                closeDrawerAction(onClick)
            }
            .padding(horizontal = DefaultSpacing),
    )
}

object NavigationDrawerTestTag {
    const val content = "navigation drawer content"
    const val storageIndicator = "navigation drawer storage indicator"
}

@Preview(name = "Drawer opened")
@Composable
fun PreviewDrawerWithUser() {
    ProtonTheme {
        NavigationDrawer(
            drawerState = DrawerState(DrawerValue.Open) { true },
            viewState = NavigationDrawerViewState(
                I18N.string.common_app,
                "Version",
                currentUser = PREVIEW_USER
            ),
            viewEvent = object : NavigationDrawerViewEvent {
                override val onMyFiles = {}
                override val onTrash = {}
                override val onOffline = {}
                override val onSettings = {}
                override val onSignOut = {}
                override val onBugReport = {}
                override val onSubscription = {}
                override val onGetFreeStorage = {}
                override val onBlackFridayPromo = {}
            }
        )
    }
}

@Preview(name = "Drawer opened")
@Composable
private fun PreviewDrawerWithoutUser() {
    ProtonTheme {
        NavigationDrawer(
            drawerState = DrawerState(DrawerValue.Open) { true },
            viewState = NavigationDrawerViewState(I18N.string.common_app, "Version"),
            viewEvent = object : NavigationDrawerViewEvent {
                override val onMyFiles = {}
                override val onTrash = {}
                override val onOffline = {}
                override val onSettings = {}
                override val onSignOut = {}
                override val onBugReport = {}
                override val onSubscription = {}
                override val onGetFreeStorage = {}
                override val onBlackFridayPromo = {}
            }
        )
    }
}
