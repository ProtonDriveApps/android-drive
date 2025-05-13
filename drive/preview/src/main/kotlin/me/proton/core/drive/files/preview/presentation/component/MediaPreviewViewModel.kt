/*
 * Copyright (c) 2025 Proton AG.
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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import javax.inject.Inject

@HiltViewModel
class MediaPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private var playerPair: Pair<Uri, ExoPlayer>? = null

    fun getPlayer(uri: Uri): ExoPlayer =
        playerPair
            ?.takeIf { uri == it.first }
            ?.let { (_, player) ->
                player
            }
            ?: let {
                playerPair?.second?.let { player -> releasePlayer(player) }
                ExoPlayer.Builder(appContext).build().also { player ->
                    preparePlayer(player, uri)
                    playerPair = uri to player
                }
            }

    private fun preparePlayer(player: ExoPlayer, uri: Uri) {
        viewModelScope.launch {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        }
    }

    private fun releasePlayer(player: ExoPlayer) {
        viewModelScope.launch {
            player.release()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerPair?.second?.let { player ->
            releasePlayer(player)
            playerPair = null
        }
    }
}
