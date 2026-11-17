package com.example.ui.navigation

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.domain.model.AppPreferences
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.PreferencesRepository
import com.example.ui.auth.AuthGateScreen
import com.example.ui.screens.board.GameBoardScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.lobby.LobbyScreen
import com.example.ui.screens.lobby.OnlineRoomScreen
import com.example.ui.screens.mode.GameModeId
import com.example.ui.screens.mode.GameModeSelectionScreen
import com.example.ui.screens.players.PlayerSelectionScreen
import com.example.ui.screens.profile.PlayerProfileScreen
import com.example.ui.screens.result.ResultScreen
import com.example.ui.screens.rules.RulesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import kotlinx.coroutines.launch

private data class TopLevelNavItem(
  val route: AppDestination,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val TOP_LEVEL_ITEMS = listOf(
  TopLevelNavItem(
    route = AppDestination.HomeRoute,
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_home",
  ),
  TopLevelNavItem(
    route = AppDestination.LobbyRoute,
    labelRes = R.string.nav_lobby,
    selectedIcon = Icons.Filled.Groups,
    unselectedIcon = Icons.Outlined.Groups,
    testTag = "nav_lobby",
  ),
  TopLevelNavItem(
    route = AppDestination.ProfileRoute,
    labelRes = R.string.nav_profile,
    selectedIcon = Icons.Filled.Person,
    unselectedIcon = Icons.Outlined.Person,
    testTag = "nav_profile",
  ),
  TopLevelNavItem(
    route = AppDestination.RulesRoute,
    labelRes = R.string.nav_rules,
    selectedIcon = Icons.Filled.MenuBook,
    unselectedIcon = Icons.Outlined.MenuBook,
    testTag = "nav_rules",
  ),
  TopLevelNavItem(
    route = AppDestination.SettingsRoute,
    labelRes = R.string.nav_settings,
    selectedIcon = Icons.Filled.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    testTag = "nav_settings",
  ),
)

@Composable
fun AppNavigation(
  authRepository: AuthRepository,
  preferencesRepository: PreferencesRepository,
  modifier: Modifier = Modifier,
) {
  var showSplash by rememberSaveable { mutableStateOf(true) }
  val scope = rememberCoroutineScope()
  val currentUser by authRepository.currentUser.collectAsStateWithLifecycle(
    initialValue = authRepository.getCurrentUserSnapshot(),
  )
  val prefs by preferencesRepository.preferences.collectAsStateWithLifecycle(
    initialValue = AppPreferences(),
  )

  LaunchedEffect(currentUser?.uid) {
    if (currentUser != null && prefs.guestModeEnabled) {
      preferencesRepository.setGuestSession(enabled = false)
    }
  }

  val isGuestSession = currentUser == null && prefs.guestModeEnabled
  val phaseKey = when {
    showSplash -> "splash"
    currentUser != null -> "main_google_${currentUser!!.uid}"
    isGuestSession -> "main_guest"
    else -> "auth"
  }

  AnimatedContent(
    targetState = phaseKey,
    transitionSpec = {
      fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
    },
    label = "root_phase_transition",
    modifier = modifier.fillMaxSize(),
  ) { stateKey ->
    when {
      stateKey == "splash" -> {
        SplashScreen(
          onSplashFinished = { showSplash = false },
        )
      }
      stateKey == "auth" -> {
        AuthGateScreen(
          onAuthSuccess = {
            scope.launch { preferencesRepository.setGuestSession(enabled = false) }
          },
          onEnterGuestMode = { guestName ->
            scope.launch { preferencesRepository.setGuestSession(enabled = true, displayName = guestName) }
          },
        )
      }
      else -> {
        val activeUser = currentUser ?: AuthenticatedUser(
          uid = "guest_local",
          displayName = prefs.guestDisplayName.ifBlank { "অতিথি খেলোয়াড়" },
          email = "",
        )
        key(stateKey) {
          AuthenticatedAppShell(user = activeUser)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun AuthenticatedAppShell(
  user: AuthenticatedUser,
  modifier: Modifier = Modifier,
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination

  val isFullScreenGameFlow =
    currentDestination?.hasRoute<AppDestination.GameModeRoute>() == true ||
      currentDestination?.hasRoute<AppDestination.PlayerSelectionRoute>() == true ||
      currentDestination?.hasRoute<AppDestination.GameBoardRoute>() == true ||
      currentDestination?.hasRoute<AppDestination.OnlineRoomRoute>() == true ||
      currentDestination?.hasRoute<AppDestination.ResultRoute>() == true

  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val widthSizeClass = (context as? Activity)?.let { activity ->
    calculateWindowSizeClass(activity).widthSizeClass
  } ?: WindowWidthSizeClass.Compact

  val useNavigationRail =
    !isFullScreenGameFlow &&
      (widthSizeClass != WindowWidthSizeClass.Compact ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

  val showBottomBar = !isFullScreenGameFlow && !useNavigationRail

  fun navigateTopLevel(destination: AppDestination) {
    navController.navigate(destination) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = {
      if (showBottomBar) {
        NavigationBar {
          TOP_LEVEL_ITEMS.forEach { item ->
            val selected = currentDestination?.hasRoute(item.route::class) == true
            val label = stringResource(item.labelRes)
            NavigationBarItem(
              selected = selected,
              onClick = { navigateTopLevel(item.route) },
              icon = {
                Icon(
                  imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = label,
                )
              },
              label = {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              modifier = Modifier.testTag(item.testTag),
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      if (useNavigationRail) {
        NavigationRail {
          TOP_LEVEL_ITEMS.forEach { item ->
            val selected = currentDestination?.hasRoute(item.route::class) == true
            val label = stringResource(item.labelRes)
            NavigationRailItem(
              selected = selected,
              onClick = { navigateTopLevel(item.route) },
              icon = {
                Icon(
                  imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = label,
                )
              },
              label = {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                )
              },
              modifier = Modifier.testTag("${item.testTag}_rail"),
            )
          }
        }
      }

      NavHost(
        navController = navController,
        startDestination = AppDestination.HomeRoute,
        enterTransition = {
          fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 12 }
        },
        exitTransition = {
          fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 12 }
        },
        popEnterTransition = {
          fadeIn(tween(240)) + slideInHorizontally(tween(240)) { -it / 12 }
        },
        popExitTransition = {
          fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 12 }
        },
        modifier = Modifier
          .weight(1f)
          .fillMaxSize(),
      ) {
        composable<AppDestination.HomeRoute> {
          HomeScreen(
            playerName = user.displayName,
            onPlayNow = { navController.navigate(AppDestination.GameModeRoute) },
            onOpenLobby = { navigateTopLevel(AppDestination.LobbyRoute) },
            onOpenProfile = { navigateTopLevel(AppDestination.ProfileRoute) },
            onOpenRules = { navigateTopLevel(AppDestination.RulesRoute) },
            onOpenSettings = { navigateTopLevel(AppDestination.SettingsRoute) },
          )
        }

        composable<AppDestination.GameModeRoute> {
          GameModeSelectionScreen(
            onNavigateBack = { navController.popBackStack() },
            onSelectLocalMode = { mode ->
              navController.navigate(AppDestination.PlayerSelectionRoute(mode.name))
            },
            onOpenOnlineLobby = {
              navController.popBackStack(AppDestination.HomeRoute, inclusive = false)
              navigateTopLevel(AppDestination.LobbyRoute)
            },
          )
        }

        composable<AppDestination.PlayerSelectionRoute> { backStackEntry ->
          val args = backStackEntry.toRoute<AppDestination.PlayerSelectionRoute>()
          val mode = GameModeId.entries.firstOrNull { it.name == args.modeName } ?: GameModeId.CLASSIC
          PlayerSelectionScreen(
            mode = mode,
            onNavigateBack = { navController.popBackStack() },
            onStartGame = { playerCount, playerName, avatarId, difficulty ->
              navController.navigate(
                AppDestination.GameBoardRoute(
                  modeName = mode.name,
                  playerCount = playerCount,
                  playerName = playerName,
                  avatarId = avatarId,
                  difficultyName = difficulty.name,
                )
              )
            },
          )
        }

        composable<AppDestination.GameBoardRoute> { backStackEntry ->
          val args = backStackEntry.toRoute<AppDestination.GameBoardRoute>()
          val mode = GameModeId.entries.firstOrNull { it.name == args.modeName } ?: GameModeId.CLASSIC
          val difficulty = com.example.domain.ai.AiDifficulty.entries
            .firstOrNull { it.name == args.difficultyName }
            ?: com.example.domain.ai.AiDifficulty.MEDIUM
          GameBoardScreen(
            mode = mode,
            playerCount = args.playerCount,
            playerName = args.playerName,
            avatarId = args.avatarId,
            difficulty = difficulty,
            onNavigateBack = { navController.popBackStack() },
            onMatchFinished = { isVictory, playerScore, opponentScore ->
              navController.navigate(
                AppDestination.ResultRoute(
                  modeName = mode.name,
                  playerName = args.playerName,
                  avatarId = args.avatarId,
                  playerCount = args.playerCount,
                  isVictory = isVictory,
                  playerScore = playerScore,
                  opponentScore = opponentScore,
                )
              ) {
                popUpTo<AppDestination.GameBoardRoute> { inclusive = true }
              }
            },
          )
        }

        composable<AppDestination.ResultRoute> { backStackEntry ->
          val args = backStackEntry.toRoute<AppDestination.ResultRoute>()
          val mode = GameModeId.entries.firstOrNull { it.name == args.modeName } ?: GameModeId.CLASSIC
          ResultScreen(
            mode = mode,
            playerName = args.playerName,
            avatarId = args.avatarId,
            playerCount = args.playerCount,
            isVictory = args.isVictory,
            playerScore = args.playerScore,
            opponentScore = args.opponentScore,
            onPlayAgain = {
              navController.navigate(
                AppDestination.GameBoardRoute(
                  modeName = mode.name,
                  playerCount = args.playerCount,
                  playerName = args.playerName,
                  avatarId = args.avatarId,
                )
              ) {
                popUpTo<AppDestination.ResultRoute> { inclusive = true }
              }
            },
            onBackToMenu = {
              navController.popBackStack(AppDestination.HomeRoute, inclusive = false)
            },
          )
        }

        composable<AppDestination.LobbyRoute> {
          LobbyScreen(
            onEnterRoom = { roomId ->
              navController.navigate(AppDestination.OnlineRoomRoute(roomId))
            },
            onPlayOffline = {
              navController.navigate(AppDestination.GameModeRoute)
            },
          )
        }
        composable<AppDestination.OnlineRoomRoute> { backStackEntry ->
          val args = backStackEntry.toRoute<AppDestination.OnlineRoomRoute>()
          OnlineRoomScreen(
            roomId = args.roomId,
            onNavigateBackToLobby = {
              navController.popBackStack()
            },
          )
        }
        composable<AppDestination.ProfileRoute> {
          PlayerProfileScreen()
        }
        composable<AppDestination.RulesRoute> {
          RulesScreen()
        }
        composable<AppDestination.SettingsRoute> {
          SettingsScreen()
        }
      }
    }
  }
}
