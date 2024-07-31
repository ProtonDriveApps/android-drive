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
package me.proton.core.drive.files.preview.presentation.component

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.isNightMode
import me.proton.core.drive.base.presentation.extension.conditional

@Composable
@OptIn(UnstableApi::class)
fun MediaPreview(
    uri: Uri,
    isFullScreen: Boolean,
    modifier: Modifier = Modifier,
    play: Boolean = true,
    mediaControllerVisibility: (Boolean) -> Unit = {}
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isFullScreen && isNightMode().not()) {
            MaterialTheme.colors.onBackground
        } else {
            MaterialTheme.colors.background
        }
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .conditional(!isFullScreen) {
                navigationBarsPadding()
            },
        contentAlignment = Alignment.Center
    ) {
        val localContext = LocalContext.current
        val player = remember(uri) { ExoPlayer.Builder(localContext).build() }
        DisposableEffect(uri) {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            onDispose {
                player.release()
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    player.playWhenReady = false
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            }
        }
        LaunchedEffect(play) {
            player.playWhenReady = play
        }
        AndroidView(factory = { context ->
            PlayerView(context).also { playerView ->
                playerView.player = player
                playerView.hideController()
                playerView.setControllerVisibilityListener(
                    PlayerView.ControllerVisibilityListener {
                        mediaControllerVisibility(playerView.isControllerFullyVisible)
                    }
                )
            }
        })
    }
}

@Preview
@Composable
private fun PreviewMediaPreview() {
    ProtonTheme {
        MediaPreview(uri = Uri.parse(""), isFullScreen = true)
    }
}
