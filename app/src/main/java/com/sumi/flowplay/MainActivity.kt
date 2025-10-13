package com.sumi.flowplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sumi.flowplay.ui.player.MiniPlayer
import com.sumi.flowplay.ui.player.PlayerScreen
import com.sumi.flowplay.ui.player.PlayerViewModel
import com.sumi.flowplay.ui.playlist.PlaylistScreen
import com.sumi.flowplay.ui.playlist.PlaylistSelectScreen
import com.sumi.flowplay.ui.playlist.PlaylistViewModel
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
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val searchViewModel: SearchViewViewModel = hiltViewModel()
    val playlistViewModel: PlaylistViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        bottomBar = {
            // PlayerScreen 에서는 BottomNavigation + MiniPlayer 숨김
            if (currentDestination != "player" && currentDestination != "playlist_select") {
                Column {
                    MiniPlayer(playerViewModel) {
                        // 클릭 시 PlayerScreen으로 전환
                        navController.navigate("player") {
                            launchSingleTop = true
                        }
                    }
                    BottomNavigationBar(navController)
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "playlist",
            modifier = Modifier.padding(padding)
        ) {
            composable("playlist") {
                PlaylistScreen(
//                    onTrackClick = { track, trackList ->
//                        playerViewModel.play(track, trackList)
//                        navController.navigate("player")
//                    }
                )
            }
            composable("search") {
                SearchScreen(
                    searchViewModel = searchViewModel,
                    onTrackClick = { track, trackList ->
                        playerViewModel.play(track, trackList)
                        navController.navigate("player")
                    }
                )
            }
            composable("player") {
                PlayerScreen(
                    playerViewModel = playerViewModel,
                    onAddToPlaylist = { navController.navigate("playlist_select") }
                )
            }
            composable("playlist_select") {
                PlaylistSelectScreen(
                    playlistViewModel = playlistViewModel,
                    playerViewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("재생목록", "playlist", Icons.AutoMirrored.Filled.List),
        BottomNavItem("검색", "search", Icons.Default.Search),
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo("playlist") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val label: String, val route: String, val icon: ImageVector)