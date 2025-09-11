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

@file:OptIn(ExperimentalCoroutinesApi::class)

package me.proton.android.drive.ui.navigation

import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.ensureNavGraphSet
import me.proton.android.drive.extension.get
import me.proton.android.drive.extension.isCurrentDestination
import me.proton.android.drive.extension.log
import me.proton.android.drive.extension.require
import me.proton.android.drive.extension.requireArguments
import me.proton.android.drive.extension.runFromRoute
import me.proton.android.drive.lock.presentation.component.AppLock
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.photos.presentation.component.PhotosPermissionRationale
import me.proton.android.drive.ui.dialog.AddToAlbumsOptions
import me.proton.android.drive.ui.dialog.AlbumOptions
import me.proton.android.drive.ui.dialog.AutoLockDurations
import me.proton.android.drive.ui.dialog.ComputerOptions
import me.proton.android.drive.ui.dialog.ConfirmDeleteAlbumDialog
import me.proton.android.drive.ui.dialog.ConfirmDeletionDialog
import me.proton.android.drive.ui.dialog.ConfirmEmptyTrashDialog
import me.proton.android.drive.ui.dialog.ConfirmLeaveAlbumDialog
import me.proton.android.drive.ui.dialog.ConfirmSkipIssuesDialog
import me.proton.android.drive.ui.dialog.ConfirmStopAllSharingDialog
import me.proton.android.drive.ui.dialog.ConfirmStopLinkSharingDialog
import me.proton.android.drive.ui.dialog.ConfirmStopSyncFolderDialog
import me.proton.android.drive.ui.dialog.FileOrFolderOptions
import me.proton.android.drive.ui.dialog.LogOptions
import me.proton.android.drive.ui.dialog.MultipleFileOrFolderOptions
import me.proton.android.drive.ui.dialog.Onboarding
import me.proton.android.drive.ui.dialog.ParentFolderOptions
import me.proton.android.drive.ui.dialog.ProtonDocsInsertImageOptions
import me.proton.android.drive.ui.dialog.SendFileDialog
import me.proton.android.drive.ui.dialog.ShareExternalInvitationOptions
import me.proton.android.drive.ui.dialog.ShareInvitationOptions
import me.proton.android.drive.ui.dialog.ShareLinkPermissions
import me.proton.android.drive.ui.dialog.ShareMemberOptions
import me.proton.android.drive.ui.dialog.ShareMultiplePhotosOptions
import me.proton.android.drive.ui.dialog.SortingList
import me.proton.android.drive.ui.dialog.SystemAccessDialog
import me.proton.android.drive.ui.dialog.WhatsNew
import me.proton.android.drive.ui.navigation.animation.defaultEnterSlideTransition
import me.proton.android.drive.ui.navigation.animation.defaultPopExitSlideTransition
import me.proton.android.drive.ui.navigation.internal.DriveNavHost
import me.proton.android.drive.ui.navigation.internal.MutableNavControllerSaver
import me.proton.android.drive.ui.navigation.internal.createNavController
import me.proton.android.drive.ui.navigation.internal.modalBottomSheet
import me.proton.android.drive.ui.navigation.internal.rememberAnimatedNavController
import me.proton.android.drive.ui.screen.AccountSettingsScreen
import me.proton.android.drive.ui.screen.AlbumScreen
import me.proton.android.drive.ui.screen.AppAccessScreen
import me.proton.android.drive.ui.screen.BackupIssuesScreen
import me.proton.android.drive.ui.screen.CreateNewAlbumScreen
import me.proton.android.drive.ui.screen.DefaultHomeTabScreen
import me.proton.android.drive.ui.screen.SubscriptionPromoScreen
import me.proton.android.drive.ui.screen.FileInfoScreen
import me.proton.android.drive.ui.screen.GetMoreFreeStorageScreen
import me.proton.android.drive.ui.screen.HomeScreen
import me.proton.android.drive.ui.screen.LauncherScreen
import me.proton.android.drive.ui.screen.LogScreen
import me.proton.android.drive.ui.screen.MoveToFolder
import me.proton.android.drive.ui.screen.OfflineScreen
import me.proton.android.drive.ui.screen.PhotosBackupScreen
import me.proton.android.drive.ui.screen.PhotosUpsellScreen
import me.proton.android.drive.ui.screen.PickerAlbumScreen
import me.proton.android.drive.ui.screen.PickerPhotosScreen
import me.proton.android.drive.ui.screen.PreviewScreen
import me.proton.android.drive.ui.screen.SettingsScreen
import me.proton.android.drive.ui.screen.SigningOutScreen
import me.proton.android.drive.ui.screen.TrashScreen
import me.proton.android.drive.ui.screen.UploadToScreen
import me.proton.android.drive.ui.screen.UserInvitationScreen
import me.proton.android.drive.ui.viewmodel.ConfirmStopSyncFolderDialogViewModel
import me.proton.android.drive.ui.viewmodel.PreviewViewModel
import me.proton.core.account.domain.entity.Account
import me.proton.core.compose.component.bottomsheet.ModalBottomSheetViewState
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.drivelink.device.presentation.component.RenameDevice
import me.proton.core.drive.drivelink.rename.presentation.Rename
import me.proton.core.drive.drivelink.shared.presentation.component.DiscardChangesDialog
import me.proton.core.drive.drivelink.shared.presentation.component.LinkSettings
import me.proton.core.drive.drivelink.shared.presentation.component.ManageAccess
import me.proton.core.drive.drivelink.shared.presentation.component.ShareViaInvitations
import me.proton.core.drive.drivelink.shared.presentation.component.ShareViaLink
import me.proton.core.drive.folder.create.presentation.CreateFolder
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.presentation.compose.StorageFullDialog
import me.proton.core.drive.notification.presentation.component.NotificationPermissionRationale
import me.proton.core.drive.notification.presentation.viewmodel.NotificationPermissionRationaleViewModel
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.user.presentation.user.SignOutConfirmationDialog
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.android.settings.domain.entity.WhatsNewKey

@Composable
@ExperimentalAnimationApi
fun AppNavGraph(
    keyStoreCrypto: KeyStoreCrypto,
    deepLinkBaseUrl: String,
    clearBackstackTrigger: SharedFlow<Unit>,
    deepLinkIntent: SharedFlow<Intent>,
    defaultStartDestination: String?,
    photosRoute: String?,
    locked: Flow<Boolean>,
    primaryAccount: Flow<Account?>,
    announceEvent: AnnounceEvent,
    exitApp: () -> Unit,
    navigateToPasswordManagement: (UserId) -> Unit,
    navigateToRecoveryEmail: (UserId) -> Unit,
    navigateToSecurityKeys: (UserId) -> Unit,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    navigateToUpgradePlan: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) {
    val navController = rememberAnimatedNavController(keyStoreCrypto)
    val localContext = LocalContext.current
    var homeNavController by rememberSaveable(saver = MutableNavControllerSaver(localContext, keyStoreCrypto)) {
        mutableStateOf(createNavController(localContext))
    }
    LaunchedEffect(navController) {
        announceScreen(announceEvent, primaryAccount, navController, "App")
        navController
            .currentBackStack
            .onEach { backStackEntries ->
                val destinations = backStackEntries.map { entry -> entry.destination.route }.joinToString()
                CoreLogger.d(DriveLogTag.UI, "App current back stack: $destinations")
            }
            .launchIn(this)
    }
    LaunchedEffect(homeNavController) {
        announceScreen(announceEvent, primaryAccount, homeNavController, "Home")
        homeNavController
            .currentBackStack
            .onEach { backStackEntries ->
                val destinations = backStackEntries.map { entry -> entry.destination.route }.joinToString()
                CoreLogger.d(DriveLogTag.UI, "Home current back stack: $destinations")
            }
            .launchIn(this)
    }
    LaunchedEffect(clearBackstackTrigger) {
        clearBackstackTrigger
            .onEach {
                navController.ensureNavGraphSet().navigate(Screen.Launcher.route) {
                    popUpTo(0)
                }
                homeNavController = createNavController(localContext)
            }
            .launchIn(this)
    }
    LaunchedEffect(deepLinkIntent) {
        deepLinkIntent
            .onEach { intent ->
                CoreLogger.d(DriveLogTag.UI, "Deep link intent received")
                if (!navController.ensureNavGraphSet().handleDeepLink(intent)) {
                    // clear query params with user information before logging
                    val uri = intent.data?.buildUpon()?.apply { clearQuery() }?.build()
                    IllegalStateException("Invalid deep link: $uri").log(DriveLogTag.UI)
                    homeNavController = createNavController(localContext)
                }
            }
            .launchIn(this)
    }
    AppLock(locked = locked, primaryAccount = primaryAccount) {
        defaultStartDestination?.let {
            AppNavGraph(
                navController = navController,
                homeNavController = homeNavController,
                deepLinkBaseUrl = deepLinkBaseUrl,
                defaultStartDestination = defaultStartDestination,
                photosRoute = photosRoute,
                exitApp = exitApp,
                navigateToPasswordManagement = navigateToPasswordManagement,
                navigateToRecoveryEmail= navigateToRecoveryEmail,
                navigateToSecurityKeys = navigateToSecurityKeys,
                navigateToBugReport = navigateToBugReport,
                navigateToSubscription = navigateToSubscription,
                navigateToRatingBooster = navigateToRatingBooster,
                navigateToUpgradePlan = navigateToUpgradePlan,
                onDrawerStateChanged = onDrawerStateChanged,
            )
        }
    }
}

@Composable
@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun AppNavGraph(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    defaultStartDestination: String,
    photosRoute: String?,
    exitApp: () -> Unit,
    navigateToPasswordManagement: (UserId) -> Unit,
    navigateToRecoveryEmail: (UserId) -> Unit,
    navigateToSecurityKeys: (UserId) -> Unit,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    navigateToUpgradePlan: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) {
    DriveNavHost(
        navController = navController,
        startDestination = Screen.Launcher.route,
        modifier = Modifier.fillMaxSize()
    ) {
        addLauncher(
            navController = navController,
            deepLinkBaseUrl = deepLinkBaseUrl,
        )
        addSignOutConfirmationDialog(navController)
        addSigningOut()
        addHome(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            defaultStartDestination = defaultStartDestination,
            photosRoute = photosRoute,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            navigateToRatingBooster = navigateToRatingBooster,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addHomeFiles(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            photosRoute = photosRoute,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            navigateToRatingBooster = navigateToRatingBooster,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addHomePhotos(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            navigateToRatingBooster = navigateToRatingBooster,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addHomePhotosAndAlbums(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            navigateToRatingBooster = navigateToRatingBooster,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addHomeComputers(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            photosRoute = photosRoute,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            navigateToRatingBooster = navigateToRatingBooster,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addHomeSharedTabs(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            photosRoute = photosRoute,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            navigateToRatingBooster = navigateToRatingBooster,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addPhotosIssues(navController)
        addPhotosUpsell(navigateToSubscription)
        addConfirmSkipIssues(navController)
        addConfirmStopSyncFolderDialog(navController)
        addPhotosPermissionRationale(navController)
        addSortingList()
        addFileOrFolderOptions(navController)
        addMultipleFileOrFolderOptions(navController)
        addParentFolderOptions(navController)
        addConfirmDeletionDialog(navController)
        addConfirmStopLinkSharingDialog(navController)
        addConfirmStopAllSharingDialog(navController)
        addConfirmEmptyTrashDialog(navController)
        addTrash(navController)
        addOffline(navController)
        addPagerPreview(navController)
        addSettings(navController)
        addAccountSettings(
            navController = navController,
            navigateToPasswordManagement = navigateToPasswordManagement,
            navigateToRecoveryEmail = navigateToRecoveryEmail,
            navigateToSecurityKeys = navigateToSecurityKeys
        )
        addFileInfo(navController)
        addMoveToFolder(navController)
        addRenameDialog(navController)
        addCreateFolderDialog(navController)
        addStorageFull(navController, deepLinkBaseUrl)
        addSendFile(navController)
        addManageAccess(navController)
        addLinkSettings(navController)
        addShareViaInvitations(navController)
        addInvitationOptions(navController)
        addExternalInvitationOptions(navController)
        addMemberOptions(navController)
        addUserInvitation(navController)
        addShareLinkPermissions(navController)
        addDiscardShareViaInvitationsChanges(navController)
        addShareViaLink(navController)
        addDiscardShareViaLinkChanges(navController)
        addDiscardLinkSettingsChanges(navController)
        addUploadTo(navController, deepLinkBaseUrl, exitApp)
        addAppAccess(navController)
        addSystemAccessDialog(navController)
        addAutoLockDurations(navController)
        addPhotosBackup(navController)
        addComputerOptions(navController)
        addRenameComputerDialog(navController)
        addGetMoreFreeStorage(navController)
        addDefaultHomeTab(navController)
        addLog(navController)
        addLogOptions()
        addProtonDocsInsertImageOptions(navController)
        addOnboarding(navController)
        addWhatsNew(navController)
        addNotificationPermissionRationale(navController)
        addCreateNewAlbum(navController)
        addAlbum(navController)
        addAlbumOptions(navController)
        addConfirmDeleteAlbumDialog(navController)
        addPickerPhotos(navController)
        addPickerAlbum(navController)
        addSubscriptionPromoScreen(navigateToUpgradePlan)
        addConfirmLeaveAlbumDialog(navController)
        addShareMultiplePhotosOptions(navController)
        addAddToAlbumsOptions(navController)
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addLauncher(
    navController: NavHostController,
    deepLinkBaseUrl: String,
) = composable(
    route = Screen.Launcher.route,
    arguments = listOf(
        navArgument(Screen.Launcher.REDIRECTION) {
            type = NavType.StringType
            nullable = true
        },
    ),
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.Launcher.deepLink(deepLinkBaseUrl) }
    )
) { navBackStackEntry ->
    val redirection = navBackStackEntry.get<String>(Screen.Launcher.REDIRECTION)
    LauncherScreen(
        foregroundState = navController.isCurrentDestination(route = Screen.Launcher.route),
        navigateToHomeScreen = { userId ->
            navController.runFromRoute(route = Screen.Launcher.route) {
                navController.navigate(Screen.Home(userId, redirection)) {
                    popUpTo(Screen.Launcher.route) { inclusive = true }
                }
            }
        },
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addSigningOut() = composable(
    route = Screen.SigningOut.route,
    arguments = listOf(navArgument(Screen.SigningOut.USER_ID) { type = NavType.StringType })
) {
    SigningOutScreen()
}

fun NavGraphBuilder.addSignOutConfirmationDialog(navController: NavHostController) = dialog(
    route = Screen.Dialogs.SignOut.route,
    arguments = listOf(
        navArgument(Screen.Dialogs.SignOut.USER_ID) {
            type = NavType.StringType
        },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Dialogs.SignOut.USER_ID))
    SignOutConfirmationDialog(
        onRemove = {
            navController.navigate(Screen.SigningOut(userId = userId)) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        },
        onDismiss = {
            navController.popBackStack(
                route = Screen.Dialogs.SignOut.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addSortingList() = modalBottomSheet(
    route = Screen.Sorting.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Sorting.BY) {
            type = NavType.EnumType(By::class.java)
            nullable = false
            defaultValue = Sorting.DEFAULT.by
        },
        navArgument(Screen.Sorting.DIRECTION) {
            type = NavType.EnumType(Direction::class.java)
            nullable = false
            defaultValue = Sorting.DEFAULT.direction
        },
    ),
) { _, runAction ->
    SortingList(runAction = runAction)
}

fun NavGraphBuilder.addFileOrFolderOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.FileOrFolderOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.FileOrFolderOptions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.FileOrFolderOptions.LINK_ID) { type = NavType.StringType },
        navArgument(Screen.FileOrFolderOptions.ALBUM_ID) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(Screen.FileOrFolderOptions.ALBUM_SHARE_ID) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(Screen.FileOrFolderOptions.SELECTION_ID) {
            type = NavType.StringType
            nullable = true
        },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    FileOrFolderOptions(
        runAction = runAction,
        navigateToInfo = { linkId ->
            navController.navigate(Screen.Info(userId, linkId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToMove = { linkId, parentId ->
            navController.navigate(Screen.Move(userId, linkId, parentId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToRename = { fileId ->
            navController.navigate(Screen.Files.Dialogs.Rename(userId, fileId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToDelete = { linkId ->
            navController.navigate(Screen.Files.Dialogs.ConfirmDeletion(userId, linkId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToSendFile = { fileId ->
            navController.navigate(Screen.SendFile(userId, fileId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToManageAccess = { linkId ->
            navController.navigate(Screen.ManageAccess(userId, linkId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToShareViaInvitations = { linkId ->
            navController.navigate(Screen.ShareViaInvitations(userId, linkId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToNotificationPermissionRationale = {
            navController.navigate(
                Screen.NotificationPermissionRationale(
                    userId = userId,
                    rationaleContext = NotificationPermissionRationaleViewModel.RationaleContext.DEFAULT,
                )
            ) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToAddToAlbumsOptions = { selectionId ->
            navController.navigate(Screen.AddToAlbumsOptions(userId, selectionId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        dismiss = {
            navController.popBackStack(
                route = Screen.FileOrFolderOptions.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addMultipleFileOrFolderOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.MultipleFileOrFolderOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.MultipleFileOrFolderOptions.SELECTION_ID) { type = NavType.StringType },
        navArgument(Screen.MultipleFileOrFolderOptions.ALBUM_SHARE_ID) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(Screen.MultipleFileOrFolderOptions.ALBUM_ID) {
            type = NavType.StringType
            nullable = true
        },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    MultipleFileOrFolderOptions(
        runAction = runAction,
        navigateToMove = { selectionId, parentId ->
            navController.navigate(Screen.Move(userId, selectionId, parentId)) {
                popUpTo(Screen.MultipleFileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToAddToAlbumsOptions = { selectionId ->
            navController.navigate(Screen.AddToAlbumsOptions(userId, selectionId)) {
                popUpTo(Screen.MultipleFileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToShareMultiplePhotosOptions = { selectionId ->
            navController.navigate(Screen.ShareMultiplePhotosOptions(userId, selectionId)) {
                popUpTo(Screen.MultipleFileOrFolderOptions.route) { inclusive = true }
            }
        },
        dismiss = {
            navController.popBackStack(
                route = Screen.MultipleFileOrFolderOptions.route,
                inclusive = true,
            )
        },
    )
}

fun NavGraphBuilder.addParentFolderOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ParentFolderOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ParentFolderOptions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.ParentFolderOptions.FOLDER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    ParentFolderOptions(
        runAction = runAction,
        navigateToCreateFolder = { parentId ->
            navController.navigate(Screen.Files.Dialogs.CreateFolder(userId, parentId)) {
                popUpTo(route = Screen.ParentFolderOptions.route) { inclusive = true }
            }
        },
        navigateToStorageFull = {
            navController.navigate(Screen.Dialogs.StorageFull(userId)) {
                popUpTo(route = Screen.ParentFolderOptions.route) { inclusive = true }
            }
        },
        navigateToPreview = { fileId ->
            navController.navigate(Screen.PagerPreview(PagerType.SINGLE, userId, fileId)) {
                popUpTo(route = Screen.ParentFolderOptions.route) { inclusive = true }
            }
        },
        navigateToNotificationPermissionRationale = {
            navController.navigate(
                Screen.NotificationPermissionRationale(
                    userId = userId,
                    rationaleContext = NotificationPermissionRationaleViewModel.RationaleContext.DEFAULT,
                )
            ) {
                popUpTo(route = Screen.ParentFolderOptions.route) { inclusive = true }
            }
        },
        dismiss = {
            navController.popBackStack(
                route = Screen.ParentFolderOptions.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addInvitationOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ShareViaInvitations.InternalOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.ShareViaInvitations.InternalOptions.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.InternalOptions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.InternalOptions.LINK_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.InternalOptions.INVITATION_ID) { type = NavType.StringType },
    ),
) { _, runAction ->
    ShareInvitationOptions(
        runAction = runAction
    )
}

fun NavGraphBuilder.addExternalInvitationOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ShareViaInvitations.ExternalOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.ShareViaInvitations.ExternalOptions.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.ExternalOptions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.ExternalOptions.LINK_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.ExternalOptions.INVITATION_ID) { type = NavType.StringType },
    ),
) { _, runAction ->
    ShareExternalInvitationOptions(
        runAction = runAction
    )
}

fun NavGraphBuilder.addMemberOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ShareMemberOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.ShareMemberOptions.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareMemberOptions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.ShareMemberOptions.LINK_ID) { type = NavType.StringType },
        navArgument(Screen.ShareMemberOptions.MEMBER_ID) { type = NavType.StringType },
    ),
) { _, runAction ->
    ShareMemberOptions(
        runAction = runAction
    )
}

fun NavGraphBuilder.addUserInvitation(
    navController : NavHostController,
) = composable(
    route = Screen.UserInvitation.route,
    arguments = listOf(
        navArgument(Screen.UserInvitation.USER_ID) { type = NavType.StringType },
        navArgument(Screen.UserInvitation.ALBUMS_ONLY) { type = NavType.BoolType },
    ),
) {
    UserInvitationScreen (onBack = {
        navController.popBackStack(
            route = Screen.UserInvitation.route,
            inclusive = true,
        )
    })
}

fun NavGraphBuilder.addShareLinkPermissions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ShareLinkPermissions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.ShareLinkPermissions.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareLinkPermissions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.ShareLinkPermissions.LINK_ID) { type = NavType.StringType },
    ),
) { _, runAction ->
    ShareLinkPermissions(
        runAction = runAction
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmDeletionDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.ConfirmDeletion.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmDeletion.FILE_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmDeletion.SHARE_ID) { type = NavType.StringType },
    ),
) {
    ConfirmDeletionDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Files.Dialogs.ConfirmDeletion.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmStopLinkSharingDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.ConfirmStopLinkSharing.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmStopLinkSharing.LINK_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmStopLinkSharing.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmStopLinkSharing.CONFIRM_POP_UP_ROUTE) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.Dialogs.ConfirmStopLinkSharing.CONFIRM_POP_UP_ROUTE_INCLUSIVE) { type = NavType.BoolType },
    ),
) { navBackStackEntry ->
    val confirmPopUpRoute = navBackStackEntry.get<String>(Screen.Files.Dialogs.ConfirmStopLinkSharing.CONFIRM_POP_UP_ROUTE)
    val onDismiss: () -> Unit = {
        navController.popBackStack(
            route = Screen.Files.Dialogs.ConfirmStopLinkSharing.route,
            inclusive = true,
        )
    }
    val onConfirm: () -> Unit = confirmPopUpRoute?.let {
        val confirmPopUpRouteInclusive = navBackStackEntry.get<Boolean>(
            Screen.Files.Dialogs.ConfirmStopLinkSharing.CONFIRM_POP_UP_ROUTE_INCLUSIVE
        ) ?: true
        { navController.popBackStack(route = confirmPopUpRoute, inclusive = confirmPopUpRouteInclusive) }
    } ?: onDismiss
    ConfirmStopLinkSharingDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmStopAllSharingDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.ConfirmStopAllSharing.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmStopAllSharing.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmStopAllSharing.CONFIRM_POP_UP_ROUTE) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.Dialogs.ConfirmStopAllSharing.CONFIRM_POP_UP_ROUTE_INCLUSIVE) { type = NavType.BoolType },
    ),
) { navBackStackEntry ->
    val confirmPopUpRoute = navBackStackEntry.get<String>(Screen.Files.Dialogs.ConfirmStopAllSharing.CONFIRM_POP_UP_ROUTE)
    val onDismiss: () -> Unit = {
        navController.popBackStack(
            route = Screen.Files.Dialogs.ConfirmStopAllSharing.route,
            inclusive = true,
        )
    }
    val onConfirm: () -> Unit = confirmPopUpRoute?.let {
        val confirmPopUpRouteInclusive = navBackStackEntry.get<Boolean>(
            Screen.Files.Dialogs.ConfirmStopAllSharing.CONFIRM_POP_UP_ROUTE_INCLUSIVE
        ) ?: true
        { navController.popBackStack(route = confirmPopUpRoute, inclusive = confirmPopUpRouteInclusive) }
    } ?: onDismiss
    ConfirmStopAllSharingDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmEmptyTrashDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.ConfirmEmptyTrash.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.VOLUME_ID) { type = NavType.StringType },
    ),
) {
    ConfirmEmptyTrashDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Files.Dialogs.ConfirmEmptyTrash.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
internal fun NavGraphBuilder.addHome(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    route: String,
    defaultStartDestination: String,
    photosRoute: String?,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
    arguments: List<NamedNavArgument> = listOf(
        navArgument(Screen.Home.USER_ID) {
            type = NavType.StringType
        },
    ),
) = composable(
    route = route,
    arguments = arguments,
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    val startDestination = when (navBackStackEntry.get<String>(Screen.Home.TAB)) {
        Screen.Home.TAB_FILES -> Screen.Files.route
        Screen.Home.TAB_PHOTOS -> photosRoute ?: Screen.Photos.route
        Screen.Home.TAB_COMPUTERS -> Screen.Computers.route
        Screen.Home.TAB_SHARED_TABS -> Screen.SharedTabs.route
        else -> defaultStartDestination
    }
    val shareId = navBackStackEntry.get<String>(Screen.Files.SHARE_ID)
    val currentFolderId = navBackStackEntry.get<String>(Screen.Files.FOLDER_ID)?.let { folderId ->
        shareId?.let {
            FolderId(ShareId(userId, shareId), folderId)
        }
    }
    HomeScreen(
        userId,
        homeNavController,
        deepLinkBaseUrl,
        startDestination,
        navBackStackEntry.requireArguments(),
        navigateToBugReport = navigateToBugReport,
        onDrawerStateChanged = onDrawerStateChanged,
        navigateToSigningOut = {
            navController.navigate(Screen.Dialogs.SignOut(userId))
        },
        navigateToTrash = {
            navController.navigate(Screen.Trash(userId))
        },
        navigateToOffline = {
            navController.navigate(Screen.OfflineFiles(userId))
        },
        navigateToPreview = { fileId, pagerType ->
            navController.navigate(Screen.PagerPreview(pagerType, userId, fileId))
        },
        navigateToPreviewWithTag = { fileId, pagerType, photoTag ->
            navController.navigate(Screen.PagerPreview(pagerType, userId, fileId, photoTag))
        },
        navigateToSorting = { sorting ->
            navController.navigate(
                Screen.Sorting(userId, currentFolderId, sorting.by, sorting.direction)
            )
        },
        navigateToSettings = {
            navController.navigate(Screen.Settings(userId))
        },
        navigateToFileOrFolderOptions = { linkId, selectionId ->
            navController.navigate(
                Screen.FileOrFolderOptions(
                    userId = userId,
                    linkId = linkId,
                    selectionId = selectionId,
                )
            )
        },
        navigateToMultipleFileOrFolderOptions = { selectionId ->
            navController.navigate(
                Screen.MultipleFileOrFolderOptions(userId, selectionId)
            )
        },
        navigateToParentFolderOptions = { folderId ->
            navController.navigate(
                Screen.ParentFolderOptions(userId, folderId)
            )
        },
        navigateToSubscription = navigateToSubscription,
        navigateToPhotosIssues = { folderId ->
            navController.navigate(
                Screen.BackupIssues.invoke(folderId)
            )
        },
        navigateToPhotosUpsell = {
            navController.navigate(
                Screen.Photos.Upsell(userId)
            )
        },
        navigateToBackupSettings = {
            navController.navigate(
                Screen.Settings.PhotosBackup(userId)
            )
        },
        navigateToPhotosPermissionRationale = {
            navController.navigate(
                Screen.PhotosPermissionRationale(userId)
            )
        },
        navigateToComputerOptions = { deviceId ->
            navController.navigate(
                Screen.ComputerOptions.invoke(userId, deviceId)
            )
        },
        navigateToGetMoreFreeStorage = {
            navController.navigate(
                Screen.GetMoreFreeStorage(userId)
            )
        },
        navigateToOnboarding = {
            navController.navigate(
                Screen.Onboarding(userId)
            )
        },
        navigateToWhatsNew = { key ->
            navController.navigate(
                Screen.WhatsNew(userId, key)
            )
        },

        navigateToRatingBooster = navigateToRatingBooster,
        navigateToNotificationPermissionRationale = {
            navController.navigate(
                Screen.NotificationPermissionRationale(
                    userId = userId,
                    rationaleContext = NotificationPermissionRationaleViewModel.RationaleContext.BACKUP,
                )
            )
        },
        navigateToUserInvitation = { albumsOnly ->
            navController.navigate(Screen.UserInvitation(userId, albumsOnly))
        },
        navigateToCreateNewAlbum = {
            navController.navigate(Screen.PhotosAndAlbums.CreateNewAlbum(userId))
        },
        navigateToAlbum = { albumId ->
            navController.navigate(Screen.Album(albumId))
        },
        navigateToSubscriptionPromo = { key ->
            navController.navigate(Screen.Promo.Subscription(userId, key))
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHome(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    defaultStartDestination: String,
    photosRoute: String?,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.Home.route,
    defaultStartDestination = defaultStartDestination,
    photosRoute = photosRoute,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    navigateToRatingBooster = navigateToRatingBooster,
    onDrawerStateChanged= onDrawerStateChanged,
    arguments = listOf(
        navArgument(Screen.Home.USER_ID) {
            type = NavType.StringType
        },
        navArgument(Screen.Home.TAB) {
            type = NavType.StringType
            nullable = true
        },
    ),
)

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHomeFiles(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    photosRoute: String?,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.Files.route,
    defaultStartDestination = Screen.Files.route,
    photosRoute = photosRoute,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    navigateToRatingBooster = navigateToRatingBooster,
    onDrawerStateChanged= onDrawerStateChanged,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.FOLDER_NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
)

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHomePhotos(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.Photos.route,
    defaultStartDestination = Screen.Photos.route,
    photosRoute = Screen.Photos.route,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    navigateToRatingBooster = navigateToRatingBooster,
    onDrawerStateChanged = onDrawerStateChanged
)

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHomePhotosAndAlbums(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.PhotosAndAlbums.route,
    defaultStartDestination = Screen.PhotosAndAlbums.route,
    photosRoute = Screen.PhotosAndAlbums.route,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    navigateToRatingBooster = navigateToRatingBooster,
    onDrawerStateChanged = onDrawerStateChanged
)

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHomeComputers(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    photosRoute: String?,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.Computers.route,
    defaultStartDestination = Screen.Computers.route,
    photosRoute = photosRoute,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    navigateToRatingBooster = navigateToRatingBooster,
    onDrawerStateChanged = onDrawerStateChanged
)

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHomeSharedTabs(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    photosRoute: String?,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToRatingBooster: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.SharedTabs.route,
    defaultStartDestination = Screen.SharedTabs.route,
    photosRoute = photosRoute,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    navigateToRatingBooster = navigateToRatingBooster,
    onDrawerStateChanged = onDrawerStateChanged
)

fun NavGraphBuilder.addPhotosPermissionRationale(
    navController: NavHostController,
) = composable(
    route = Screen.PhotosPermissionRationale.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
    ),
) {
    PhotosPermissionRationale(
        modifier = Modifier
            .navigationBarsPadding()
            .offset(y = (-8).dp), // remove default modalBottomSheet top space
        // me.proton.core.compose.component.bottomsheet.ModalBottomSheet:59
        onBack = {
            navController.popBackStack(
                route = Screen.PhotosPermissionRationale.route,
                inclusive = true,
            )
        },
    )
}
fun NavGraphBuilder.addPhotosIssues(
    navController: NavHostController,
) = composable(
    route = Screen.BackupIssues.route,
    arguments = listOf(
        navArgument(Screen.BackupIssues.USER_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.FOLDER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.BackupIssues.USER_ID))
    val shareId = ShareId(userId, navBackStackEntry.require(Screen.BackupIssues.SHARE_ID))
    val folderId = FolderId(shareId, navBackStackEntry.require(Screen.BackupIssues.FOLDER_ID))
    BackupIssuesScreen(
        modifier = Modifier
            .navigationBarsPadding(),
        navigateBack = {
            navController.popBackStack(
                route = Screen.BackupIssues.route,
                inclusive = true,
            )
        },
        navigateToSkipIssues = {
            navController.navigate(
                Screen.BackupIssues.Dialogs.ConfirmSkipIssues(
                    folderId,
                    Screen.BackupIssues.route
                )
            )
        }
    )
}

fun NavGraphBuilder.addConfirmSkipIssues(
    navController: NavHostController,
) = dialog(
    route = Screen.BackupIssues.Dialogs.ConfirmSkipIssues.route,
    arguments = listOf(
        navArgument(Screen.BackupIssues.USER_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.FOLDER_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.Dialogs.ConfirmSkipIssues.CONFIRM_POP_UP_ROUTE) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.BackupIssues.Dialogs.ConfirmSkipIssues.CONFIRM_POP_UP_ROUTE_INCLUSIVE) {
            type =
                NavType.BoolType
        },
    ),
) { navBackStackEntry ->
    val confirmPopUpRoute =
        navBackStackEntry.get<String>(Screen.BackupIssues.Dialogs.ConfirmSkipIssues.CONFIRM_POP_UP_ROUTE)
    val onDismiss: () -> Unit = {
        navController.popBackStack(
            route = Screen.BackupIssues.Dialogs.ConfirmSkipIssues.route,
            inclusive = true,
        )
    }
    val onConfirm: () -> Unit = confirmPopUpRoute?.let {
        val confirmPopUpRouteInclusive = navBackStackEntry.get<Boolean>(
            Screen.BackupIssues.Dialogs.ConfirmSkipIssues.CONFIRM_POP_UP_ROUTE_INCLUSIVE
        ) ?: true
        {
            navController.popBackStack(
                route = confirmPopUpRoute,
                inclusive = confirmPopUpRouteInclusive
            )
        }
    } ?: onDismiss
    ConfirmSkipIssuesDialog(
        modifier = Modifier
            .navigationBarsPadding(),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

fun NavGraphBuilder.addConfirmStopSyncFolderDialog(
    navController: NavHostController,
) = dialog(
    route = Screen.Settings.PhotosBackup.Dialogs.ConfirmStopSyncFolder.route,
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Settings.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Settings.FOLDER_ID) { type = NavType.StringType },
        navArgument(ConfirmStopSyncFolderDialogViewModel.ID) {
            type = NavType.IntType
        },
    ),
) { navBackStackEntry ->
    val onAction: () -> Unit = {
        navController.popBackStack(
            route = Screen.Settings.PhotosBackup.Dialogs.ConfirmStopSyncFolder.route,
            inclusive = true,
        )
    }
    ConfirmStopSyncFolderDialog(
        modifier = Modifier.navigationBarsPadding(),
        onDismiss = onAction,
        onConfirm = onAction,
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addTrash(navController: NavHostController) = composable(
    route = Screen.Trash.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    TrashScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Trash.route,
                inclusive = true,
            )
        },
        navigateToEmptyTrash = { volumeId ->
            navController.navigate(Screen.Files.Dialogs.ConfirmEmptyTrash(userId, volumeId))
        },
        navigateToFileOrFolderOptions = { linkId ->
            navController.navigate(Screen.FileOrFolderOptions(userId, linkId))
        },
        navigateToSortingDialog = { sorting ->
            navController.navigate(Screen.Sorting(userId, null, sorting.by, sorting.direction))
        }
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addOffline(navController: NavHostController) = composable(
    route = Screen.OfflineFiles.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.FOLDER_NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    OfflineScreen(
        navigateToFiles = { folderId, folderName ->
            navController.navigate(
                Screen.OfflineFiles(userId, folderId, folderName)
            )
        },
        navigateToPreview = { pagerType, fileId ->
            navController.navigate(
                Screen.PagerPreview(pagerType, userId, fileId)
            )
        },
        navigateToAlbum = { albumId ->
            navController.navigate(
                Screen.Album(albumId)
            )
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.OfflineFiles.route,
                inclusive = true,
            )
        },
        navigateToSortingDialog = { sorting ->
            navController.navigate(
                Screen.Sorting(userId, null, sorting.by, sorting.direction)
            )
        },
        navigateToFileOrFolderOptions = { linkId ->
            navController.navigate(
                Screen.FileOrFolderOptions(userId, linkId)
            )
        },
        navigateToAlbumOptions = { albumId ->
            navController.navigate(
                Screen.AlbumOptions(userId, albumId)
            )
        },
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addPagerPreview(navController: NavHostController) = composable(
    route = Screen.PagerPreview.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.PagerPreview.PAGER_TYPE) { type = NavType.EnumType(PagerType::class.java) },
        navArgument(Screen.PagerPreview.USER_ID) { type = NavType.StringType },
        navArgument(Screen.PagerPreview.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.PagerPreview.FILE_ID) { type = NavType.StringType },
        navArgument(Screen.PagerPreview.ALBUM_SHARE_ID) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(Screen.PagerPreview.ALBUM_ID) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(Screen.PagerPreview.PHOTO_TAG) {
            type = NavType.StringType
            nullable = true
        },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    PreviewScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.PagerPreview.route,
                inclusive = true,
            )
        },
        navigateToFileOrFolderOptions = { linkId, albumId ->
            navController.navigate(
                Screen.FileOrFolderOptions(userId, linkId, albumId)
            )
        },
        navigateToProtonDocsInsertImageOptions = {
            navController.navigate(
                Screen.ProtonDocsInsertImageOptions(userId)
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addSettings(navController: NavHostController) = composable(
    route = Screen.Settings.route,
    arguments = listOf(
        navArgument(Screen.PagerPreview.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    SettingsScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Settings.route,
                inclusive = true,
            )
        },
        navigateToAccountSettings = {
            navController.navigate(Screen.Settings.AccountSettings(userId))
        },
        navigateToAppAccess = {
            navController.navigate(Screen.Settings.AppAccess(userId))
        },
        navigateToAutoLockDurations = {
            navController.navigate(Screen.Settings.AutoLockDurations(userId))
        },
        navigateToPhotosBackup = {
            navController.navigate(Screen.Settings.PhotosBackup(userId))
        },
        navigateToDefaultHomeTab = {
            navController.navigate(Screen.Settings.DefaultHomeTab(userId))
        },
        navigateToLog = {
            navController.navigate(Screen.Log(userId))
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addAccountSettings(
    navController: NavHostController,
    navigateToPasswordManagement: (UserId) -> Unit,
    navigateToRecoveryEmail: (UserId) -> Unit,
    navigateToSecurityKeys: (UserId) -> Unit
) = composable(
    route = Screen.Settings.AccountSettings.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Settings.USER_ID))
    AccountSettingsScreen(
        navigateToPasswordManagement = { navigateToPasswordManagement(userId) },
        navigateToRecoveryEmail = { navigateToRecoveryEmail(userId) },
        navigateToSecurityKeys = { navigateToSecurityKeys(userId) },
        navigateBack = {
            navController.popBackStack(
                route = Screen.Settings.AccountSettings.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalAnimationApi
@OptIn(ExperimentalCoroutinesApi::class)
fun NavGraphBuilder.addFileInfo(navController: NavHostController) = composable(
    route = Screen.Info.route,
    enterTransition = defaultEnterSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Up) { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Down) { true },
    arguments = listOf(
        navArgument(Screen.Info.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Info.LINK_ID) { type = NavType.StringType },
    ),
) {
    FileInfoScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Info.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addRenameDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.Rename.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.Rename.FILE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.Dialogs.Rename.FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
) {
    Rename(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Files.Dialogs.Rename.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addCreateFolderDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.CreateFolder.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.CreateFolder.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.CreateFolder.PARENT_ID) { type = NavType.StringType },
    ),
) {
    CreateFolder(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Files.Dialogs.CreateFolder.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addMoveToFolder(navController: NavHostController) = composable(
    route = Screen.Move.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Move.SELECTION_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Move.SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Move.LINK_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Move.PARENT_SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Move.PARENT_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    MoveToFolder(
        navigateToCreateFolder = { parentId ->
            navController.navigate(Screen.Files.Dialogs.CreateFolder(userId, parentId))
        }
    ) {
        navController.popBackStack(
            route = Screen.Move.route,
            inclusive = true,
        )
    }
}

fun NavGraphBuilder.addStorageFull(navController: NavHostController, deepLinkBaseUrl: String) = dialog(
    route = Screen.Dialogs.StorageFull.route,
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.Dialogs.StorageFull.deepLink(deepLinkBaseUrl) }
    )
) {
    StorageFullDialog {
        navController.popBackStack(
            route = Screen.Dialogs.StorageFull.route,
            inclusive = true,
        )
    }
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addSendFile(navController: NavHostController) = dialog(
    route = Screen.SendFile.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.SendFile.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.SendFile.FILE_ID) { type = NavType.StringType },
    ),
) {
    SendFileDialog {
        navController.popBackStack(
            route = Screen.SendFile.route,
            inclusive = true,
        )
    }
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addShareViaLink(navController: NavHostController) = composable(
    route = Screen.ShareViaLink.route,
    enterTransition = defaultEnterSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Up) { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Down) { true },
    arguments = listOf(
        navArgument(Screen.ShareViaLink.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaLink.LINK_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.ShareViaLink.USER_ID))
    ShareViaLink(
        navigateToStopSharing = { linkId ->
            navController.navigate(Screen.Files.Dialogs.ConfirmStopLinkSharing(userId, linkId, Screen.ShareViaLink.route))
        },
        navigateToDiscardChanges = { linkId ->
            navController.navigate(Screen.ShareViaLink.Dialogs.DiscardChanges(userId, linkId))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.ShareViaLink.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addLinkSettings(navController: NavHostController) = composable(
    route = Screen.LinkSettings.route,
    enterTransition = defaultEnterSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Up) { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Down) { true },
    arguments = listOf(
        navArgument(Screen.LinkSettings.USER_ID) { type = NavType.StringType },
        navArgument(Screen.LinkSettings.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.LinkSettings.LINK_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.LinkSettings.USER_ID))
    LinkSettings(
        navigateToDiscardChanges = { linkId ->
            navController.navigate(Screen.LinkSettings.Dialogs.DiscardChanges(linkId))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.LinkSettings.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addManageAccess(navController: NavHostController) = composable(
    route = Screen.ManageAccess.route,
    enterTransition = defaultEnterSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Up) { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Down) { true },
    arguments = listOf(
        navArgument(Screen.ManageAccess.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ManageAccess.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.ManageAccess.LINK_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.ManageAccess.USER_ID))
    ManageAccess(
        navigateToShareViaInvitations = { linkId ->
            navController.navigate(Screen.ShareViaInvitations(userId, linkId))
        },
        navigateToLinkSettings = { linkId ->
            navController.navigate(Screen.LinkSettings(userId, linkId))
        },
        navigateToStopLinkSharing = { linkId ->
            navController.navigate(Screen.Files.Dialogs.ConfirmStopLinkSharing(userId, linkId))
        },
        navigateToInvitationOptions = { linkId, invitationId ->
            navController.navigate(Screen.ShareViaInvitations.InternalOptions(linkId, invitationId))
        },
        navigateToExternalInvitationOptions = { linkId, invitationId ->
            navController.navigate(Screen.ShareViaInvitations.ExternalOptions(linkId, invitationId))
        },
        navigateToMemberOptions = { linkId, memberId ->
            navController.navigate(Screen.ShareMemberOptions(linkId, memberId))
        },
        navigateToShareLinkPermissions = { linkId ->
            navController.navigate(Screen.ShareLinkPermissions(linkId))
        },
        navigateToStopAllSharing = { shareId ->
            navController.navigate(Screen.Files.Dialogs.ConfirmStopAllSharing(shareId, confirmPopUpRoute = Screen.ManageAccess.route))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.ManageAccess.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addShareViaInvitations(navController: NavHostController) = composable(
    route = Screen.ShareViaInvitations.route,
    arguments = listOf(
        navArgument(Screen.ShareViaInvitations.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.LINK_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    ShareViaInvitations(
        navigateToDiscardChanges = { linkId ->
            navController.navigate(Screen.ShareViaInvitations.Dialogs.DiscardChanges(linkId))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.ShareViaInvitations.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addDiscardShareViaLinkChanges(navController: NavHostController) = dialog(
    route = Screen.ShareViaLink.Dialogs.DiscardChanges.route,
    arguments = listOf(
        navArgument(Screen.ShareViaLink.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaLink.LINK_ID) { type = NavType.StringType },
    ),
) {
    DiscardChangesDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.ShareViaLink.Dialogs.DiscardChanges.route,
                inclusive = true,
            )
        },
        onConfirm = {
            navController.popBackStack(
                route = Screen.ShareViaLink.route,
                inclusive = true,
            )
        }
    )
}
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addDiscardLinkSettingsChanges(navController: NavHostController) = dialog(
    route = Screen.LinkSettings.Dialogs.DiscardChanges.route,
    arguments = listOf(
        navArgument(Screen.LinkSettings.USER_ID) { type = NavType.StringType },
        navArgument(Screen.LinkSettings.LINK_ID) { type = NavType.StringType },
    ),
) {
    DiscardChangesDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.LinkSettings.Dialogs.DiscardChanges.route,
                inclusive = true,
            )
        },
        onConfirm = {
            navController.popBackStack(
                route = Screen.LinkSettings.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addDiscardShareViaInvitationsChanges(navController: NavHostController) = dialog(
    route = Screen.ShareViaInvitations.Dialogs.DiscardChanges.route,
    arguments = listOf(
        navArgument(Screen.ShareViaInvitations.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaInvitations.LINK_ID) { type = NavType.StringType },
    ),
) {
    DiscardChangesDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.ShareViaInvitations.Dialogs.DiscardChanges.route,
                inclusive = true,
            )
        },
        onConfirm = {
            navController.popBackStack(
                route = Screen.ShareViaInvitations.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addUploadTo(
    navController: NavHostController,
    deepLinkBaseUrl: String,
    exitApp: () -> Unit,
) = composable(
    route = Screen.Upload.route,
    arguments = listOf(
        navArgument(Screen.Upload.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Upload.URIS) {
            type = UploadType
            defaultValue = UploadParameters(emptyList())
        },
        navArgument(Screen.Upload.PARENT_SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Upload.PARENT_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.Upload.deepLink(deepLinkBaseUrl) }
    )
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Upload.USER_ID))
    UploadToScreen(
        navigateToStorageFull = {
            navController.navigate(Screen.Dialogs.StorageFull(userId))
        },
        navigateToCreateFolder = { parentId ->
            navController.navigate(Screen.Files.Dialogs.CreateFolder(userId, parentId))
        },
        exitApp = exitApp,
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addAppAccess(navController: NavHostController) = composable(
    route = Screen.Settings.AppAccess.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Settings.USER_ID))
    AppAccessScreen(
        navigateToSystemAccess = {
            navController.navigate(Screen.Settings.AppAccess.Dialogs.SystemAccess(userId))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.Settings.AppAccess.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addSystemAccessDialog(navController: NavHostController) = dialog(
    route = Screen.Settings.AppAccess.Dialogs.SystemAccess.route,
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) {
    SystemAccessDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Settings.AppAccess.Dialogs.SystemAccess.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addAutoLockDurations(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.Settings.AutoLockDurations.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
) { _, runAction ->
    AutoLockDurations(
        runAction = runAction,
        dismiss = {
            navController.popBackStack(
                route = Screen.Settings.AutoLockDurations.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addPhotosBackup(navController: NavHostController) = composable(
    route = Screen.Settings.PhotosBackup.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Settings.USER_ID))
    PhotosBackupScreen(
        navigateToPhotosPermissionRationale = {
            navController.navigate(
                Screen.PhotosPermissionRationale(userId)
            )
        },
        navigateToConfirmStopSyncFolder = { folderId, id ->
            navController.navigate(
                Screen.Settings.PhotosBackup.Dialogs.ConfirmStopSyncFolder.invoke(folderId, id)
            )
        },
        navigateToNotificationPermissionRationale = {
            navController.navigate(
                Screen.NotificationPermissionRationale(
                    userId = userId,
                    rationaleContext = NotificationPermissionRationaleViewModel.RationaleContext.BACKUP,
                )
            )
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.Settings.PhotosBackup.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addPhotosUpsell(
    navigateToSubscription: () -> Unit,
) = modalBottomSheet(
    route = Screen.Photos.Upsell.route,
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) { _, runAction ->
    PhotosUpsellScreen(
        runAction = runAction,
        navigateToSubscription = navigateToSubscription,
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addSubscriptionPromoScreen(
    navigateToSubscription: () -> Unit,
) = modalBottomSheet(
    route = Screen.Promo.Subscription.route,
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Promo.Subscription.PROMO_KEY) { type = NavType.StringType },
    ),
) { _, runAction ->
    SubscriptionPromoScreen(
        runAction = runAction,
        navigateToSubscription = navigateToSubscription,
    )
}

fun NavGraphBuilder.addComputerOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ComputerOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ComputerOptions.DEVICE_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    ComputerOptions(
        runAction = runAction,
        navigateToRenameComputer = { deviceId: DeviceId, folderId: FolderId ->
            navController.navigate(Screen.Dialogs.RenameComputer.invoke(userId, deviceId, folderId)) {
                popUpTo(route = Screen.ComputerOptions.route) { inclusive = true }
            }
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addRenameComputerDialog(navController: NavHostController) = dialog(
    route = Screen.Dialogs.RenameComputer.route,
    arguments = listOf(
        navArgument(Screen.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Dialogs.RenameComputer.FOLDER_ID) { type = NavType.StringType },
        navArgument(Screen.Dialogs.RenameComputer.DEVICE_ID) { type = NavType.StringType },
    ),
) {
    RenameDevice(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Dialogs.RenameComputer.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addGetMoreFreeStorage(navController: NavHostController) = composable(
    route = Screen.GetMoreFreeStorage.route,
    enterTransition = defaultEnterSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Up) { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Down) { true },
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) {
    GetMoreFreeStorageScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.GetMoreFreeStorage.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addDefaultHomeTab(navController: NavHostController) = composable(
    route = Screen.Settings.DefaultHomeTab.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) {
    DefaultHomeTabScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Settings.DefaultHomeTab.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addLog(navController: NavHostController) = composable(
    route = Screen.Log.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Log.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Log.USER_ID))
    LogScreen(
        navigateToLogOptions = {
            navController.navigate(Screen.Log.Options(userId))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.Log.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addLogOptions() = modalBottomSheet(
    route = Screen.Log.Options.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Log.USER_ID) { type = NavType.StringType },
    ),
) { _, _ ->
    LogOptions()
}

fun NavGraphBuilder.addProtonDocsInsertImageOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ProtonDocsInsertImageOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.USER_ID) { type = NavType.StringType },
    ),
) { _, _ ->
    val viewModel = navController.previousBackStackEntry?.let { prevNavBackStackEntry ->
        runCatching { hiltViewModel<PreviewViewModel>(prevNavBackStackEntry) }.getOrNull()
    }
    ProtonDocsInsertImageOptions(
        saveResult = { uris ->
            viewModel?.savedStateHandle?.set(PreviewViewModel.PROTON_DOCS_IMAGE_URIS, uris)
        },
        dismiss = {
            navController.popBackStack(
                route = Screen.ProtonDocsInsertImageOptions.route,
                inclusive = true,
            )
        },
    )
}

fun NavGraphBuilder.addOnboarding(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.Onboarding.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Onboarding.USER_ID) { type = NavType.StringType },
    ),
) { _, _ ->
    Onboarding(
        dismiss = {
            navController.popBackStack(
                route = Screen.Onboarding.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addWhatsNew(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.WhatsNew.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.WhatsNew.USER_ID) { type = NavType.StringType },
        navArgument(Screen.WhatsNew.KEY) { type = NavType.EnumType(WhatsNewKey::class.java) },
    ),
) { _, _ ->
    WhatsNew(
        dismiss = {
            navController.popBackStack(
                route = Screen.WhatsNew.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addNotificationPermissionRationale(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.NotificationPermissionRationale.route,
    viewState = ModalBottomSheetViewState(),
    arguments = listOf(
        navArgument(Screen.NotificationPermissionRationale.USER_ID) { type = NavType.StringType },
        navArgument(Screen.NotificationPermissionRationale.RATIONALE_CONTEXT) { type = NavType.StringType },
    ),
) { _, runAction ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        NotificationPermissionRationale(
            runAction = runAction,
            dismiss = {
                navController.popBackStack(
                    route = Screen.NotificationPermissionRationale.route,
                    inclusive = true,
                )
            },
        )
    } else {
        CoreLogger.w(
            tag = DriveLogTag.UI,
            message = "NotificationPermissionRationale invoked on API level ${Build.VERSION.SDK_INT}",
        )
    }
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addCreateNewAlbum(navController: NavHostController) = composable(
    route = Screen.PhotosAndAlbums.CreateNewAlbum.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.PhotosAndAlbums.USER_ID) { type = NavType.StringType },
        navArgument(Screen.PhotosAndAlbums.SHARED_ALBUM) { type = NavType.BoolType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.PhotosAndAlbums.USER_ID))
    val sharedAlbum: Boolean = navBackStackEntry.get(Screen.PhotosAndAlbums.SHARED_ALBUM) ?: false
    CreateNewAlbumScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.PhotosAndAlbums.CreateNewAlbum.route,
                inclusive = true,
            )
        },
        navigateToAlbum = { albumId: AlbumId ->
            navController.navigate(Screen.Album(albumId)) {
                popUpTo(route = Screen.PhotosAndAlbums.CreateNewAlbum.route) {
                    inclusive = true
                }
            }
            if (sharedAlbum) {
                navController.navigate(Screen.ShareViaInvitations(userId, albumId))
            }
        },
        navigateToPicker = {
            navController.navigate(Screen.Picker.Photos(userId = userId))
        },
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addAlbum(navController: NavHostController) = composable(
    route = Screen.Album.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Album.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Album.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Album.ALBUM_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    AlbumScreen(
        navigateToAlbumOptions = { albumId ->
            navController.navigate(Screen.AlbumOptions(userId, albumId))
        },
        navigateToPhotosOptions = { linkId, albumId, selectionId ->
            navController.navigate(
                Screen.FileOrFolderOptions(userId, linkId, albumId, selectionId)
            )
        },
        navigateToMultiplePhotosOptions = { selectionId, albumId ->
            navController.navigate(
                Screen.MultipleFileOrFolderOptions(userId, selectionId, albumId)
            )
        },
        navigateToPreview = { fileId, albumId ->
            navController.navigate(Screen.PagerPreview(
                pagerType = PagerType.ALBUM,
                userId = userId,
                fileId = fileId,
                albumId = albumId,
            ))
        },
        navigateToPicker = { albumId ->
            navController.navigate(Screen.Picker.Photos(destinationAlbumId = albumId))
        },
        navigateToShareViaInvitations = { albumId ->
            navController.navigate(Screen.ShareViaInvitations(userId, albumId))
        },
        navigateToManageAccess = { albumId ->
            navController.navigate(Screen.ManageAccess(userId, albumId))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.Album.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addAlbumOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.AlbumOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.USER_ID) { type = NavType.StringType },
        navArgument(Screen.AlbumOptions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.AlbumOptions.ALBUM_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    AlbumOptions(
        runAction = runAction,
        navigateToShareViaInvitations = { linkId ->
            navController.navigate(Screen.ShareViaInvitations(userId, linkId)) {
                popUpTo(Screen.AlbumOptions.route) { inclusive = true }
            }
        },
        navigateToManageAccess = { linkId ->
            navController.navigate(Screen.ManageAccess(userId, linkId)) {
                popUpTo(Screen.AlbumOptions.route) { inclusive = true }
            }
        },
        navigateToRename = { albumId ->
            navController.navigate(Screen.Files.Dialogs.Rename(userId, albumId)) {
                popUpTo(Screen.AlbumOptions.route) { inclusive = true }
            }
        },
        navigateToDelete = { albumId ->
            navController.navigate(Screen.Album.Dialogs.ConfirmDeleteAlbum(albumId)) {
                popUpTo(Screen.AlbumOptions.route) { inclusive = true }
            }
        },
        navigateToLeave = { albumId ->
            navController.navigate(Screen.Album.Dialogs.ConfirmLeaveAlbum(albumId)) {
                popUpTo(Screen.AlbumOptions.route) { inclusive = true }
            }
        },
        dismiss = {
            navController.popBackStack(
                route = Screen.AlbumOptions.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmDeleteAlbumDialog(navController: NavHostController) = dialog(
    route = Screen.Album.Dialogs.ConfirmDeleteAlbum.route,
    arguments = listOf(
        navArgument(Screen.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Album.Dialogs.ConfirmDeleteAlbum.ALBUM_ID) { type = NavType.StringType },
        navArgument(Screen.Album.Dialogs.ConfirmDeleteAlbum.SHARE_ID) { type = NavType.StringType },
    ),
) {
    ConfirmDeleteAlbumDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Album.Dialogs.ConfirmDeleteAlbum.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addPickerPhotos(navController: NavHostController) = composable(
    route = Screen.Picker.Photos.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Picker.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Picker.IN_PICKER_MODE) { type = NavType.BoolType },
        navArgument(Screen.Picker.SHARE_ID) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(Screen.Picker.ALBUM_ID) {
            type = NavType.StringType
            nullable = true
        },
    ),
) {
    PickerPhotosScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Picker.Photos.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addPickerAlbum(navController: NavHostController) = composable(
    route = Screen.Picker.Album.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Picker.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Picker.IN_PICKER_MODE) { type = NavType.BoolType },
        navArgument(Screen.Picker.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Picker.ALBUM_ID) { type = NavType.StringType },
        navArgument(Screen.Picker.DESTINATION_SHARE_ID) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(Screen.Picker.DESTINATION_ALBUM_ID) {
            type = NavType.StringType
            nullable = true
        },
    ),
) {
    PickerAlbumScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Picker.Album.route,
                inclusive = true,
            )
        },
        onAddToAlbumDone = {
            navController.popBackStack(
                route = Screen.Picker.Photos.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmLeaveAlbumDialog(navController: NavHostController) = dialog(
    route = Screen.Album.Dialogs.ConfirmLeaveAlbum.route,
    arguments = listOf(
        navArgument(Screen.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Album.Dialogs.ConfirmLeaveAlbum.ALBUM_ID) { type = NavType.StringType },
        navArgument(Screen.Album.Dialogs.ConfirmLeaveAlbum.SHARE_ID) { type = NavType.StringType },
    ),
) {
    ConfirmLeaveAlbumDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Album.Dialogs.ConfirmLeaveAlbum.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addShareMultiplePhotosOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ShareMultiplePhotosOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.ShareMultiplePhotosOptions.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareMultiplePhotosOptions.SELECTION_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.ShareMultiplePhotosOptions.USER_ID))
    ShareMultiplePhotosOptions(
        runAction = runAction,
        navigateToCreateNewAlbum = {
            navController.navigate(
                Screen.PhotosAndAlbums.CreateNewAlbum(
                    userId = userId,
                    sharedAlbum = true,
                )
            )
        },
        navigateToAlbum = { albumId ->
            navController.navigate(Screen.Album(albumId))
        }
    )
}

fun NavGraphBuilder.addAddToAlbumsOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.AddToAlbumsOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.AddToAlbumsOptions.USER_ID) { type = NavType.StringType },
        navArgument(Screen.AddToAlbumsOptions.SELECTION_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.AddToAlbumsOptions.USER_ID))
    AddToAlbumsOptions(
        runAction = runAction,
        navigateToCreateNewAlbum = {
            navController.navigate(
                Screen.PhotosAndAlbums.CreateNewAlbum(userId)
            )
        },
        navigateToAlbum = { albumId ->
            navController.navigate(Screen.Album(albumId))
        }
    )
}

private suspend fun CoroutineScope.announceScreen(
    announceEvent: AnnounceEvent,
    primaryAccount: Flow<Account?>,
    navController: NavHostController,
    source: String
) {
    combine(
        primaryAccount.filterNotNull(),
        navController.currentBackStackEntryFlow,
    ) { account, entry ->
        account.userId to entry.destination.route
    }.onEach { (userId, route) ->
        announceEvent(userId, Event.Screen(source, route))
    }.launchIn(this)
}
