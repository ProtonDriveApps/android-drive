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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import me.proton.android.drive.ui.viewmodel.LocatedMapPhoto
import me.proton.android.drive.ui.viewmodel.PhotoMapContent
import me.proton.android.drive.ui.viewmodel.PhotoMapViewModel
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
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
        )
        PhotoMapScreen(onOpenPhoto = { onOpenPhoto(it, albumId) }, viewModel = viewModel)
    }
}

@Composable
internal fun PhotoMapScreen(
    onOpenPhoto: (FileId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = state.content.photos
    var showPath by rememberSaveable { mutableStateOf(false) }
    var downloadThumbnails by rememberSaveable(state.range) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val thumbnailSession = remember(state.range, viewModel.thumbnails, scope) {
        viewModel.thumbnails.openSession(scope)
    }
    DisposableEffect(thumbnailSession) {
        onDispose { thumbnailSession.cancel() }
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
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
        PhotoMapFilters(state.range, showPath, { showDatePicker = true }, { showPath = it })
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { viewport = it }) {
            PhotoMapViewport(
                camera = camera,
                viewport = viewport,
                photos = photos,
                paths = paths,
                downloadThumbnails = downloadThumbnails,
                thumbnailSession = thumbnailSession,
                thumbnails = viewModel.thumbnails,
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
            PhotoMapStatus(
                content = state.content,
                thumbnailSession = thumbnailSession,
                downloadThumbnails = downloadThumbnails,
                onCancelThumbnails = {
                    thumbnailSession.cancel()
                    downloadThumbnails = false
                },
                onRetry = viewModel::retry,
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
            )
        }

        if (selectedPhotos.isNotEmpty()) {
            MapPhotosCard(
                photos = selectedPhotos,
                zone = viewModel.zone,
                downloadThumbnails = downloadThumbnails,
                thumbnailSession = thumbnailSession,
                thumbnails = viewModel.thumbnails,
                onClose = dismissPin,
                onOpenPhoto = onOpenPhoto,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }

    if (showDatePicker) {
        PhotoMapDatePicker(
            initialRange = state.range,
            onDismiss = { showDatePicker = false },
            onConfirm = { range ->
                showDatePicker = false
                if (range != state.range) {
                    dismissPin()
                    autoFit = true
                    viewModel.selectRange(range)
                }
            },
        )
    }
}

@Composable
private fun PhotoMapFilters(
    range: MapDateRange,
    showPath: Boolean,
    onSelectDates: () -> Unit,
    onTogglePath: (Boolean) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onSelectDates, modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(I18N.string.photos_map_date_range, range.start.format(formatter), range.end.format(formatter)),
                color = ProtonTheme.colors.textNorm,
            )
        }
        Row(
            modifier = Modifier.toggleable(showPath, role = Role.Checkbox, onValueChange = onTogglePath)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = showPath, onCheckedChange = null)
            Text(stringResource(I18N.string.photos_map_show_path), color = ProtonTheme.colors.textNorm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoMapDatePicker(
    initialRange: MapDateRange,
    onDismiss: () -> Unit,
    onConfirm: (MapDateRange) -> Unit,
) {
    val picker = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange.start.toUtcMillis(),
        initialSelectedEndDateMillis = initialRange.end.toUtcMillis(),
    )
    val start = picker.selectedStartDateMillis
    val end = picker.selectedEndDateMillis
    val primary = ProtonTheme.colors.interactionNorm
    val colors = if (ProtonTheme.colors.isDark) darkColorScheme(primary = primary) else lightColorScheme(primary = primary)
    MaterialTheme(colorScheme = colors) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    enabled = start != null && end != null,
                    onClick = {
                        if (start != null && end != null) {
                            onConfirm(MapDateRange(start.utcDate(), end.utcDate()))
                        }
                    },
                ) {
                    Text(stringResource(I18N.string.common_done_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(I18N.string.common_cancel_action))
                }
            },
        ) {
            DateRangePicker(state = picker, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PhotoMapStatus(
    content: PhotoMapContent,
    thumbnailSession: PhotoMapThumbnails.Session,
    downloadThumbnails: Boolean,
    onCancelThumbnails: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    val thumbnailProgress by thumbnailSession.progress.collectAsStateWithLifecycle()
    val showThumbnailProgress = downloadThumbnails && thumbnailProgress.loading
    val missingCount = content.withoutLocationCount
    val showMissingLocations = !content.loading && missingCount > 0
    val showEmptyRange = !content.loading && !content.hasError && content.photos.isEmpty() && missingCount == 0
    if (!content.loading && !content.hasError && !showMissingLocations && !showEmptyRange && !showThumbnailProgress) {
        return
    }
    Surface(
        modifier = if (content.loading || showThumbnailProgress) modifier.fillMaxWidth() else modifier,
        shape = RoundedCornerShape(8.dp),
        color = ProtonTheme.colors.backgroundNorm,
        shadowElevation = 2.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                content.loading -> {
                    val label = if (content.locationsTotal > 0) {
                        stringResource(
                            I18N.string.photos_map_location_progress, content.locationsChecked, content.locationsTotal,
                        )
                    } else {
                        stringResource(I18N.string.photos_map_loading_locations)
                    }
                    PhotoMapProgress(label, content.locationsChecked, content.locationsTotal)
                }
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
            if (showMissingLocations) {
                Text(
                    text = pluralStringResource(I18N.plurals.photos_map_without_location, missingCount, missingCount),
                    modifier = Modifier.padding(12.dp),
                    color = ProtonTheme.colors.textNorm,
                )
            }
            if (showThumbnailProgress) {
                val total = thumbnailProgress.active + thumbnailProgress.queued + thumbnailProgress.downloaded
                PhotoMapProgress(
                    label = stringResource(
                        I18N.string.photos_map_thumbnail_progress, thumbnailProgress.active, thumbnailProgress.queued,
                    ),
                    completed = thumbnailProgress.downloaded,
                    total = total,
                )
                TextButton(onClick = onCancelThumbnails) { Text(stringResource(I18N.string.common_cancel_action)) }
            }
        }
    }
}

@Composable
private fun PhotoMapProgress(label: String, completed: Int, total: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, color = ProtonTheme.colors.textNorm, style = MaterialTheme.typography.bodySmall)
        val bar = Modifier.fillMaxWidth().padding(top = 5.dp)
        if (total == 0) {
            LinearProgressIndicator(modifier = bar, color = ProtonTheme.colors.interactionNorm)
        } else {
            LinearProgressIndicator(
                progress = { (completed.toFloat() / total).coerceIn(0f, 1f) },
                modifier = bar,
                color = ProtonTheme.colors.interactionNorm,
            )
        }
    }
}

@Composable
private fun MapPhotosCard(
    photos: List<LocatedMapPhoto>,
    zone: ZoneId,
    downloadThumbnails: Boolean,
    thumbnailSession: PhotoMapThumbnails.Session,
    thumbnails: PhotoMapThumbnails,
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
                            description = photo.name,
                            download = downloadThumbnails,
                            thumbnailSession = thumbnailSession,
                            thumbnails = thumbnails,
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

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.utcDate(): LocalDate = Instant.ofEpochMilli(this).atOffset(ZoneOffset.UTC).toLocalDate()

private val MapCameraSaver = listSaver<MapCamera, Double>(
    save = { listOf(it.center.x, it.center.y, it.zoom) },
    restore = { MapCamera(MapPoint(it[0], it[1]), it[2]) },
)
