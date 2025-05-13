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

package me.proton.core.drive.test.api

import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.photo.data.api.entity.AlbumPhotoListingDto
import me.proton.core.drive.photo.data.api.request.FavoriteRequest
import me.proton.core.drive.photo.data.api.request.TagRequest
import me.proton.core.drive.photo.data.api.response.AddToRemoveFromAlbumResponse
import me.proton.core.drive.photo.data.api.response.CreateAlbumResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumListingsResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumPhotoListingResponse
import me.proton.core.network.data.protonApi.ProtonErrorData
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path

fun MockWebServer.createAlbum(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/photos/volumes/{volumeID}/albums", block)
}

fun MockWebServer.createAlbum(albumDto: CreateAlbumResponse.AlbumDto) {
    createAlbum {
        jsonResponse {
            CreateAlbumResponse(
                code = ProtonApiCode.SUCCESS,
                album = albumDto,
            )
        }
    }
}

fun MockWebServer.updateAlbumName(block: RequestContext.() -> MockResponse) = routing {
    put("/drive/photos/volumes/{volumeID}/albums/{linkID}", block)
}

fun MockWebServer.updateAlbumName() {
    updateAlbumName {
        jsonResponse {
            CodeResponse(
                code = ProtonApiCode.SUCCESS.toInt(),
            )
        }
    }
}

fun MockWebServer.updateAlbumNameFailure(protonCode: Int, error: String, httpCode: Int = 422) {
    updateAlbumName {
        jsonResponse(status = httpCode) {
            ProtonErrorData(
                code = protonCode,
                error = error,
            )
        }
    }
}

fun MockWebServer.getAlbumListings(block: RequestContext.() -> MockResponse) = routing {
    get("/drive/photos/volumes/{volumeID}/albums", block)
}

fun MockWebServer.getAlbumListings(
    albumListingsDto: List<GetAlbumListingsResponse.AlbumListingsDto>,
) {
    getAlbumListings {
        jsonResponse {
            GetAlbumListingsResponse(
                code = ProtonApiCode.SUCCESS,
                anchorId = null,
                more = false,
                albums = albumListingsDto,
            )
        }
    }
}

fun MockWebServer.getAlbumSharedWithMeListings(block: RequestContext.() -> MockResponse) = routing {
    get("/drive/photos/albums/shared-with-me", block)
}

fun MockWebServer.getAlbumSharedWithMeListings(
    albumListingsDto: List<GetAlbumListingsResponse.AlbumListingsDto>,
) {
    getAlbumSharedWithMeListings {
        jsonResponse {
            GetAlbumListingsResponse(
                code = ProtonApiCode.SUCCESS,
                anchorId = null,
                more = false,
                albums = albumListingsDto,
            )
        }
    }
}

fun MockWebServer.getAlbumPhotoListings(block: RequestContext.() -> MockResponse) = routing {
    get("/drive/photos/volumes/{volumeID}/albums/{linkID}/children", block)
}

fun MockWebServer.getAlbumPhotoListings(albumPhotoListingsDto: List<AlbumPhotoListingDto>) {
    getAlbumPhotoListings {
        jsonResponse {
            GetAlbumPhotoListingResponse(
                code = ProtonApiCode.SUCCESS,
                photos = albumPhotoListingsDto,
                anchorId = null,
                hasMore = false,
            )
        }
    }
}

fun MockWebServer.addPhotosToAlbum(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/photos/volumes/{volumeID}/albums/{linkID}/add-multiple", block)
}

fun MockWebServer.addPhotosToAlbum(photoIds: List<String>) {
    addPhotosToAlbum {
        jsonResponse {
            AddToRemoveFromAlbumResponse(
                code = ProtonApiCode.SUCCESS,
                responses = photoIds.map { photoId ->
                    AddToRemoveFromAlbumResponse.Responses(
                        linkId = photoId,
                        response = AddToRemoveFromAlbumResponse.Response(
                            code = ProtonApiCode.SUCCESS,
                            error = null,
                            details = AddToRemoveFromAlbumResponse.Details(
                                newLinkId = photoId,
                            )
                        )
                    )
                }
            )
        }
    }
}

fun MockWebServer.addPhotosToAlbumFailure(
    failures: List<Triple<String, Int, String>>,
    httpCode: Int = 422,
) {
    addPhotosToAlbum {
        jsonResponse(status = httpCode) {
            AddToRemoveFromAlbumResponse(
                code = 1001,
                responses = failures.map { (photoId, protonCode, error) ->
                    AddToRemoveFromAlbumResponse.Responses(
                        linkId = photoId,
                        response = AddToRemoveFromAlbumResponse.Response(
                            code = protonCode.toLong(),
                            error = error,
                            details = null,
                        )
                    )
                }
            )
        }
    }
}

fun MockWebServer.postLinkFavorite(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/photos/volumes/{volumeID}/links/{linkID}/favorite", block)
}

fun MockWebServer.postLinkFavorite() {
    postLinkFavorite {
        jsonResponse {
            CodeResponse(
                code = ProtonApiCode.SUCCESS.toInt(),
            )
        }
    }
}

fun MockWebServer.postLinkTags(block: RequestContext.() -> MockResponse) = routing {
    post("/drive/photos/volumes/{volumeID}/links/{linkID}/tags", block)
}

fun MockWebServer.postLinkTags() {
    postLinkTags {
        jsonResponse {
            CodeResponse(
                code = ProtonApiCode.SUCCESS.toInt(),
            )
        }
    }
}


fun MockWebServer.deleteLinkTags(block: RequestContext.() -> MockResponse) = routing {
    delete("/drive/photos/volumes/{volumeID}/links/{linkID}/tags", block)
}

fun MockWebServer.deleteLinkTags() {
    deleteLinkTags {
        jsonResponse {
            CodeResponse(
                code = ProtonApiCode.SUCCESS.toInt(),
            )
        }
    }
}
