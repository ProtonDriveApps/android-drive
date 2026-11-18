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

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.proton.android.drive.BuildConfig
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal data class MapTile(val zoom: Int, val x: Int, val y: Int)

@Singleton
internal class PhotoMapTiles @Inject constructor(@ApplicationContext context: Context) {
    private val baseUrl = "https://tile.openstreetmap.org/".toHttpUrl()
    private val userAgent = "${BuildConfig.APP_VERSION_HEADER} (+https://proton.me/drive)"
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        // Retain downloaded tiles on disk. OkHttp still checks freshness and revalidates on revisit.
        .cache(Cache(File(context.noBackupFilesDir, "photo-map-tiles/http"), Long.MAX_VALUE))
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            when {
                // OpenStreetMap may return a policy error image with status 200.
                response.header("x-blocked") != null ->
                    response.newBuilder().header("Cache-Control", "no-store").build()
                response.isSuccessful && response.header("Cache-Control") == null && response.header("Expires") == null ->
                    response.newBuilder().header("Cache-Control", "max-age=604800").build()
                else -> response
            }
        }
        .build()

    suspend fun load(tile: MapTile): ImageBitmap? = withContext(Dispatchers.IO) {
        val url = baseUrl.newBuilder()
            .addPathSegments("${tile.zoom}/${tile.x}/${tile.y}.png")
            .build()
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        val bytes = client.newCall(request).readTile() ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..512 || bounds.outHeight !in 1..512) return@withContext null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }

    private suspend fun Call.readTile(): ByteArray? = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bytes = try {
                    response.use {
                        val source = it.body?.source()
                        if (it.isSuccessful && source != null && !source.request(MAX_TILE_BYTES + 1)) {
                            source.readByteArray()
                        } else {
                            null
                        }
                    }
                } catch (_: IOException) {
                    null
                }
                continuation.resume(bytes)
            }
        })
    }

    private companion object {
        const val MAX_TILE_BYTES = 1024L * 1024
    }
}
