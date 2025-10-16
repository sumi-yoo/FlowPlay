package com.sumi.flowplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sumi.flowplay.service.MusicPlayerService
import com.sumi.flowplay.ui.player.MiniPlayer
import com.sumi.flowplay.ui.player.PlayerScreen
import com.sumi.flowplay.ui.player.PlayerViewModel
import com.sumi.flowplay.ui.playlist.PlaylistDetailScreen
import com.sumi.flowplay.ui.playlist.PlaylistScreen
import com.sumi.flowplay.ui.playlist.PlaylistSelectScreen
import com.sumi.flowplay.ui.playlist.PlaylistViewModel
import com.sumi.flowplay.ui.search.SearchScreen
import com.sumi.flowplay.ui.search.SearchViewViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var musicService: MusicPlayerService? = null
    private var isBound = false

    private val playerViewModel: PlayerViewModel by viewModels()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val bd = binder as MusicPlayerService.LocalBinder
            musicService = bd.getService()
            isBound = true
            musicService?.let { playerViewModel.bindService(it) }
            observePlayerCommands()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MainScreen(playerViewModel)
            }
        }

        // 음악 서비스 시작 (앱 실행 시 백그라운드에서 재생 준비)
        val serviceIntent = Intent(this, MusicPlayerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicPlayerService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun observePlayerCommands() {
        lifecycleScope.launch {
            playerViewModel.playerCommand.collect { command ->
                musicService?.let { service ->
                    when (command) {
                        is PlayerViewModel.PlayerCommand.Play -> service.play(command.track, command.tracks)
                        PlayerViewModel.PlayerCommand.TogglePlay -> service.togglePlayPause()
                        PlayerViewModel.PlayerCommand.SkipNext -> service.skipNext()
                        PlayerViewModel.PlayerCommand.SkipPrevious -> service.skipPrevious()
                        is PlayerViewModel.PlayerCommand.Seek -> service.seekTo(command.position)
                        PlayerViewModel.PlayerCommand.ToggleShuffle -> service.toggleShuffle()
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(playerViewModel: PlayerViewModel) {
    val navController = rememberNavController()
    val searchViewModel: SearchViewViewModel = hiltViewModel()
    val playlistViewModel: PlaylistViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        bottomBar = {
            // BottomNavigation + MiniPlayer 숨김
            if (currentDestination != "player" && currentDestination != "playlistSelect") {
                Column {
                    MiniPlayer(playerViewModel) {
                        // 클릭 시 PlayerScreen으로 전환
                        navController.navigate("player") {
                            launchSingleTop = true
                        }
                    }
                    if (currentDestination != "playlistDetail/{playlistId}") {
                        BottomNavigationBar(navController)
                    }
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
                    playlistViewModel = playlistViewModel,
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlistDetail/$playlistId")
                    }
                )
            }
            composable("search") {
                SearchScreen(
                    searchViewModel = searchViewModel,
                    playerViewModel = playerViewModel,
                    onTrackClick = { track, trackList ->
                        playerViewModel.play(track, trackList)
                        navController.navigate("player")
                    }
                )
            }
            composable("player") {
                PlayerScreen(
                    playerViewModel = playerViewModel,
                    playlistViewModel = playlistViewModel,
                    onAddToPlaylist = { navController.navigate("playlistSelect") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("playlistSelect") {
                PlaylistSelectScreen(
                    playlistViewModel = playlistViewModel,
                    playerViewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "playlistDetail/{playlistId}",
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L

                LaunchedEffect(playlistId) {
                    playlistViewModel.selectPlaylistById(playlistId)
                }

                PlaylistDetailScreen(
                    playlistId = playlistId,
                    playlistViewModel = playlistViewModel,
                    playerViewModel = playerViewModel,
                    onTrackClick = { track, trackList ->
                        playerViewModel.play(track, trackList)
                        navController.navigate("player")
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(stringResource(R.string.playlist_title), "playlist", Icons.AutoMirrored.Filled.List),
        BottomNavItem(stringResource(R.string.search_title), "search", Icons.Default.Search),
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