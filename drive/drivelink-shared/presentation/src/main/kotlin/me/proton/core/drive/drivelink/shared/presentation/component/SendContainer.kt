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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.drive.drivelink.shared.presentation.viewstate.SaveButtonViewState
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
internal fun SendContainer(
    saveButtonViewState: SaveButtonViewState,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (saveButtonViewState.isVisible) {
        Column(modifier = modifier) {

            Divider(
                color = ProtonTheme.colors.separatorNorm,
            )
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = ProtonDimens.ListItemHeight)
                    .fillMaxWidth()
                    .padding(
                        horizontal = ProtonDimens.DefaultSpacing,
                        vertical = ProtonDimens.SmallSpacing
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier.weight(1F),
                    text = saveButtonViewState.label,
                    style = ProtonTheme.typography.defaultWeak(saveButtonViewState.isEnabled)
                )
                if (saveButtonViewState.inProgress) {
                    Box(
                        modifier = Modifier.size(ProtonDimens.DefaultButtonMinHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSave,
                        enabled = saveButtonViewState.isEnabled
                    ) {
                        Icon(
                            painter = painterResource(id = CorePresentation.drawable.ic_proton_paper_plane_horizontal),
                            contentDescription = stringResource(
                                id = I18N.string.common_send_action
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SendContainerPreview(
    @PreviewParameter(SaveButtonViewStateParameterProvider::class) viewState: SaveButtonViewState,
) {
    ProtonTheme {
        Surface {
            SendContainer(
                saveButtonViewState = viewState,
                onSave = {},
            )
        }
    }
}

private class SaveButtonViewStateParameterProvider : PreviewParameterProvider<SaveButtonViewState> {
    override val values = sequenceOf(
        SaveButtonViewState(
            label = "Label",
            isVisible = true,
            isEnabled = true,
            inProgress = false
        ),
        SaveButtonViewState(
            label = "Label",
            isVisible = true,
            isEnabled = false,
            inProgress = false
        ),
        SaveButtonViewState(
            label = "Label",
            isVisible = true,
            isEnabled = false,
            inProgress = true
        ),
        SaveButtonViewState(
            label = "Label",
            isVisible = false,
            isEnabled = false,
            inProgress = false
        ),
        SaveButtonViewState(
            label = "Very very very very very very very very very Long Label",
            isVisible = true,
            isEnabled = true,
            inProgress = false
        ),
    )
}
