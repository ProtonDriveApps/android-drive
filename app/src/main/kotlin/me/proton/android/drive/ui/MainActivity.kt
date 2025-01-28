/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.deepLinkBaseUrl
import me.proton.android.drive.lock.data.provider.BiometricPromptProvider
import me.proton.android.drive.lock.domain.manager.AppLockManager
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.provider.ActivityLauncher
import me.proton.android.drive.ui.navigation.AppNavGraph
import me.proton.android.drive.ui.provider.LocalSnackbarPadding
import me.proton.android.drive.ui.provider.ProvideLocalSnackbarPadding
import me.proton.android.drive.ui.viewmodel.AccountViewModel
import me.proton.android.drive.ui.viewmodel.BugReportViewModel
import me.proton.android.drive.ui.viewmodel.PlansViewModel
import me.proton.android.drive.usecase.GetDefaultEnabledDynamicHomeTab
import me.proton.android.drive.usecase.ProcessIntent
import me.proton.android.drive.usecase.ShowRatingBooster
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.ListenToBroadcastMessages
import me.proton.core.drive.messagequeue.domain.ActionProvider
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.thumbnail.presentation.coil.ThumbnailEnabled
import me.proton.core.notification.presentation.deeplink.DeeplinkManager
import me.proton.core.notification.presentation.deeplink.onActivityCreate
import me.proton.core.usersettings.presentation.compose.view.SecurityKeysActivity
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.android.settings.domain.entity.ThemeStyle
import me.proton.drive.android.settings.domain.usecase.GetHomeTab
import me.proton.drive.android.settings.domain.usecase.GetThemeStyle
import javax.inject.Inject
import me.proton.core.presentation.R as CorePresentation

@AndroidEntryPoint
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAnimationApi::class)
class MainActivity : FragmentActivity() {
    @Inject lateinit var keyStoreCrypto: KeyStoreCrypto
    @Inject lateinit var listenToBroadcastMessages: ListenToBroadcastMessages
    @Inject lateinit var actionProvider: ActionProvider
    @Inject lateinit var getThemeStyle: GetThemeStyle
    @Inject lateinit var getHomeTab: GetHomeTab
    @Inject lateinit var getDefaultEnabledDynamicHomeTab: GetDefaultEnabledDynamicHomeTab
    @Inject lateinit var processIntent: ProcessIntent
    @Inject lateinit var biometricPromptProvider: BiometricPromptProvider
    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var deeplinkManager: DeeplinkManager
    @Inject lateinit var activityLauncher: ActivityLauncher
    @Inject lateinit var announceEvent: AnnounceEvent
    @Inject lateinit var showRatingBooster: ShowRatingBooster

    lateinit var configurationProvider: ConfigurationProvider
    private val accountViewModel: AccountViewModel by viewModels()
    private val bugReportViewModel: BugReportViewModel by viewModels()
    private val plansViewModel: PlansViewModel by viewModels()
    private val rootView: View by lazy { findViewById(android.R.id.content) }
    private val clearBackstackTrigger = MutableSharedFlow<Unit>()
    private val deepLinkIntent = MutableSharedFlow<Intent>()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MainActivityEntryPoint {
        val configurationProvider: ConfigurationProvider
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        configurationProvider = EntryPointAccessors.fromApplication(
            context = this,
            entryPoint = MainActivityEntryPoint::class.java,
        ).configurationProvider
        applySecureFlag()
        setTheme(CorePresentation.style.ProtonTheme_Drive)
        super.onCreate(savedInstanceState)
        deeplinkManager.onActivityCreate(this, savedInstanceState)
        biometricPromptProvider.bindToActivity(this)
        initializeViewModels()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContent {
            var isDrawerOpen by remember { mutableStateOf(false) }
            val snackbarHostState = remember { ProtonSnackbarHostState() }
            val isDarkTheme by isDarkTheme()
            val startDestination by defaultStartDestination()
            SystemBarColorLaunchedEffect(isDrawerOpen, isDarkTheme)
            NotificationsLaunchedEffect(snackbarHostState)
            Content(isDarkTheme) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ProtonTheme.colors.backgroundNorm)
                ) {
                    AppNavGraph(
                        keyStoreCrypto = keyStoreCrypto,
                        deepLinkBaseUrl = this@MainActivity.deepLinkBaseUrl,
                        clearBackstackTrigger = clearBackstackTrigger,
                        deepLinkIntent = deepLinkIntent,
                        defaultStartDestination = startDestination,
                        locked = appLockManager.locked,
                        primaryAccount = accountViewModel.primaryAccount,
                        announceEvent = announceEvent,
                        exitApp = { finish() },
                        navigateToPasswordManagement = accountViewModel::startPasswordManagement,
                        navigateToRecoveryEmail = accountViewModel::startUpdateRecoveryEmail,
                        navigateToSecurityKeys = { SecurityKeysActivity.start(this@MainActivity) },
                        navigateToBugReport = bugReportViewModel::sendBugReport,
                        navigateToSubscription = plansViewModel::showCurrentPlans,
                        navigateToRatingBooster = { showRatingBooster(this@MainActivity) },
                    ) { isOpen ->
                        isDrawerOpen = isOpen
                    }
                    HeadlessSnackBar(snackbarHostState)
                }
            }
        }
        if (savedInstanceState == null)
            intent?.let { processIntent(it, deepLinkIntent, accountViewModel) }
    }

    @Composable
    private fun BoxScope.HeadlessSnackBar(snackbarHostState: ProtonSnackbarHostState) {
        val animatedPadding by animateDpAsState(targetValue = LocalSnackbarPadding.current.value)
        ProtonSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = animatedPadding)
                .clickable { snackbarHostState.snackbarHostState.currentSnackbarData?.dismiss() }
        )
    }

    @Composable
    private fun isDarkTheme(): State<Boolean> {
        val isSystemDark = isSystemInDarkTheme()
        return remember {
            accountViewModel.primaryAccount.flatMapLatest { account ->
                account?.let {
                    getThemeStyle(account.userId).map { style ->
                        when (style) {
                            ThemeStyle.SYSTEM -> isSystemDark
                            ThemeStyle.DARK -> true
                            ThemeStyle.LIGHT -> false
                        }
                    }
                } ?: flowOf(isSystemDark)
            }
        }.collectAsState(initial = isSystemDark)
    }

    @Composable
    private fun defaultStartDestination(): State<String?> =
        remember {
            accountViewModel.primaryAccount.flatMapLatest { account ->
                account?.let {
                    getDefaultEnabledDynamicHomeTab(account.userId)
                        .map { dynamicHomeTab ->
                            dynamicHomeTab.route
                        }
                } ?: flowOf(null)
            }
        }.collectAsState(initial = null)

    @Composable
    private fun SystemBarColorLaunchedEffect(isDrawerOpen: Boolean, isDarkTheme: Boolean) {
        val systemUiController = rememberSystemUiController()
        LaunchedEffect(isDrawerOpen, isDarkTheme) {
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = !isDrawerOpen && !isDarkTheme
            )
        }
    }

    @Composable
    private fun NotificationsLaunchedEffect(snackbarHostState: ProtonSnackbarHostState) {
        LaunchedEffect(this) {
            listenToBroadcastMessages().collectLatest { message ->
                val action = actionProvider.provideAction(message.extra)
                if (snackbarHostState.showSnackbar(
                        type = when (message.type) {
                            BroadcastMessage.Type.SUCCESS -> ProtonSnackbarType.SUCCESS
                            BroadcastMessage.Type.WARNING -> ProtonSnackbarType.WARNING
                            BroadcastMessage.Type.ERROR -> ProtonSnackbarType.ERROR
                            BroadcastMessage.Type.INFO -> ProtonSnackbarType.NORM
                        },
                        message = message.message,
                        actionLabel = action?.getLabel(this@MainActivity)?.toString()
                    ) == SnackbarResult.ActionPerformed
                ) {
                    action?.invoke()
                }
            }
        }
    }

    @Composable
    private fun Content(isDarkTheme: Boolean, content: @Composable () -> Unit) {
        ThumbnailEnabled {
            ProtonTheme(isDarkTheme) {
                ProvideLocalSnackbarPadding {
                    content()
                }
            }
        }
    }

    fun hideSystemBars() {
        with(WindowInsetsControllerCompat(window, rootView)) {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun showSystemBars() {
        WindowInsetsControllerCompat(window, rootView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun initializeViewModels() {
        setupAccountsViewModel()
        bugReportViewModel.initialize(this)
        plansViewModel.initialize(this)
    }

    private fun setupAccountsViewModel() {
        accountViewModel.initialize(this)
        accountViewModel.state
            .flowWithLifecycle(lifecycle, minActiveState = Lifecycle.State.RESUMED)
            .onEach { state ->
                CoreLogger.d(DriveLogTag.UI, "AccountViewModel state = $state")
                when (state) {
                    is AccountViewModel.State.Processing,
                    is AccountViewModel.State.StepNeeded,
                    is AccountViewModel.State.AccountReady -> Unit
                    is AccountViewModel.State.PrimaryNeeded -> {
                        clearBackstackTrigger.emit(Unit)
                        activityLauncher {
                            CoreLogger.d(DriveLogTag.UI, "AccountViewModel startAddAccount")
                            accountViewModel.startAddAccount()
                        }
                    }
                    is AccountViewModel.State.ExitApp -> finish()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun applySecureFlag() {
        if (configurationProvider.preventScreenCapture) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    override fun onDestroy() {
        accountViewModel.deInitialize()
        bugReportViewModel.deInitialize()
        plansViewModel.deInitialize()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.let {
            processIntent(intent, deepLinkIntent, accountViewModel, true)
        }
    }
}
