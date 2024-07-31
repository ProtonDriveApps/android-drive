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

package me.proton.core.drive.log.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.LargeSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallIconSize
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.CircleSelection
import me.proton.core.drive.log.domain.entity.Log
import me.proton.core.drive.log.presentation.entity.LogOriginItem
import me.proton.core.drive.log.presentation.entity.LogLevelItem
import me.proton.core.drive.log.presentation.viewevent.LogOptionsViewEvent
import me.proton.core.drive.log.presentation.viewstate.LogOptionsViewState
import me.proton.core.drive.i18n.R as I18N

@Composable
fun LogOptions(
    logLevelItems: Flow<Set<LogLevelItem>>,
    logOriginItems: Flow<Set<LogOriginItem>>,
    viewState: LogOptionsViewState,
    viewEvent: LogOptionsViewEvent,
    modifier: Modifier = Modifier,
) {
    val levelItems = logLevelItems.collectAsStateWithLifecycle(initialValue = emptySet())
    val originItems = logOriginItems.collectAsStateWithLifecycle(initialValue = emptySet())
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DefaultSpacing)
    ) {
        LogOptionsSection(
            titleResId = viewState.logLevelItemsLabel,
        )
        LogLevels(
            levels = levelItems.value,
            onClick = viewEvent.onLogLevel,
        )
        Spacer(modifier = Modifier.size(SmallSpacing))
        LogOptionsSection(
            titleResId = viewState.logOriginItemsLabel,
        )
        LogOrigins(
            origins = originItems.value,
            onClick = viewEvent.onLogOrigin,
        )
    }
}

@Composable
fun LogOptionsSection(
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(id = titleResId),
        style = ProtonTheme.typography.body1Medium,
        modifier = modifier.padding(horizontal = DefaultSpacing)
    )
}

@Composable
fun LogLevels(
    levels: Set<LogLevelItem>,
    modifier: Modifier = Modifier,
    onClick: (Log.Level) -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        levels.map { levelItem ->
            LogOptionItem(
                levelItem = levelItem,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun LogOrigins(
    origins: Set<LogOriginItem>,
    modifier: Modifier = Modifier,
    onClick: (Log.Origin) -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        origins.map { originItem ->
            LogOptionItem(
                originItem = originItem,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun LogOptionItem(
    levelItem: LogLevelItem,
    modifier: Modifier = Modifier,
    onClick: (Log.Level) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(levelItem.level) }
            .padding(horizontal = LargeSpacing, vertical = SmallSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = levelItem.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = DefaultSpacing)
                .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CircleSelection(
            isSelected = levelItem.isChecked,
            modifier = Modifier.size(SmallIconSize)
        )
    }
}

@Composable
fun LogOptionItem(
    originItem: LogOriginItem,
    modifier: Modifier = Modifier,
    onClick: (Log.Origin) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(originItem.origin) }
            .padding(horizontal = LargeSpacing, vertical = SmallSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = originItem.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = DefaultSpacing)
                .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CircleSelection(
            isSelected = originItem.isChecked,
            modifier = Modifier.size(SmallIconSize)
        )
    }
}

@Preview
@Composable
fun LogOptionsPreview() {
    ProtonTheme {
        Surface {
            LogOptions(
                logLevelItems = flowOf(
                    setOf(
                        LogLevelItem(
                            title = "Default",
                            isChecked = true,
                            level = Log.Level.NORMAL,
                        ),
                        LogLevelItem(
                            title = "Error",
                            isChecked = true,
                            level = Log.Level.ERROR,
                        )
                    )
                ),
                logOriginItems = flowOf(
                    setOf(
                        LogOriginItem(
                            title = "API",
                            isChecked = true,
                            origin = Log.Origin.EVENT_NETWORK,
                        ),
                        LogOriginItem(
                            title = "Exception",
                            isChecked = false,
                            origin = Log.Origin.EVENT_THROWABLE,
                        ),
                    )
                ),
                viewState = LogOptionsViewState(
                    logLevelItemsLabel = I18N.string.log_level,
                    logOriginItemsLabel = I18N.string.log_category,
                ),
                viewEvent = object : LogOptionsViewEvent {},
            )
        }
    }
}
