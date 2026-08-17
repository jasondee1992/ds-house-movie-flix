package com.jasond.homeflix.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jasond.homeflix.ui.screens.DetailsScreen
import com.jasond.homeflix.ui.screens.HomeScreen
import com.jasond.homeflix.ui.screens.PlayerScreen
import com.jasond.homeflix.ui.screens.SearchScreen
import com.jasond.homeflix.ui.screens.LibraryGridScreen
import com.jasond.homeflix.ui.screens.ServerSetupScreen
import com.jasond.homeflix.ui.screens.SplashScreen
import com.jasond.homeflix.ui.theme.TvMotion
import com.jasond.homeflix.data.HomeRepository
import com.jasond.homeflix.data.ServerPreferences
import com.jasond.homeflix.data.MyListStore
import com.jasond.homeflix.data.remote.ApiClient
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withTimeout

private enum class AppStage { SPLASH, CHECKING_SERVER, SERVER_SETUP, LIBRARY }

@Composable
fun HomeFlixApp() {
    val context = LocalContext.current
    val serverPreferences = remember(context) { ServerPreferences(context) }
    var stage by remember { mutableStateOf(AppStage.SPLASH) }
    var sessionServerUrl by remember { mutableStateOf<String?>(null) }
    when (stage) {
        AppStage.SPLASH -> SplashScreen {
            val savedUrl = serverPreferences.loadServerUrl()
            if (savedUrl == null) stage = AppStage.SERVER_SETUP
            else {
                sessionServerUrl = savedUrl
                stage = AppStage.CHECKING_SERVER
            }
        }
        AppStage.CHECKING_SERVER -> {
            SplashScreen { }
            LaunchedEffect(sessionServerUrl) {
                val url = sessionServerUrl ?: return@LaunchedEffect
                val connected = runCatching {
                    val response = withTimeout(6_000) { ApiClient.createService(url).health() }
                    response.status == "ok" && response.service == "homeflix"
                }.getOrDefault(false)
                if (connected) {
                    ApiClient.configure(url)
                    stage = AppStage.LIBRARY
                } else stage = AppStage.SERVER_SETUP
            }
        }
        AppStage.SERVER_SETUP -> ServerSetupScreen(initialAddress = sessionServerUrl.orEmpty()) { url ->
            serverPreferences.saveServerUrl(url)
            sessionServerUrl = url
            stage = AppStage.LIBRARY
        }
        AppStage.LIBRARY -> key(sessionServerUrl) {
            val factory = remember(sessionServerUrl) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        HomeViewModel(HomeRepository(ApiClient.service), MyListStore(context)) as T
                }
            }
            val homeViewModel: HomeViewModel = viewModel(key = "home-${sessionServerUrl.orEmpty()}", factory = factory)
            HomeFlixLibrary(homeViewModel)
        }
    }
}

@Composable
private fun HomeFlixLibrary(homeViewModel: HomeViewModel) {
    val navController = rememberNavController()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val movies = (homeState as? HomeUiState.Success)?.movies.orEmpty()
    val myListIds by homeViewModel.myListIds.collectAsStateWithLifecycle()
    val navigateSidebar: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(tween(TvMotion.ScreenMillis)) + scaleIn(tween(TvMotion.ScreenMillis), initialScale = .985f) },
        exitTransition = { fadeOut(tween(TvMotion.ScreenMillis)) + scaleOut(tween(TvMotion.ScreenMillis), targetScale = 1.015f) },
        popEnterTransition = { fadeIn(tween(TvMotion.ScreenMillis)) },
        popExitTransition = { fadeOut(tween(TvMotion.ScreenMillis)) },
    ) {
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onMovieSelected = { movieId -> navController.navigate("details/$movieId") },
                onPlayMovie = { movieId -> navController.navigate("player/$movieId?startOver=false") },
                myListIds = myListIds,
                onToggleMyList = homeViewModel::toggleMyList,
                onNavigate = navigateSidebar,
            )
        }
        composable("search") {
            SearchScreen(homeViewModel, { navController.navigate("details/$it") }, navigateSidebar)
        }
        composable("movies") {
            LibraryGridScreen("movies", "Movies", movies, { navController.navigate("details/$it") }, navigateSidebar)
        }
        composable("my-list") {
            LibraryGridScreen("my-list", "My List", movies.filter { it.id in myListIds },
                { navController.navigate("details/$it") }, navigateSidebar)
        }
        composable("settings") {
            LibraryGridScreen(
                "settings", "Settings", emptyList(), { navController.navigate("details/$it") }, navigateSidebar,
                emptyTitle = "More settings coming soon",
                emptyMessage = "DS Cinema settings will appear here as they become available.",
            )
        }
        composable(
            route = "details/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.LongType }),
        ) { entry ->
            val movieId = entry.arguments?.getLong("movieId") ?: -1L
            val progress by homeViewModel.progress.collectAsStateWithLifecycle()
            DetailsScreen(
                movie = homeViewModel.movieById(movieId),
                progress = progress[movieId],
                relatedMovies = homeViewModel.relatedMovies(movieId),
                isInMyList = movieId in myListIds,
                onToggleMyList = { homeViewModel.toggleMyList(movieId) },
                onLoadProgress = { homeViewModel.loadProgress(movieId) },
                onPlayMovie = { id, startOver -> navController.navigate("player/$id?startOver=$startOver") },
                onMovieSelected = { id -> navController.navigate("details/$id") },
                onBack = navController::popBackStack,
            )
        }
        composable(
            route = "player/{movieId}?startOver={startOver}",
            arguments = listOf(
                navArgument("movieId") { type = NavType.LongType },
                navArgument("startOver") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val movieId = entry.arguments?.getLong("movieId") ?: -1L
            PlayerScreen(movie = homeViewModel.movieById(movieId), onBack = {
                homeViewModel.loadMovies()
                navController.popBackStack()
            })
        }
    }
}
