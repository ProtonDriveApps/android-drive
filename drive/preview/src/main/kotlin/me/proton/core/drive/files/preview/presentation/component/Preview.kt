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
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import me.proton.core.compose.component.ErrorPadding
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonErrorMessageWithAction
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.overline
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.requireIsInstance
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.base.presentation.component.LinearProgressIndicator
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.debugOnly
import me.proton.core.drive.base.presentation.extension.iconResId
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.files.preview.presentation.component.event.PreviewViewEvent
import me.proton.core.drive.files.preview.presentation.component.state.ContentState
import me.proton.core.drive.files.preview.presentation.component.state.PreviewContentState
import me.proton.core.drive.files.preview.presentation.component.state.PreviewViewState
import me.proton.core.util.kotlin.exhaustive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod")
fun Preview(
    viewState: PreviewViewState,
    viewEvent: PreviewViewEvent,
    modifier: Modifier = Modifier,
    onPageChanged: FlowCollector<Int>? = null,
) {
    val pagerState = key(viewState.items) {
        rememberPagerState(initialPage = viewState.currentIndex) {
            viewState.items.size
        }
    }
    val userScrollEnabled = remember { mutableStateOf(true) }
    val isFullScreen by rememberFlowWithLifecycle(viewState.isFullscreen).collectAsState(false)

    onPageChanged?.let {
        LaunchedEffect(pagerState) {
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
            userScrollEnabled = userScrollEnabled.value,
            key = { page ->
                takeIf { page in viewState.items.indices }
                    ?.let { viewState.items[page].key }
                    ?: getDefaultLazyLayoutKey(page)
            },
            modifier = Modifier.testTag(PreviewComponentTestTag.pager)
        ) { page ->
            takeIf { page in viewState.items.indices }?.let {
                PreviewContent(
                    viewState.items[page],
                    isFullScreen,
                    viewState.host,
                    viewState.appVersionHeader,
                    viewEvent,
                    with(LocalDensity.current) { topBarHeightAnimated.toDp() },
                    isFocused = pagerState.currentPage == page,
                    userScrollEnabled,
                )
            }
        }
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { fullHeight: Int -> -fullHeight }),
            exit = slideOutVertically(targetOffsetY = { fullHeight: Int -> -fullHeight }),
        ) {
            val item = viewState.items.getOrNull(pagerState.currentPage)
            TopAppBar(
                modifier = Modifier
                    .background(appBarGradient)
                    .statusBarsPadding()
                    .conditional(isLandscape) {
                        navigationBarsPadding()
                    }
                    .onSizeChanged { size ->
                        topBarHeight = size.height
                    }
                    .testTag(PreviewComponentTestTag.screen),
                navigationIcon = painterResource(id = viewState.navigationIconResId),
                onNavigationIcon = { viewEvent.onTopAppBarNavigation() },
                title = item?.title ?: "",
                isTitleEncrypted = item?.isTitleEncrypted ?: false,
                backgroundColor = Color.Transparent,
                actions = {
                    ActionButton(
                        icon = CorePresentation.drawable.ic_proton_three_dots_vertical,
                        contentDescription = I18N.string.content_description_more_options,
                        onClick = viewEvent.onMoreOptions,
                    )
                },
            )
        }
    }
}

private val appBarGradient: Brush @Composable get() = Brush.verticalGradient(
    colors = listOf(0.8f, 0.7f, 0.6f, 0.5f, 0f).map { alpha ->
        ProtonTheme.colors.backgroundNorm.copy(alpha = alpha)
    }
)

@Composable
fun PreviewContent(
    item: PreviewViewState.Item,
    isFullScreen: Boolean,
    host: String,
    appVersionHeader: String,
    viewEvent: PreviewViewEvent,
    topBarHeight: Dp,
    isFocused: Boolean,
    userScrollEnabled: MutableState<Boolean>,
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
            item.title,
            host,
            appVersionHeader,
            item.category.toComposable(),
            isFullScreen,
            viewEvent,
            topBarHeight,
            isFocused,
            userScrollEnabled
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
                progress?.value?.value to stringResource(id = I18N.string.preview_downloading_state).debugOnly()
            }
            is ContentState.Decrypting -> null to stringResource(id = I18N.string.preview_processing_state).debugOnly()
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
    title: String,
    host: String,
    appVersionHeader: String,
    previewComposable: PreviewComposable,
    isFullScreen: Boolean,
    viewEvent: PreviewViewEvent,
    topBarHeight: Dp,
    isFocused: Boolean,
    userScrollEnabled: MutableState<Boolean>,
    transformationState : TransformationState = rememberTransformationState(),
    onDoubleTap : () -> Unit = { transformationState.scale = 2F },
    modifier: Modifier = Modifier,
) {
    val dragEnable = transformationState.hasScale()

    if (isFocused) {
        DisposableEffect(dragEnable) {
            userScrollEnabled.value = when {
                previewComposable == PreviewComposable.ProtonDoc -> false
                else -> !dragEnable
            }
            onDispose {
                userScrollEnabled.value = previewComposable != PreviewComposable.ProtonDoc
            }
        }
    }

    val pointerInputModifier = modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { viewEvent.onSingleTap() },
                onDoubleTap = {
                    if (transformationState.scale == 1F) {
                        onDoubleTap()
                    } else {
                        transformationState.scale = 1F
                        transformationState.offset = Offset.Zero
                    }
                }
            )
        }

    when (previewComposable) {
        PreviewComposable.Image -> ImagePreview(
            modifier = pointerInputModifier,
            source = contentState.source,
            transformationState = transformationState,
            isFullScreen = isFullScreen,
            onRenderFailed = viewEvent.onRenderFailed,
        )
        PreviewComposable.Sound,
        PreviewComposable.Video -> MediaPreview(
            modifier = pointerInputModifier.testTag(PreviewComponentTestTag.mediaPreview),
            uri = requireIsInstance(contentState.source),
            isFullScreen = isFullScreen,
            play = isFocused,
            mediaControllerVisibility = viewEvent.mediaControllerVisibility
        )
        PreviewComposable.Pdf -> PdfPreview(
            uri = requireIsInstance(contentState.source),
            modifier = pointerInputModifier.padding(top = topBarHeight),
            transformationState = transformationState,
            onRenderFailed = viewEvent.onRenderFailed,
        )
        PreviewComposable.Text -> TextPreview(
            uri = requireIsInstance(contentState.source),
            modifier = pointerInputModifier.padding(top = topBarHeight),
            onRenderFailed = viewEvent.onRenderFailed,
        )
        PreviewComposable.ProtonDoc -> when (contentState.source) {
            is String -> ProtonDocumentPreview(
                uriString = requireIsInstance(contentState.source),
                title = title,
                host = host,
                appVersionHeader = appVersionHeader,
                modifier = pointerInputModifier.padding(top = topBarHeight),
                onDownloadResult = viewEvent.onProtonDocsDownloadResult,
                onShowFileChooser = viewEvent.onProtonDocsShowFileChooser,
            )
            is Uri -> ProtonDocumentPreview(
                modifier = pointerInputModifier.padding(top = topBarHeight),
                onOpenInBrowser = viewEvent.onOpenInBrowser,
            )
            else -> Unit
        }
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
    onMessage: (() -> Unit)? = null,
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
                modifier = Modifier
                    .size(PlaceholderSize)
                    .testTag(PreviewComponentTestTag.placeholder),
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
                if (onMessage == null) {
                    Text(
                        text = message,
                        style = ProtonTheme.typography.overline(),
                    )
                } else {
                    ProtonSolidButton(
                        onClick = onMessage,
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = DefaultSpacing)
                        )
                    }
                }
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
            message = stringResource(id = I18N.string.title_not_found),
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
        Surface {
            Preview(
                viewState = PreviewViewState(
                    navigationIconResId = CorePresentation.drawable.ic_proton_arrow_back,
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
                    ),
                    host = "proton.me",
                    appVersionHeader = "android-drive@1.0.0",
                ),
                viewEvent = object : PreviewViewEvent {
                    override val onTopAppBarNavigation: () -> Unit = {}
                    override val onMoreOptions: () -> Unit = {}
                    override val onSingleTap: () -> Unit = {}
                    override val onRenderFailed: (Throwable, Any) -> Unit = { _, _ -> }
                    override val mediaControllerVisibility: (Boolean) -> Unit = {}
                    override val onOpenInBrowser: () -> Unit = {}
                    override val onProtonDocsDownloadResult: (Result<String>) -> Unit = {}
                    override val onProtonDocsShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean = { _, _ -> false }
                },
            )
        }
    }
}

@Preview
@Composable
@Suppress("MagicNumber")
fun PreviewPreviewDownloading() {
    ProtonTheme {
        Surface {
            PreviewDownloadingAndDecrypting(
                state = ContentState.Downloading(flowOf(Percentage(50))),
                fileTypeCategory = FileTypeCategory.Zip
            )
        }
    }
}

@Preview
@Composable
fun PreviewPreviewNotFound() {
    ProtonTheme {
        Surface {
            PreviewNotFound(deferDuration = 0.seconds)
        }
    }
}

@Preview
@Composable
fun PreviewPreviewError() {
    ProtonTheme {
        Surface {
            PreviewError(
                message = "Error message",
                fileTypeCategory = FileTypeCategory.Keynote,
            )
        }
    }
}

fun FileTypeCategory.toComposable(): PreviewComposable = when (this) {
    FileTypeCategory.Audio -> PreviewComposable.Sound
    FileTypeCategory.Image -> PreviewComposable.Image
    FileTypeCategory.Pdf -> PreviewComposable.Pdf
    FileTypeCategory.Text -> PreviewComposable.Text
    FileTypeCategory.Video -> PreviewComposable.Video
    FileTypeCategory.ProtonDoc -> PreviewComposable.ProtonDoc
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
    ProtonDoc,
    Text,
    Unknown,
}

object PreviewComponentTestTag {
    const val screen = "preview screen"
    const val placeholder = "preview placeholder"
    const val pager = "preview pager"
    const val mediaPreview = "preview media"
}
