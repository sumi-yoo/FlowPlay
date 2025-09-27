package com.sumi.flowplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sumi.flowplay.data.model.TrackDto
import com.sumi.flowplay.ui.play.PlayerScreen
import com.sumi.flowplay.ui.search.SearchScreen
import com.sumi.flowplay.ui.search.SearchViewViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    NavHost(navController = navController, startDestination = "search") {
                        composable("search") { backStackEntry ->
                            val vm: SearchViewViewModel = hiltViewModel(backStackEntry)

                            SearchScreen(
                                viewModel = vm,
                                onTrackClick = { track, trackList ->
                                    backStackEntry.savedStateHandle.set("track", track)
                                    backStackEntry.savedStateHandle.set("trackList", ArrayList(trackList))
                                    navController.navigate("player")
                                }
                            )
                        }

                        composable("player") {
                            val track = navController.previousBackStackEntry
                                ?.savedStateHandle?.get<TrackDto>("track") ?: return@composable
                            val trackList = navController.previousBackStackEntry
                                ?.savedStateHandle?.get<ArrayList<TrackDto>>("trackList") ?: arrayListOf()

                            PlayerScreen(track = track, trackList = trackList)
                        }
                    }
                }
            }
        }
    }
}