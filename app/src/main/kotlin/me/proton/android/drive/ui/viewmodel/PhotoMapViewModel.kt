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

package me.proton.android.drive.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import me.proton.android.drive.extension.thumbnailVO
import me.proton.android.drive.ui.screen.map.MapDateRange
import me.proton.android.drive.ui.screen.map.MapPoint
import me.proton.android.drive.ui.screen.map.PhotoMapTiles
import me.proton.android.drive.ui.screen.map.mapPoint
import me.proton.core.drive.base.data.api.Dto
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.firstErrorDomainOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ProtonPhotosClientProvider
import me.proton.core.drive.base.domain.usecase.Sha256
import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.crypto.domain.repository.DecryptedTextRepository
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.repository.DriveLinkRepository
import me.proton.core.drive.drivelink.photo.domain.entity.PhotoListingAnchor
import me.proton.core.drive.drivelink.photo.domain.entity.PhotoListingsSyncState
import me.proton.core.drive.drivelink.photo.domain.usecase.FetchAlbumPhotoListingPage
import me.proton.core.drive.drivelink.photo.domain.usecase.PhotoListingsLoader
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.file.base.domain.entity.XAttr
import me.proton.core.drive.file.base.domain.extension.toXAttr
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.revisionId
import me.proton.core.drive.link.domain.extension.revisionUid
import me.proton.core.drive.link.domain.extension.xAttrKey
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.photo.domain.usecase.GetPhotoListingIdsInRangeFlow
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailVOOrNull
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetActiveVolumes
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.ProtonPhotosClient
import me.proton.drive.sdk.ProtonSdkError.ErrorDomain
import me.proton.drive.sdk.entity.AdditionalMetadataProperty
import me.proton.drive.sdk.entity.FileNode
import me.proton.drive.sdk.entity.FileRevision
import me.proton.drive.sdk.entity.PhotoNode
import me.proton.drive.sdk.entity.ThumbnailType as SdkThumbnailType

internal data class LocatedMapPhoto(
    val id: FileId,
    val name: String?,
    val thumbnail: ThumbnailVO?,
    val point: MapPoint,
    val captureSeconds: Long,
)

internal data class PhotoMapContent(
    val photos: List<LocatedMapPhoto> = emptyList(),
    val dateBounds: MapDateRange? = null,
    val loading: Boolean = false,
    val hasError: Boolean = false,
    val withoutLocationCount: Int = 0,
    val locationsChecked: Int = 0,
    val locationsTotal: Int = 0,
)

internal data class PhotoMapState(val range: MapDateRange?, val content: PhotoMapContent)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class PhotoMapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val tiles: PhotoMapTiles,
    private val getPhotoShare: GetPhotoShare,
    private val getShare: GetShare,
    private val getActiveVolumes: GetActiveVolumes,
    private val photoListingsLoader: PhotoListingsLoader,
    private val getPhotoListingIdsInRangeFlow: GetPhotoListingIdsInRangeFlow,
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,
    private val fetchAlbumPhotoListingPage: FetchAlbumPhotoListingPage,
    private val driveLinkRepository: DriveLinkRepository,
    private val decryptedTextRepository: DecryptedTextRepository,
    private val locationCache: PhotoMapLocationCache,
    private val sha256: Sha256,
    private val photosClientProvider: ProtonPhotosClientProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    val albumId = savedStateHandle.get<String>(AlbumViewModel.ALBUM_ID)?.let { id ->
        AlbumId(ShareId(userId, requireNotNull(savedStateHandle[AlbumViewModel.SHARE_ID])), id)
    }
    val zone: ZoneId = ZoneId.systemDefault()
    val showPaths = savedStateHandle.getStateFlow(SHOW_PATHS, false)
    val datePickerOpen = savedStateHandle.getStateFlow(DATE_PICKER_OPEN, false)
    private val epochDays = savedStateHandle.getStateFlow<LongArray?>(DATE_RANGE, null)
    private val retry = MutableStateFlow(0)
    private val loading = PhotoMapContent(loading = true)
    private val initialRange = epochDays.value?.toRange()
    private var verifiedAlbumDates: AlbumDates? = null

    val state = combine(epochDays, retry) { days, _ -> days?.toRange() }
        .flatMapLatest { range ->
            val interval = range?.let { it.startSeconds(zone) until it.endExclusiveSeconds(zone) }
            var latest = loading
            (albumId?.let { albumPhotos(it, interval) } ?: libraryPhotos(interval))
                .onStart { emit(loading) }
                .onEach { latest = it }
                .catch { emit(latest.copy(loading = false, hasError = true)) }
                .map { PhotoMapState(range, it) }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, PhotoMapState(initialRange, loading))

    fun selectRange(range: MapDateRange?) {
        savedStateHandle[DATE_RANGE] = range?.toEpochDays()
    }

    fun setDatePickerOpen(open: Boolean) {
        savedStateHandle[DATE_PICKER_OPEN] = open
    }

    fun togglePaths() {
        savedStateHandle[SHOW_PATHS] = !showPaths.value
    }

    fun retry() {
        retry.update { it + 1 }
    }

    private fun libraryPhotos(interval: LongRange?) = getActiveVolumes(userId)
        .filterSuccessOrError()
        .map { result -> result.toResult().getOrThrow().any { it.type == Volume.Type.PHOTO } }
        .distinctUntilChanged()
        .flatMapLatest { hasVolume ->
            if (hasVolume) libraryPhotosWithVolume(interval) else flowOf(PhotoMapContent())
        }

    private fun libraryPhotosWithVolume(interval: LongRange?) = flow {
        val share = getPhotoShare(userId).toResult().getOrThrow()
        val key = PhotoListingAnchor.mainKey
        photoListingsLoader.load(key, userId, share.volumeId, share.id, tag = null)
        val photos = flow {
            val scan = PhotoLocationScan { emit(it) }
            getPhotoListingIdsInRangeFlow(
                userId = userId,
                volumeId = share.volumeId,
                tag = null,
                captureTimeFrom = TimestampS(interval?.first ?: Long.MIN_VALUE),
                captureTimeTo = TimestampS(interval?.let { it.last + 1 } ?: Long.MAX_VALUE),
            ).conflate().collect { ids ->
                scan.retain(ids.toSet())
                val missing = mutableListOf<PhotoListing>()
                libraryListingPages(share, interval).collect { page ->
                    missing += scan.locateCached(page)
                }
                scan.locateMissing(missing)
                emit(scan.completedContent())
            }
        }.distinctUntilChanged()
        emitAll(combine(photos, photoListingsLoader.stateFor(key), libraryDateBounds(share.volumeId)) { content, sync, bounds ->
            content.copy(
                dateBounds = bounds,
                loading = content.loading || (!content.hasError && content.photos.isEmpty() &&
                    (sync == PhotoListingsSyncState.Idle || sync == PhotoListingsSyncState.Loading)),
                hasError = content.hasError || sync is PhotoListingsSyncState.Error,
            )
        })
    }

    private fun libraryDateBounds(volumeId: VolumeId) = combine(
        photoRepository.getPhotoListingsFlow(userId, volumeId, 1, null, Direction.ASCENDING),
        photoRepository.getPhotoListingsFlow(userId, volumeId, 1, null, Direction.DESCENDING),
    ) { first, last ->
        listingDateBounds(first.getOrNull()?.firstOrNull(), last.getOrNull()?.firstOrNull())
    }.distinctUntilChanged()

    private fun libraryListingPages(share: Share, interval: LongRange?) = flow {
        // Both keys form an exclusive cursor. An empty ID excludes all photos at the end boundary.
        var last: PhotoListing? = interval?.let {
            PhotoListing.Volume(
                volumeId = share.volumeId,
                linkId = FileId(share.id, ""),
                captureTime = TimestampS(it.last + 1),
                nameHash = null,
                contentHash = null,
            )
        }
        while (true) {
            val page = photoRepository.getPhotoListings(
                userId = userId,
                volumeId = share.volumeId,
                count = LISTING_PAGE_SIZE,
                lastPhotoListing = last,
            )
            val matches = if (interval == null) page else page.takeWhile { it.captureTime.value >= interval.first }
            emit(matches)
            if (page.size < LISTING_PAGE_SIZE || matches.size < page.size) break
            last = page.last()
        }
    }

    private fun albumPhotos(albumId: AlbumId, interval: LongRange?) = flow {
        val share = getShare(albumId.shareId).toResult().getOrThrow()
        val scan = PhotoLocationScan { emit(it) }
        suspend fun albumEndpoint(direction: Direction) =
            albumRepository.getAlbumPhotoListings(
                userId = userId,
                volumeId = share.volumeId,
                albumId = albumId,
                fromIndex = 0,
                count = 1,
                sortingBy = PhotoListing.Album.SortBy.CAPTURED,
                sortingDirection = direction,
            ).firstOrNull()
        val verifiedDates = verifiedAlbumDates
        scan.dateBounds = if (verifiedDates == null) {
            listingDateBounds(albumEndpoint(Direction.ASCENDING), albumEndpoint(Direction.DESCENDING))
        } else {
            verifiedDates.range
        }
        val missingCached = mutableListOf<PhotoListing>()
        cachedAlbumListingPages(albumId, share.volumeId, interval).collect { page ->
            missingCached += scan.locateCached(page)
        }
        emit(scan.content(loading = true))
        val freshIds = mutableSetOf<FileId>()
        val missingFresh = mutableListOf<PhotoListing>()
        var freshBounds: MapDateRange? = null
        var fullAlbumLoaded = false
        val remote = coRunCatchingException {
            var anchor: String? = null
            val seenAnchors = mutableSetOf<String>()
            do {
                val page = fetchAlbumPhotoListingPage(
                    userId = userId,
                    volumeId = share.volumeId,
                    albumId = albumId,
                    pageKey = anchor,
                    sortingBy = PhotoListing.Album.SortBy.CAPTURED,
                    sortingDirection = Direction.DESCENDING,
                ).getOrThrow()
                page.saveAction()
                val photos = page.albumPhotoListings
                if (photos.isNotEmpty()) {
                    val bounds = MapDateRange.fromCaptureTimes(
                        photos.minOf { it.captureTime.value },
                        photos.maxOf { it.captureTime.value },
                        zone,
                    )
                    freshBounds = freshBounds?.including(bounds) ?: bounds
                    scan.dateBounds = scan.dateBounds?.including(bounds) ?: bounds
                }
                val listings = if (interval == null) photos else photos.filter { it.captureTime.value in interval }
                listings.mapTo(freshIds, PhotoListing::linkId)
                missingFresh += scan.locateCached(listings)
                scan.publishProgress()
                if (!page.hasMore) {
                    fullAlbumLoaded = true
                    break
                }
                if (interval != null && photos.any { it.captureTime.value < interval.first }) break
                anchor = requireNotNull(page.anchoreId)
                check(seenAnchors.add(anchor)) { "Album listing returned a repeated anchor" }
            } while (true)
        }
        if (remote.isSuccess) {
            scan.retain(freshIds)
            if (fullAlbumLoaded) {
                verifiedAlbumDates = AlbumDates(freshBounds)
                scan.dateBounds = freshBounds
            }
            scan.locateMissing(missingFresh)
        } else {
            scan.locateMissing(missingCached + missingFresh)
        }
        val content = scan.completedContent()
        emit(content.copy(hasError = content.hasError || remote.isFailure))
    }

    private fun cachedAlbumListingPages(
        albumId: AlbumId,
        volumeId: VolumeId,
        interval: LongRange?,
    ) = flow {
        var offset = 0
        do {
            val page = albumRepository.getAlbumPhotoListings(
                userId = userId,
                volumeId = volumeId,
                albumId = albumId,
                fromIndex = offset,
                count = LISTING_PAGE_SIZE,
                sortingBy = PhotoListing.Album.SortBy.CAPTURED,
                sortingDirection = Direction.DESCENDING,
            )
            emit(if (interval == null) page else page.filter { it.captureTime.value in interval })
            offset += page.size
        } while (page.size == LISTING_PAGE_SIZE &&
            (interval == null || page.none { it.captureTime.value < interval.first }))
    }

    private fun listingDateBounds(first: PhotoListing?, last: PhotoListing?): MapDateRange? {
        val earliest = first?.captureTime?.value ?: return null
        val latest = last?.captureTime?.value ?: return null
        return MapDateRange.fromCaptureTimes(earliest, latest, zone)
    }

    private data class AlbumDates(val range: MapDateRange?)

    private inner class PhotoLocationScan(private val publish: suspend (PhotoMapContent) -> Unit) {
        private val resolved = mutableSetOf<FileId>()
        private val photos = linkedMapOf<FileId, LocatedMapPhoto>()
        private val photosWithoutLocation = mutableSetOf<FileId>()
        private val failures = mutableSetOf<FileId>()
        private val progress = PhotoLocationProgress()
        private var recordsRequested = 0
        private var lastLoggedRequestCount: Int? = null
        private var lastUpdate: TimeSource.Monotonic.ValueTimeMark? = null
        var dateBounds: MapDateRange? = null

        fun retain(ids: Set<FileId>) {
            resolved.retainAll(ids)
            photos.keys.retainAll(ids)
            photosWithoutLocation.retainAll(ids)
            failures.retainAll(ids)
            progress.replaceListings(ids)
        }

        fun content(loading: Boolean) = PhotoMapContent(
            photos = photos.values.toList(),
            dateBounds = dateBounds,
            loading = loading,
            hasError = failures.isNotEmpty(),
            withoutLocationCount = photosWithoutLocation.size,
            locationsChecked = progress.checked,
            locationsTotal = progress.total,
        )

        fun completedContent(): PhotoMapContent {
            if (recordsRequested != lastLoggedRequestCount) {
                CoreLogger.d(LogTag.PHOTO, "Photos Map location records requested=$recordsRequested")
                lastLoggedRequestCount = recordsRequested
            }
            return content(loading = false)
        }

        suspend fun locateCached(listings: List<PhotoListing>): List<PhotoListing> {
            if (progress.includeListings(listings.map(PhotoListing::linkId))) publishProgress()
            listings.forEach { listing ->
                val photo = photos[listing.linkId]
                if (photo != null) {
                    val thumbnail = photo.thumbnail ?: listing.thumbnailVO
                    if (photo.captureSeconds != listing.captureTime.value || photo.thumbnail != thumbnail) {
                        photos[listing.linkId] = photo.copy(captureSeconds = listing.captureTime.value, thumbnail = thumbnail)
                    }
                }
            }
            val pending = listings.distinctBy(PhotoListing::linkId).filterNot { it.linkId in resolved }
            val needsLocalLookup = mutableListOf<PhotoListing>()
            val needsRevisionCheck = mutableMapOf<FileId, CachedSdkLocation>()
            for (listing in pending) {
                val entry = locationCache.get(listing, listing.activeRevisionId)
                if (entry != null && listing.activeRevisionId != null) {
                    recordLocation(listing, entry.location, entry.name, entry.thumbnail(listing))
                } else {
                    needsLocalLookup += listing
                    if (entry != null) needsRevisionCheck[listing.linkId] = entry
                }
            }
            if (needsLocalLookup.isEmpty()) return emptyList()
            val files = driveLinkRepository.getDriveLinks(needsLocalLookup.map(PhotoListing::linkId)).first()
                .filterIsInstance<DriveLink.File>().associateBy(DriveLink.File::id)
            for (listing in needsLocalLookup) {
                val file = files[listing.linkId]?.takeIf {
                    listing.activeRevisionId == null || listing.activeRevisionId == it.activeRevisionId
                }
                val entry = needsRevisionCheck[listing.linkId]
                if (entry != null && entry.canReuse(file?.activeRevisionId)) {
                    recordLocation(listing, entry.location, entry.name, entry.thumbnail(listing))
                    continue
                }
                val point = file?.let { cachedXAttr(it)?.location?.point() }
                if (file != null && point != null) {
                    recordLocation(
                        listing, PhotoLocation.Located(point), null, file.thumbnailVOOrNull(ThumbnailType.DEFAULT),
                    )
                }
            }
            return needsLocalLookup.filterNot { it.linkId in resolved }
        }

        suspend fun locateMissing(listings: List<PhotoListing>) {
            val pending = listings.distinctBy(PhotoListing::linkId).filterNot { it.linkId in resolved }
            if (pending.isEmpty()) return
            // Flush all available cached locations before any network request.
            publish(content(loading = true))
            val client = photosClientProvider.getOrCreate(userId).getOrElse {
                pending.mapTo(failures, PhotoListing::linkId)
                return
            }
            // Keep failures isolated to a page. The Photos SDK handles request batching.
            for (page in pending.asSequence().chunked(LOCATION_PAGE_SIZE)) {
                val result = readLocations(client, page)
                val unreturned = page.map(PhotoListing::linkId).filterNot(resolved::contains)
                failures += unreturned
                resolved += unreturned
                val sdkError = (result.exceptionOrNull() as? ProtonDriveSdkException)?.error
                if (sdkError.firstErrorDomainOrNull(ErrorDomain.Network) != null ||
                    sdkError.firstErrorDomainOrNull(ErrorDomain.Transport) != null
                ) {
                    pending.asSequence().map(PhotoListing::linkId).filterNotTo(failures, resolved::contains)
                    break
                }
            }
        }

        private suspend fun readLocations(client: ProtonPhotosClient, page: List<PhotoListing>): Result<Unit> {
            val entries = page.map { it.linkId.nodeUid(it.volumeId) to it }
            val listingsByNode = entries.associate { (uid, listing) -> uid.value to listing }
            val nodeUids = entries.map { (uid, _) -> uid }.distinctBy { it.value }
            return coRunCatchingException {
                client.enumerateNodes(nodeUids).onStart {
                    recordsRequested += nodeUids.size
                }.collect { node ->
                    val listing = listingsByNode[node.uid.value] ?: return@collect
                    val revision = when (node) {
                        is PhotoNode -> node.activeRevision
                        is FileNode -> node.activeRevision
                        else -> return@collect
                    }
                    val hasErrors = node.errors.isNotEmpty()
                    val location = sdkPhotoLocation(revision.claimedAdditionalMetadata, hasErrors)
                    val name = node.name.getOrNull()
                    val thumbnail = if (location is PhotoLocation.Located) revision.thumbnail() else null
                    if (!hasErrors && location != PhotoLocation.Unavailable) {
                        val entry = CachedSdkLocation(
                            revisionId = revision.uid.revisionId,
                            point = (location as? PhotoLocation.Located)?.point,
                            name = name.takeIf { location is PhotoLocation.Located },
                            hasThumbnail = thumbnail != null,
                        )
                        locationCache.put(listing, entry)
                    }
                    recordLocation(listing, location, name, thumbnail)
                }
            }
        }

        private suspend fun recordLocation(
            listing: PhotoListing,
            location: PhotoLocation,
            name: String?,
            thumbnail: ThumbnailVO?,
        ) {
            val id = listing.linkId
            if (!resolved.add(id)) return
            progress.recordChecked(id)
            failures -= id
            when (location) {
                is PhotoLocation.Located -> addPhoto(listing, location.point, name, thumbnail)
                PhotoLocation.Missing -> {
                    photosWithoutLocation += id
                    publishProgress()
                }
                PhotoLocation.Unavailable -> {
                    failures += id
                    publishProgress()
                }
            }
        }

        private suspend fun addPhoto(listing: PhotoListing, point: MapPoint, name: String?, thumbnail: ThumbnailVO?) {
            val firstPin = photos.isEmpty()
            photos[listing.linkId] = LocatedMapPhoto(
                listing.linkId, name, thumbnail ?: listing.thumbnailVO, point, listing.captureTime.value,
            )
            publishProgress(force = firstPin)
        }

        suspend fun publishProgress(force: Boolean = false) {
            if (force || lastUpdate?.elapsedNow()?.let { it < PROGRESS_INTERVAL } != true) {
                publish(content(loading = true))
                lastUpdate = TimeSource.Monotonic.markNow()
            }
        }
    }

    private suspend fun cachedXAttr(file: DriveLink.File): XAttr? {
        val ciphertext = file.xAttr ?: return null
        val hash = sha256(ciphertext, Sha256.OutputFormat.BASE_64).getOrNull()
        return decryptedTextRepository.getDecryptedText(userId, file.xAttrKey(hash))
            ?.text?.toXAttr()?.getOrNull()
    }

    private fun FileRevision.thumbnail(): ThumbnailVO? =
        if (thumbnails.any { it.type == SdkThumbnailType.THUMBNAIL }) {
            ThumbnailVO(userId = userId, revisionUid = uid, type = ThumbnailType.DEFAULT, volumeType = Volume.Type.PHOTO)
        } else {
            null
        }

    private fun LongArray.toRange() = MapDateRange(LocalDate.ofEpochDay(this[0]), LocalDate.ofEpochDay(this[1]))

    private fun MapDateRange.toEpochDays() = longArrayOf(start.toEpochDay(), end.toEpochDay())

    private companion object {
        const val DATE_RANGE = "photoMapDateRange"
        const val DATE_PICKER_OPEN = "photoMapDatePickerOpen"
        const val SHOW_PATHS = "photoMapShowPaths"
        const val LISTING_PAGE_SIZE = 200
        const val LOCATION_PAGE_SIZE = 50
        val PROGRESS_INTERVAL = 200.milliseconds
    }
}

@Serializable
internal data class CachedSdkLocation(
    val revisionId: String,
    val point: MapPoint? = null,
    val name: String? = null,
    val hasThumbnail: Boolean = false,
) {
    val location: PhotoLocation get() = point?.let(PhotoLocation::Located) ?: PhotoLocation.Missing

    fun canReuse(currentRevisionId: String?): Boolean =
        currentRevisionId == null || currentRevisionId == revisionId

    fun thumbnail(listing: PhotoListing): ThumbnailVO? = if (hasThumbnail) {
        ThumbnailVO(
            userId = listing.linkId.shareId.userId,
            revisionUid = listing.linkId.revisionUid(listing.volumeId, revisionId),
            type = ThumbnailType.DEFAULT,
            volumeType = Volume.Type.PHOTO,
        )
    } else {
        null
    }
}

internal class PhotoLocationProgress {
    private val listings = mutableSetOf<FileId>()
    private val examined = mutableSetOf<FileId>()

    val total: Int get() = listings.size
    val checked: Int get() = examined.size

    fun includeListings(ids: List<FileId>): Boolean = listings.addAll(ids)

    fun replaceListings(ids: Set<FileId>) {
        listings.clear()
        listings.addAll(ids)
        examined.retainAll(ids)
    }

    fun recordChecked(id: FileId) {
        if (id in listings) examined.add(id)
    }
}

private val locationJson = Json { ignoreUnknownKeys = true }

internal sealed interface PhotoLocation {
    data class Located(val point: MapPoint) : PhotoLocation
    data object Missing : PhotoLocation
    data object Unavailable : PhotoLocation
}

internal fun sdkPhotoLocation(metadata: List<AdditionalMetadataProperty>?, hasErrors: Boolean): PhotoLocation {
    val location = metadata?.firstOrNull { it.name == Dto.LOCATION }
        ?: return if (hasErrors) PhotoLocation.Unavailable else PhotoLocation.Missing
    val point = coRunCatchingException { locationJson.decodeFromJsonElement<XAttr.Location>(location.value) }
        .getOrNull()?.point()
    return point?.let(PhotoLocation::Located) ?: PhotoLocation.Unavailable
}

private fun XAttr.Location.point() = mapPoint(latitude, longitude)
