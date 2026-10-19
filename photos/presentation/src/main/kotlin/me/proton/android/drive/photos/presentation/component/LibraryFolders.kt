/*
 * Copyright (c) 2023 Proton AG.
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

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import me.proton.android.drive.photos.presentation.state.LibraryFolder
import me.proton.android.drive.photos.presentation.state.LibraryFoldersState
import me.proton.android.drive.photos.presentation.viewmodel.LibraryFoldersViewModel
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.drive.base.presentation.component.ProtonRawListItem
import me.proton.core.drive.base.presentation.component.ProtonSettingsHeader
import me.proton.core.drive.base.presentation.theme.DriveTheme
import me.proton.core.drive.link.domain.entity.FolderId

@Composable
fun LibraryFolders(
    modifier: Modifier = Modifier,
    navigateToConfirmStopSyncFolder: (FolderId, Int) -> Unit,
) {
    val viewModel = hiltViewModel<LibraryFoldersViewModel>()
    val libraryFoldersState by rememberFlowWithLifecycle(flow = viewModel.state).collectAsState(
        initial = null
    )
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateToConfirmStopSyncFolder = navigateToConfirmStopSyncFolder,
        )
    }
    when (val state = libraryFoldersState) {
        is LibraryFoldersState.Content -> LibraryFoldersList(
            modifier = modifier,
            content = state,
            onToggleBucket = { id, enable ->
                viewEvent.onToggleBucket(id, enable)
            }
        )

        LibraryFoldersState.NoPermissions -> Unit
        null -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryFoldersList(
    content: LibraryFoldersState.Content,
    onToggleBucket: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .height(1000.dp) // Needed because of nested scrolling
    ) {
        ProtonSettingsHeader(title = content.title)
        Text(
            modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
            text = content.description,
            color = ProtonTheme.colors.textHint,
            style = ProtonTheme.typography.defaultSmallNorm
        )
        LazyColumn(
            modifier = modifier.padding(top = ProtonDimens.DefaultSpacing),
        ) {
            val itemModifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = ProtonDimens.ListItemHeight)
                .padding(
                    vertical = ProtonDimens.SmallSpacing,
                    horizontal = ProtonDimens.DefaultSpacing,
                )
            items(
                content.folders,
                key = { libraryFolder -> libraryFolder.id },
                contentType = { it.javaClass },
            ) { libraryFolder ->
                when (libraryFolder) {
                    is LibraryFolder.Entry -> LibraryFolderItem(
                        modifier = itemModifier.animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                ),
                        ),
                        name = libraryFolder.name,
                        description = {
                            Text(
                                libraryFolder.description,
                                style = ProtonTheme.typography.captionWeak(libraryFolder.enabled),
                            )
                        },
                        uri = libraryFolder.uri,
                        checked = libraryFolder.enabled,
                        onToggle = { enabled -> onToggleBucket(libraryFolder.id, enabled) },
                    )

                    is LibraryFolder.NotFound -> LibraryFolderItem(
                        modifier = itemModifier,
                        name = libraryFolder.name,
                        description = {
                            Text(
                                libraryFolder.description,
                                style = ProtonTheme.typography.captionWeak(false).copy(
                                    color = ProtonTheme.colors.notificationError
                                ),
                            )
                        },
                        checked = false,
                        enabled = false,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibraryFolderItem(
    name: String,
    description: @Composable (ColumnScope.() -> Unit),
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    uri: Uri? = null,
    onToggle: ((Boolean) -> Unit)? = null,
    role: Role = Role.Switch,
    toggleControl: @Composable (checked: Boolean, enabled: Boolean) -> Unit = { isChecked, isEnabled ->
        Switch(checked = isChecked, enabled = isEnabled, onCheckedChange = null)
    },
) {
    ProtonRawListItem(
        modifier = Modifier
            .toggleable(
                value = checked,
                enabled = onToggle != null && enabled,
                role = role,
                onValueChange = { enable ->
                    onToggle?.invoke(enable)
                },
            )
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing)
    ) {
        Surface(
            shape = RoundedCornerShape(ProtonDimens.DefaultCornerRadius),
        ) {
            Image(
                modifier = Modifier.size(ThumbnailSize),
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .scale(Scale.FILL)
                        .data(uri)
                        .size(with(LocalDensity.current) { ThumbnailSize.toPx().toInt() })
                        .build()
                ),
                contentDescription = "",
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                name,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                style = ProtonTheme.typography.defaultNorm(checked),
            )
            description()
        }
        toggleControl(checked, enabled)
    }
}

private val ThumbnailSize = 40.dp

@Preview
@Composable
fun LibraryFoldersListPreview() {
    DriveTheme {
        Surface {
            LibraryFoldersList(
                content = LibraryFoldersState.Content(
                    title = "Upload from",
                    description = "Your Camera folder is backed up automatically." +
                            " Select additional folders to back up and their photos will appear in the Photos tab.",
                    folders = listOf(
                        LibraryFolder.NotFound(
                            name = "Camera",
                            description = "No Camera folder found for backup"
                        ),
                        LibraryFolder.Entry(
                            id = 2,
                            name = "MyCamera",
                            description = "1234 photos, 123 videos",
                            uri = null,
                            enabled = true
                        ),
                        LibraryFolder.Entry(
                            id = 0,
                            name = "Photos",
                            description = "1 photo",
                            uri = null,
                            enabled = false
                        ),
                        LibraryFolder.Entry(
                            id = 1,
                            name = "Videos",
                            description = "1 video",
                            uri = null,
                            enabled = false
                        ),
                    )
                ),
                onToggleBucket = { _, _ -> }
            )
        }
    }
}
