package com.sumi.jamplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sumi.jamplay.service.MusicPlayerService
import com.sumi.jamplay.ui.player.MiniPlayer
import com.sumi.jamplay.ui.player.PlayerScreen
import com.sumi.jamplay.ui.player.PlayerViewModel
import com.sumi.jamplay.ui.playlist.PlaylistDetailScreen
import com.sumi.jamplay.ui.playlist.PlaylistScreen
import com.sumi.jamplay.ui.playlist.PlaylistSelectScreen
import com.sumi.jamplay.ui.playlist.PlaylistViewModel
import com.sumi.jamplay.ui.search.SearchScreen
import com.sumi.jamplay.ui.search.SearchViewViewModel
import com.sumi.jamplay.ui.theme.MusicAppTheme
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

        val navigateToPlayer = savedInstanceState == null && intent?.getBooleanExtra("navigate_to_player", false) == true

        // 음악 서비스 시작 (앱 실행 시 백그라운드에서 재생 준비)
        Intent(this, MusicPlayerService::class.java).also { intent ->
            startService(intent)
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(), Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(), Color.Transparent.toArgb()
            )
        )

        setContent {
            MusicAppTheme {
                val navController = rememberNavController()
                MainScreen(navController, playerViewModel)
                LaunchedEffect(navigateToPlayer) {
                    if (navigateToPlayer) {
                        navController.navigate("player")
                    }
                }
            }
        }
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
                        PlayerViewModel.PlayerCommand.ToggleRepeat -> service.toggleRepeat()
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController, playerViewModel: PlayerViewModel) {
    val searchViewModel: SearchViewViewModel = hiltViewModel()
    val playlistViewModel: PlaylistViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // BottomNavigation + MiniPlayer 숨김
            if (currentDestination != "player" && currentDestination != "playlistSelect") {
                Column {
                    val isHomeScreen = currentDestination != "playlistDetail/{playlistId}"

                    MiniPlayer(viewModel = playerViewModel) {
                        // 클릭 시 PlayerScreen으로 전환
                        navController.navigate("player") {
                            launchSingleTop = true
                        }
                    }
                    if (isHomeScreen) {
                        BottomNavigationBar(navController)
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "playlist",
            modifier = Modifier.background(Color.Transparent)
        ) {
            composable("playlist") {
                PlaylistScreen(
                    padding = padding,
                    playlistViewModel = playlistViewModel,
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlistDetail/$playlistId")
                    }
                )
            }
            composable("search") {
                SearchScreen(
                    padding = padding,
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
                    padding = padding,
                    playerViewModel = playerViewModel,
                    playlistViewModel = playlistViewModel,
                    onAddToPlaylist = { navController.navigate("playlistSelect") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("playlistSelect") {
                PlaylistSelectScreen(
                    padding = padding,
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
                    padding = padding,
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

    NavigationBar(
//        containerColor = Color.Transparent,
//        tonalElevation = 0.dp
    ) {
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