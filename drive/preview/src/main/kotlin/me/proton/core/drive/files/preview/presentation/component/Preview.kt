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

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import me.proton.core.compose.component.ErrorPadding
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonErrorMessageWithAction
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.overline
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.base.presentation.component.LinearProgressIndicator
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.entity.FileTypeCategory
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.debugOnly
import me.proton.core.drive.files.preview.R
import me.proton.core.drive.files.preview.presentation.component.event.PreviewViewEvent
import me.proton.core.drive.files.preview.presentation.component.state.ContentState
import me.proton.core.drive.files.preview.presentation.component.state.PreviewContentState
import me.proton.core.drive.files.preview.presentation.component.state.PreviewViewState
import me.proton.core.drive.files.preview.presentation.component.state.ZoomEffect
import me.proton.core.util.kotlin.exhaustive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@Composable
@OptIn(ExperimentalPagerApi::class)
@Suppress("LongMethod")
fun Preview(
    viewState: PreviewViewState,
    viewEvent: PreviewViewEvent,
    zoomEffect: Flow<ZoomEffect>,
    modifier: Modifier = Modifier,
    onPageChanged: FlowCollector<Int>? = null,
) {
    val detectTapGestureModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { viewEvent.onSingleTap() },
                onDoubleTap = { viewEvent.onDoubleTap() }
            )
        }
    val pagerState = rememberPagerState(initialPage = viewState.currentIndex)

    val isFullScreen by rememberFlowWithLifecycle(viewState.isFullscreen).collectAsState(false)

    onPageChanged?.let {
        LaunchedEffect(pagerState, onPageChanged) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect(onPageChanged)
        }
    }

    var topBarHeight by remember { mutableStateOf(0) }
    val topBarHeightAnimated by animateIntAsState(
        if (isFullScreen) {
            0
        } else {
            topBarHeight + WindowInsets.statusBars.getTop(LocalDensity.current)
        }
    )
    Box(modifier) {
        HorizontalPager(
            state = pagerState,
            count = viewState.items.size,
            key = { page -> viewState.items[page].key }
        ) { page ->
            PreviewContent(
                viewState.items[page],
                isFullScreen,
                viewEvent,
                zoomEffect,
                with(LocalDensity.current) { topBarHeightAnimated.toDp() },
                isFocused = pagerState.currentPage == page,
                detectTapGestureModifier,
            )
        }
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { fullHeight: Int -> -fullHeight }),
            exit = slideOutVertically(targetOffsetY = { fullHeight: Int -> -fullHeight }),
        ) {
            val item = viewState.items.getOrNull(viewState.currentIndex)
            TopAppBar(
                modifier = Modifier
                    .background(appBarGradient)
                    .statusBarsPadding()
                    .conditional(isLandscape) {
                        navigationBarsPadding()
                    }
                    .onSizeChanged { size ->
                        topBarHeight = size.height
                    }.testTag(PreviewComponentTestTag.screen),
                navigationIcon = painterResource(id = viewState.navigationIconResId),
                onNavigationIcon = { viewEvent.onTopAppBarNavigation() },
                title = item?.title ?: "",
                isTitleEncrypted = item?.isTitleEncrypted ?: false,
                backgroundColor = Color.Transparent,
                actions = {
                    ActionButton(
                        icon = CorePresentation.drawable.ic_proton_three_dots_vertical,
                        contentDescription = BasePresentation.string.content_description_more_options,
                        onClick = viewEvent.onMoreOptions,
                    )
                },
            )
        }
    }
}

private val isLandscape: Boolean @Composable get() =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

private val appBarGradient: Brush @Composable get() = Brush.verticalGradient(
    colors = listOf(0.8f, 0.7f, 0.6f, 0.5f, 0f).map { alpha ->
        ProtonTheme.colors.backgroundNorm.copy(alpha = alpha)
    }
)

@Composable
fun PreviewContent(
    item: PreviewViewState.Item,
    isFullScreen: Boolean,
    viewEvent: PreviewViewEvent,
    zoomEffect: Flow<ZoomEffect>,
    topBarHeight: Dp,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentState by rememberFlowWithLifecycle(item.contentState).collectAsState(
        ContentState.Downloading(null)
    )
    @Suppress("UnnecessaryVariable")
    when (val contentStateLocal = contentState) {
        is ContentState.Downloading,
        ContentState.Decrypting -> Deferred {
            PreviewDownloadingAndDecrypting(
                modifier = Modifier.padding(top = topBarHeight),
                state = contentStateLocal,
                fileTypeCategory = item.category,
            )
        }
        is ContentState.Available -> PreviewContentAvailable(
            contentStateLocal,
            item.category.toComposable(),
            isFullScreen,
            viewEvent,
            zoomEffect,
            topBarHeight,
            isFocused,
            modifier,
        )
        ContentState.NotFound -> PreviewNotFound()
        is ContentState.Error -> {
            when (contentStateLocal) {
                is ContentState.Error.Retryable -> PreviewErrorWithAction(
                    message = stringResource(id = contentStateLocal.messageResId),
                    action = stringResource(id = contentStateLocal.actionResId),
                    onAction = contentStateLocal.action,
                    fileTypeCategory = item.category,
                )
                is ContentState.Error.NonRetryable -> PreviewError(
                    message = contentStateLocal.message
                        ?: stringResource(id = contentStateLocal.messageResId),
                    fileTypeCategory = item.category,
                )
            }
        }
    }.exhaustive
}

@Composable
fun PreviewDownloadingAndDecrypting(
    state: ContentState,
    fileTypeCategory: FileTypeCategory,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val (progress, placeholderMessage) = when (state) {
            is ContentState.Downloading -> {
                val progress = state.progress?.let {
                    rememberFlowWithLifecycle(state.progress).collectAsState(null)
                }
                progress?.value?.value to stringResource(id = R.string.preview_downloading_state).debugOnly()
            }
            is ContentState.Decrypting -> null to stringResource(id = R.string.preview_processing_state).debugOnly()
            else -> throw IllegalStateException("Allowed states are Downloading or Decrypting but was $state")
        }
        LinearProgressIndicator(
            progress = progress,
            height = ProgressHeight,
        )
        PreviewPlaceholder(
            fileTypeCategory = fileTypeCategory,
            modifier = Modifier.navigationBarsPadding(),
            message = placeholderMessage,
        )
    }
}

@Composable
@Suppress("LongParameterList")
fun PreviewContentAvailable(
    contentState: ContentState.Available,
    previewComposable: PreviewComposable,
    isFullScreen: Boolean,
    viewEvent: PreviewViewEvent,
    zoomEffect: Flow<ZoomEffect>,
    topBarHeight: Dp,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    when (previewComposable) {
        PreviewComposable.Image -> ImagePreview(
            modifier = modifier,
            uri = contentState.uri,
            zoomEffect = zoomEffect,
            isFullScreen = isFullScreen,
        )
        PreviewComposable.Sound,
        PreviewComposable.Video -> MediaPreview(
            modifier = modifier,
            uri = contentState.uri,
            isFullScreen = isFullScreen,
            play = isFocused,
            mediaControllerVisibility = viewEvent.mediaControllerVisibility
        )
        PreviewComposable.Pdf -> PdfPreview(
            uri = contentState.uri,
            modifier = modifier.padding(top = topBarHeight),
            zoomEffect = zoomEffect,
            onRenderFailed = viewEvent.onRenderFailed,
        )
        PreviewComposable.Text -> TextPreview(
            uri = contentState.uri,
            modifier = modifier.padding(top = topBarHeight),
            onRenderFailed = viewEvent.onRenderFailed,
        )
        PreviewComposable.Unknown -> UnknownPreview()
    }.exhaustive
}

@Composable
fun PreviewError(
    message: String,
    fileTypeCategory: FileTypeCategory,
    modifier: Modifier = Modifier,
) {
    PreviewError(
        fileTypeCategory = fileTypeCategory,
        modifier = modifier.navigationBarsPadding(),
    ) {
        ProtonErrorMessage(errorMessage = message)
    }
}

@Composable
fun PreviewErrorWithAction(
    message: String,
    action: String,
    onAction: () -> Unit,
    fileTypeCategory: FileTypeCategory,
    modifier: Modifier = Modifier,
) {
    PreviewError(
        fileTypeCategory = fileTypeCategory,
        modifier = modifier.navigationBarsPadding(),
    ) {
        ProtonErrorMessageWithAction(
            errorMessage = message,
            action = action,
            onAction = onAction,
        )
    }
}

@Composable
internal fun PreviewError(
    fileTypeCategory: FileTypeCategory,
    modifier: Modifier = Modifier,
    error: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(ErrorPadding),
        ) {
            error()
        }
        PreviewPlaceholder(fileTypeCategory = fileTypeCategory)
    }
}

@Composable
fun PreviewPlaceholder(
    fileTypeCategory: FileTypeCategory,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center),
        ) {
            Image(
                modifier = Modifier.size(PlaceholderSize),
                painter = painterResource(id = fileTypeCategory.iconResId),
                contentDescription = null,
            )
        }
        if (message != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PlaceholderMessagePadding),
            ) {
                Text(
                    text = message,
                    style = ProtonTheme.typography.overline(),
                )
            }
        }
    }
}

@Composable
fun PreviewNotFound(
    modifier: Modifier = Modifier,
    deferDuration: Duration = 1.seconds,
) {
    var showPreviewNotFound by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(deferDuration)
        showPreviewNotFound = true
    }
    if (showPreviewNotFound) {
        PreviewError(
            message = stringResource(id = BasePresentation.string.title_not_found),
            fileTypeCategory = FileTypeCategory.Unknown,
            modifier = modifier.navigationBarsPadding(),
        )
    }
}

private val PlaceholderSize = 96.dp
private val PlaceholderMessagePadding = 16.dp
private val ProgressHeight = 2.dp


@Preview
@Composable
fun PreviewPreviewLoadingState() {
    ProtonTheme {
        Preview(
            viewState = PreviewViewState(
                navigationIconResId = 0,
                isFullscreen = flowOf(true),
                currentIndex = 0,
                previewContentState = PreviewContentState.Content,
                items = listOf(
                    PreviewViewState.Item(
                        key = "key",
                        title = "Title",
                        category = FileTypeCategory.Image,
                        contentState = emptyFlow(),
                    )
                )
            ),
            viewEvent = object : PreviewViewEvent {
                override val onTopAppBarNavigation: () -> Unit = {}
                override val onMoreOptions: () -> Unit = {}
                override val onSingleTap: () -> Unit = {}
                override val onDoubleTap: () -> Unit = {}
                override val onRenderFailed: (Throwable) -> Unit = {}
                override val mediaControllerVisibility: (Boolean) -> Unit = {}
            },
            zoomEffect = emptyFlow(),
        )
    }
}

@Preview
@Composable
@Suppress("MagicNumber")
fun PreviewPreviewDownloading() {
    ProtonTheme {
        PreviewDownloadingAndDecrypting(
            state = ContentState.Downloading(flowOf(Percentage(50))),
            fileTypeCategory = FileTypeCategory.Zip
        )
    }
}

@Preview
@Composable
fun PreviewPreviewNotFound() {
    ProtonTheme {
        PreviewNotFound(deferDuration = 0.seconds)
    }
}

@Preview
@Composable
fun PreviewPreviewError() {
    ProtonTheme {
        PreviewError(
            message = "Error message",
            fileTypeCategory = FileTypeCategory.Keynote,
        )
    }
}

fun FileTypeCategory.toComposable(): PreviewComposable = when (this) {
    FileTypeCategory.Audio -> PreviewComposable.Sound
    FileTypeCategory.Image -> PreviewComposable.Image
    FileTypeCategory.Pdf -> PreviewComposable.Pdf
    FileTypeCategory.Text -> PreviewComposable.Text
    FileTypeCategory.Video -> PreviewComposable.Video
    FileTypeCategory.Calendar,
    FileTypeCategory.Doc,
    FileTypeCategory.Keynote,
    FileTypeCategory.Numbers,
    FileTypeCategory.Pages,
    FileTypeCategory.Ppt,
    FileTypeCategory.TrustedKey,
    FileTypeCategory.Unknown,
    FileTypeCategory.Xls,
    FileTypeCategory.Xml,
    FileTypeCategory.Zip,
    -> PreviewComposable.Unknown
}

/**
 * This class helps ensuring we have only one place which defines if a preview is
 * supported or not
 */
enum class PreviewComposable {
    Image,
    Sound,
    Video,
    Pdf,
    Text,
    Unknown,
}

object PreviewComponentTestTag {
    const val screen = "preview screen"
}
