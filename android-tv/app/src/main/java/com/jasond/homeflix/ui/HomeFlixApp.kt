package com.jasond.homeflix.ui

import androidx.compose.runtime.Composable
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jasond.homeflix.ui.screens.DetailsScreen
import com.jasond.homeflix.ui.screens.HomeScreen

@Composable
fun HomeFlixApp(homeViewModel: HomeViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
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
            DetailsScreen(
                movie = homeViewModel.movieById(movieId),
                onPlayMovie = { Toast.makeText(context, "Playback will be enabled in Phase 4", Toast.LENGTH_SHORT).show() },
                onBack = navController::popBackStack,
            )
        }
    }
}
