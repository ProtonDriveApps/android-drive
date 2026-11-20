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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.proton.android.drive.ui.viewmodel.PhotoMapCenterPickerState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.i18n.R as I18N

@Composable
internal fun PhotoMapCenterPicker(
    picker: PhotoMapCenterPickerState,
    tileStore: PhotoMapTiles,
    onMove: ((MapCamera) -> MapCamera) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val tileSize = with(LocalDensity.current) { MapTileSize.toPx().toDouble() }
    val mapHeight = minOf(LocalConfiguration.current.screenHeightDp.dp * 0.42f, 340.dp)
    val thumbnailRequests = remember { mutableMapOf<Any, ThumbnailVO>() }
    val interactionColor = ProtonTheme.colors.interactionNorm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(I18N.string.photos_map_default_centre)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(I18N.string.photos_map_choose_default_centre))
                Box(
                    Modifier.fillMaxWidth().height(mapHeight).clip(RoundedCornerShape(8.dp))
                        .onSizeChanged { viewport = it },
                ) {
                    PhotoMapViewport(
                        camera = picker.camera,
                        viewport = viewport,
                        photos = emptyList(),
                        paths = emptyList(),
                        thumbnailRequests = thumbnailRequests,
                        tileStore = tileStore,
                        onTransform = { pan, anchor, zoom ->
                            onMove {
                                it.move(
                                    pan.x.toDouble(), pan.y.toDouble(), anchor.x.toDouble(), anchor.y.toDouble(),
                                    zoom, tileSize,
                                )
                            }
                        },
                        onSelectPin = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                    Canvas(Modifier.align(Alignment.Center).size(28.dp)) {
                        val radius = 10.dp.toPx()
                        drawCircle(Color.Black, radius, style = Stroke(4.dp.toPx()))
                        drawCircle(Color.White, radius, style = Stroke(2.dp.toPx()))
                        drawCircle(Color.White, radius = 4.dp.toPx())
                        drawCircle(interactionColor, radius = 2.dp.toPx())
                    }
                }
                if (picker.saveFailed) {
                    Text(
                        text = stringResource(I18N.string.photos_map_default_centre_save_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !picker.saving) {
                Text(stringResource(I18N.string.common_cancel_action))
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !picker.saving) {
                Text(stringResource(I18N.string.common_save_action))
            }
        },
    )
}
