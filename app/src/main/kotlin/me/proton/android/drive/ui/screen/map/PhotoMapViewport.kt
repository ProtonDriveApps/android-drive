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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.roundToInt
import me.proton.android.drive.ui.viewmodel.LocatedMapPhoto
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.i18n.R as I18N

@Composable
internal fun PhotoMapViewport(
    camera: MapCamera,
    viewport: IntSize,
    photos: List<LocatedMapPhoto>,
    paths: List<MapPathSegment<LocatedMapPhoto>>,
    downloadThumbnails: Boolean,
    thumbnailRequests: MutableMap<Any, ThumbnailVO>,
    tileStore: PhotoMapTiles,
    onTransform: (pan: Offset, anchor: Offset, zoom: Double) -> Unit,
    onSelectPin: (MapPin<LocatedMapPhoto>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tileSize = with(LocalDensity.current) { MapTileSize.toPx().toDouble() }
    val markerSize = with(LocalDensity.current) { MAP_PIN_DP.dp.roundToPx() }
    val markerHeight = markerSize + with(LocalDensity.current) { MapPinTipHeight.roundToPx() }
    val transform by rememberUpdatedState(onTransform)
    val worldSize = camera.worldSize(tileSize)
    val pinZoom = clusterZoom(camera.zoom)
    val pins = remember(photos, pinZoom) {
        clusterMapPins(photos, pinZoom, LocatedMapPhoto::point)
    }
    val placements = if (viewport == IntSize.Zero) emptyList() else visibleTiles(camera, viewport, tileSize)
    val images = placements.map(TilePlacement::tile).distinct().associateWith { tile ->
        key(tile) {
            val image by produceState<TileImage>(TileImage.Loading, tileStore, tile) {
                value = TileImage.Loaded(tileStore.load(tile))
            }
            image
        }
    }

    Box(
        modifier
            .clipToBounds()
            .background(ProtonTheme.colors.backgroundSecondary)
            .pointerInput(viewport) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                    transform(pan, centroid - center, log2(zoom.toDouble()))
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            placements.forEach { placement ->
                val bitmap = (images[placement.tile] as? TileImage.Loaded)?.bitmap ?: return@forEach
                val left = placement.left.roundToInt()
                val top = placement.top.roundToInt()
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(left, top),
                    dstSize = IntSize(
                        (placement.left + placement.size).roundToInt() - left,
                        (placement.top + placement.size).roundToInt() - top,
                    ),
                )
            }
            paths.forEach { (from, to) ->
                val start = from.point.toScreen(camera, viewport, worldSize)
                val end = Offset(
                    x = start.x + (wrappedDistance(to.point.x, from.point.x) * worldSize).toFloat(),
                    y = start.y + ((to.point.y - from.point.y) * worldSize).toFloat(),
                )
                for (copy in -1..1) {
                    val shift = Offset((worldSize * copy).toFloat(), 0f)
                    drawLine(
                        color = Color(0xFF1976D2),
                        start = start + shift,
                        end = end + shift,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        if (images.isNotEmpty() && images.values.all { it is TileImage.Loaded && it.bitmap == null }) {
            Text(
                text = stringResource(I18N.string.photos_map_tiles_unavailable),
                modifier = Modifier.align(Alignment.Center).mapControlBackground().padding(8.dp),
                color = ProtonTheme.colors.textNorm,
            )
        }

        pins.forEach { pin ->
            val position = pin.point.toScreen(camera, viewport, worldSize)
            if (position.x in -markerSize.toFloat()..(viewport.width + markerSize).toFloat() &&
                position.y in 0f..(viewport.height + markerHeight).toFloat()
            ) {
                key(pin.photos.first().id) {
                    PhotoMapPin(
                        pin = pin,
                        downloadThumbnails = downloadThumbnails,
                        thumbnailRequests = thumbnailRequests,
                        onClick = { onSelectPin(pin) },
                        modifier = Modifier.offset {
                            IntOffset(
                                (position.x - markerSize / 2f).roundToInt(),
                                position.y.roundToInt() - markerHeight,
                            )
                        },
                    )
                }
            }
        }

        Column(Modifier.align(Alignment.CenterEnd).padding(8.dp).mapControlBackground()) {
            MapZoomButton(
                label = "+",
                description = stringResource(I18N.string.photos_map_zoom_in),
                enabled = camera.zoom < MAX_ZOOM,
                onClick = { transform(Offset.Zero, Offset.Zero, 1.0) },
            )
            MapZoomButton(
                label = "−",
                description = stringResource(I18N.string.photos_map_zoom_out),
                enabled = camera.zoom > MIN_ZOOM,
                onClick = { transform(Offset.Zero, Offset.Zero, -1.0) },
            )
        }

        val uriHandler = LocalUriHandler.current
        Text(
            text = stringResource(I18N.string.photos_map_attribution),
            color = ProtonTheme.colors.textNorm,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart)
                .mapControlBackground()
                .clickable(role = Role.Button) { uriHandler.openUri("https://www.openstreetmap.org/copyright") }
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun PhotoMapPin(
    pin: MapPin<LocatedMapPhoto>,
    downloadThumbnails: Boolean,
    thumbnailRequests: MutableMap<Any, ThumbnailVO>,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val representative = pin.photos.first()
    val count = pin.photos.size
    val description = if (count == 1 && representative.name != null) representative.name else {
        pluralStringResource(I18N.plurals.photos_map_pin_count, count, count)
    }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier.semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(MAP_PIN_DP.dp)
                .shadow(3.dp, shape)
                .clip(shape)
                .background(ProtonTheme.colors.backgroundNorm)
                .border(2.dp, Color.White, shape),
        ) {
            MapPhotoThumbnail(
                photo = representative,
                description = null,
                download = downloadThumbnails,
                thumbnailRequests = thumbnailRequests,
                modifier = Modifier.fillMaxSize().padding(2.dp).clip(shape),
            )
            if (count > 1) {
                Text(
                    text = count.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.TopEnd)
                        .padding(2.dp)
                        .background(Color(0xFF204D87), CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Canvas(Modifier.size(width = 12.dp, height = MapPinTipHeight)) {
            val triangle = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(triangle, Color.White)
        }
    }
}

@Composable
internal fun MapPhotoThumbnail(
    photo: LocatedMapPhoto,
    description: String?,
    download: Boolean,
    thumbnailRequests: MutableMap<Any, ThumbnailVO>,
    modifier: Modifier,
) {
    val thumbnail = photo.thumbnail
    if (thumbnail == null) {
        Box(modifier.semantics { description?.let { contentDescription = it } })
    } else {
        val context = LocalContext.current
        val request = remember(context, thumbnail, download) {
            ImageRequest.Builder(context)
                .data(thumbnail)
                .networkCachePolicy(if (download) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .build()
        }
        val painter = key(download) {
            rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
        }
        if (download && painter.state is AsyncImagePainter.State.Loading) {
            DisposableEffect(thumbnailRequests, thumbnail) {
                val token = Any()
                thumbnailRequests[token] = thumbnail
                onDispose { thumbnailRequests.remove(token) }
            }
        }
        Image(
            painter = painter,
            contentDescription = description,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MapZoomButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(label, color = if (enabled) ProtonTheme.colors.textNorm else ProtonTheme.colors.textWeak, fontSize = 22.sp)
    }
}

@Composable
private fun Modifier.mapControlBackground() =
    clip(RoundedCornerShape(6.dp)).background(ProtonTheme.colors.backgroundNorm)

internal val MapTileSize = MAP_TILE_DP.dp
private val MapPinTipHeight = 7.dp

private sealed interface TileImage {
    data object Loading : TileImage
    data class Loaded(val bitmap: ImageBitmap?) : TileImage
}

private data class TilePlacement(
    val tile: MapTile,
    val left: Double,
    val top: Double,
    val size: Double,
)

private fun visibleTiles(camera: MapCamera, viewport: IntSize, tileSize: Double): List<TilePlacement> {
    val zoom = camera.zoom.toInt()
    val columns = 1 shl zoom
    val size = camera.worldSize(tileSize) / columns
    val left = camera.center.x * columns - viewport.width / (2 * size)
    val top = camera.center.y * columns - viewport.height / (2 * size)
    val right = ceil(left + viewport.width / size).toInt()
    val bottom = ceil(top + viewport.height / size).toInt().coerceAtMost(columns)
    return buildList {
        for (y in floor(top).toInt().coerceAtLeast(0) until bottom) {
            for (x in floor(left).toInt() until right) {
                add(TilePlacement(MapTile(zoom, Math.floorMod(x, columns), y), (x - left) * size, (y - top) * size, size))
            }
        }
    }
}

private fun MapPoint.toScreen(camera: MapCamera, viewport: IntSize, worldSize: Double) = Offset(
    x = (wrappedDistance(x, camera.center.x) * worldSize + viewport.width / 2.0).toFloat(),
    y = ((y - camera.center.y) * worldSize + viewport.height / 2.0).toFloat(),
)
