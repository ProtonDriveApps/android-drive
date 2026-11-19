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

package me.proton.android.drive.ui.screen.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import me.proton.android.drive.ui.viewmodel.LocatedMapPhoto
import me.proton.android.drive.ui.viewmodel.PhotoMapContent
import me.proton.android.drive.ui.viewmodel.PhotoMapViewModel
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
internal fun AlbumMapScreen(
    onBack: () -> Unit,
    onOpenPhoto: (FileId, AlbumId) -> Unit,
) {
    val viewModel = hiltViewModel<PhotoMapViewModel>()
    val albumId = requireNotNull(viewModel.albumId)
    Column(
        Modifier.fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopAppBar(
            navigationIcon = painterResource(CorePresentation.drawable.ic_proton_arrow_back),
            navigationContentDescription = stringResource(I18N.string.common_back_action),
            onNavigationIcon = onBack,
            title = stringResource(I18N.string.photos_map_album_title),
            actions = { PhotoMapMenu(viewModel) },
        )
        PhotoMapScreen(onOpenPhoto = { onOpenPhoto(it, albumId) }, viewModel = viewModel)
    }
}

@Composable
internal fun PhotoMapScreen(
    onOpenPhoto: (FileId) -> Unit,
    viewModel: PhotoMapViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = state.content.photos
    val showPath by viewModel.showPaths.collectAsStateWithLifecycle()
    val showDatePicker by viewModel.datePickerOpen.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshRelativeDates() }
    val thumbnailRequests = remember(state.range) { mutableStateMapOf<Any, ThumbnailVO>() }
    var camera by rememberSaveable(stateSaver = MapCameraSaver) { mutableStateOf(MapCamera.World) }
    var autoFit by rememberSaveable { mutableStateOf(true) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedZoom by rememberSaveable { mutableStateOf(MIN_ZOOM) }
    val selectedPhotos = remember(photos, selectedKey, selectedZoom) {
        val key = selectedKey ?: return@remember emptyList()
        clusterMapPins(photos, selectedZoom, LocatedMapPhoto::point)
            .firstOrNull { group -> group.photos.any { it.key == key } }
            ?.photos.orEmpty()
    }

    val tileSize = with(LocalDensity.current) { MapTileSize.toPx().toDouble() }
    val fitPadding = with(LocalDensity.current) { 44.dp.toPx().toDouble() }
    LaunchedEffect(photos, viewport, tileSize, autoFit) {
        if (autoFit && viewport != IntSize.Zero && photos.isNotEmpty()) {
            camera = fitMapCamera(
                photos.map(LocatedMapPhoto::point),
                viewport.width.toDouble(),
                viewport.height.toDouble(),
                tileSize,
                fitPadding,
            )
        }
    }

    val paths = remember(photos, showPath, viewModel.zone) {
        if (showPath) dailyMapPathSegments(photos, viewModel.zone, LocatedMapPhoto::captureSeconds) else emptyList()
    }
    val dismissPin = { selectedKey = null }
    BackHandler(selectedPhotos.isNotEmpty(), onBack = dismissPin)

    Column(modifier.fillMaxSize().background(ProtonTheme.colors.backgroundNorm)) {
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { viewport = it }) {
            PhotoMapViewport(
                camera = camera,
                viewport = viewport,
                photos = photos,
                paths = paths,
                thumbnailRequests = thumbnailRequests,
                tileStore = viewModel.tiles,
                onTransform = { pan, anchor, zoom ->
                    autoFit = false
                    camera = camera.move(
                        pan.x.toDouble(), pan.y.toDouble(), anchor.x.toDouble(), anchor.y.toDouble(), zoom, tileSize,
                    )
                },
                onSelectPin = { pin ->
                    autoFit = false
                    selectedKey = pin.photos.first().key
                    selectedZoom = clusterZoom(camera.zoom)
                    // The second pane changes the map's height. Keeping the pin as
                    // the camera center puts it in the middle of the remaining map.
                    camera = camera.copy(center = pin.point)
                },
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PhotoMapDateLabel(state.dateSelection)
                PhotoMapStatus(
                    content = state.content,
                    thumbnailRequests = thumbnailRequests,
                    onRetry = viewModel::retry,
                )
            }
        }

        if (selectedPhotos.isNotEmpty()) {
            MapPhotosCard(
                photos = selectedPhotos,
                zone = viewModel.zone,
                thumbnailRequests = thumbnailRequests,
                onClose = dismissPin,
                onOpenPhoto = onOpenPhoto,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }

    if (showDatePicker) {
        val dateBounds = state.content.dateBounds
        val today = LocalDate.now(viewModel.zone)
        val initialRange = state.range ?: dateBounds ?: MapDateRange(today, today)
        PhotoMapDatePicker(
            selection = state.dateSelection,
            initialRange = initialRange,
            dateBounds = dateBounds?.including(initialRange) ?: initialRange,
            zone = viewModel.zone,
            onDismiss = { viewModel.setDatePickerOpen(false) },
            onConfirm = { selection ->
                viewModel.setDatePickerOpen(false)
                if (selection != state.dateSelection) {
                    dismissPin()
                    autoFit = true
                    viewModel.selectDates(selection)
                }
            },
        )
    }
}

@Composable
internal fun PhotoMapMenu(viewModel: PhotoMapViewModel) {
    val showPaths by viewModel.showPaths.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Box {
        ActionButton(
            icon = CorePresentation.drawable.ic_proton_three_dots_vertical,
            contentDescription = I18N.string.common_more,
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(I18N.string.photos_map_select_dates)) },
                onClick = {
                    expanded = false
                    viewModel.setDatePickerOpen(true)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (showPaths) I18N.string.photos_map_hide_path else I18N.string.photos_map_show_path))
                },
                onClick = {
                    expanded = false
                    viewModel.togglePaths()
                },
            )
        }
    }
}

@Composable
private fun PhotoMapDateLabel(selection: MapDateSelection) {
    val label = when (selection) {
        is MapDateSelection.Preset -> {
            if (selection.preset == MapDatePreset.ALL_TIME) return
            stringResource(selection.preset.label)
        }
        is MapDateSelection.Custom -> {
            val locale = LocalConfiguration.current.locales[0]
            val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
            val (start, end) = selection.range
            val startLabel = start.format(formatter)
            if (start == end) startLabel else {
                stringResource(I18N.string.photos_map_custom_dates, startLabel, end.format(formatter))
            }
        }
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ProtonTheme.colors.backgroundNorm,
        shadowElevation = 2.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = ProtonTheme.colors.textNorm,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PhotoMapStatus(
    content: PhotoMapContent,
    thumbnailRequests: Map<Any, ThumbnailVO>,
    onRetry: () -> Unit,
) {
    if (content.loading) return
    val loadingThumbnails = thumbnailRequests.values.toSet().size
    val showThumbnailStatus = loadingThumbnails > 0
    val showEmptyRange = content.photos.isEmpty() && !content.hasPhotosWithoutLocation
    if (!content.hasError && !showEmptyRange && !showThumbnailStatus) return
    Surface(
        modifier = if (showThumbnailStatus) Modifier.fillMaxWidth() else Modifier,
        shape = RoundedCornerShape(8.dp),
        color = ProtonTheme.colors.backgroundNorm,
        shadowElevation = 2.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                content.hasError -> Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(I18N.string.photos_map_photos_unavailable),
                        modifier = Modifier.weight(1f, fill = false),
                        color = ProtonTheme.colors.textNorm,
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(I18N.string.common_retry_action)) }
                }
                showEmptyRange -> Text(
                    text = stringResource(I18N.string.photos_map_no_photos),
                    modifier = Modifier.padding(12.dp),
                    color = ProtonTheme.colors.textNorm,
                )
            }
            if (showThumbnailStatus) {
                Text(
                    text = pluralStringResource(
                        I18N.plurals.photos_map_thumbnail_progress, loadingThumbnails, loadingThumbnails,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = ProtonTheme.colors.textNorm,
                )
            }
        }
    }
}

@Composable
private fun MapPhotosCard(
    photos: List<LocatedMapPhoto>,
    zone: ZoneId,
    thumbnailRequests: MutableMap<Any, ThumbnailVO>,
    onClose: () -> Unit,
    onOpenPhoto: (FileId) -> Unit,
    modifier: Modifier,
) {
    val groups = remember(photos, zone) { groupMapPhotosByDate(photos, zone, LocatedMapPhoto::captureSeconds) }
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Surface(modifier, color = ProtonTheme.colors.backgroundNorm, shadowElevation = 6.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pluralStringResource(I18N.plurals.photos_map_pin_count, photos.size, photos.size),
                    color = ProtonTheme.colors.textNorm,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(CorePresentation.drawable.ic_proton_cross),
                        contentDescription = stringResource(I18N.string.common_close_action),
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                groups.forEach { (date, items) ->
                    item(key = date.toEpochDay(), span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = date.format(formatter),
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = ProtonTheme.colors.textNorm,
                        )
                    }
                    items(items, key = { it.key }) { photo ->
                        MapPhotoThumbnail(
                            photo = photo,
                            description = photo.name ?: pluralStringResource(I18N.plurals.photos_map_pin_count, 1, 1),
                            thumbnailRequests = thumbnailRequests,
                            modifier = Modifier.aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ProtonTheme.colors.backgroundSecondary)
                                .clickable(role = Role.Button) { onOpenPhoto(photo.id) },
                        )
                    }
                }
            }
        }
    }
}

private val LocatedMapPhoto.key: String get() = "${id.shareId.id}/${id.id}"


private val MapCameraSaver = listSaver<MapCamera, Double>(
    save = { listOf(it.center.x, it.center.y, it.zoom) },
    restore = { MapCamera(MapPoint(it[0], it[1]), it[2]) },
)
