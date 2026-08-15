package com.jasond.homeflix.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jasond.homeflix.ui.screens.DetailsScreen
import com.jasond.homeflix.ui.screens.HomeScreen
import com.jasond.homeflix.ui.screens.PlayerScreen

@Composable
fun HomeFlixApp(homeViewModel: HomeViewModel = viewModel()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onMovieSelected = { movieId -> navController.navigate("details/$movieId") },
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
                onLoadProgress = { homeViewModel.loadProgress(movieId) },
                onPlayMovie = { id, startOver -> navController.navigate("player/$id?startOver=$startOver") },
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
