package com.sumi.flowplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sumi.flowplay.service.MusicPlayerService
import com.sumi.flowplay.ui.player.MiniPlayer
import com.sumi.flowplay.ui.player.PlayerScreen
import com.sumi.flowplay.ui.player.PlayerViewModel
import com.sumi.flowplay.ui.playlist.PlaylistScreen
import com.sumi.flowplay.ui.search.SearchScreen
import com.sumi.flowplay.ui.search.SearchViewViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var musicService: MusicPlayerService? = null
    private var isBound = false

    private val playerViewModel: PlayerViewModel by viewModels()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val binder = binder as MusicPlayerService.LocalBinder
            musicService = binder.getService()
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
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicPlayerService::class.java).also { intent ->
            startService(intent) // 포그라운드 재생 유지
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
        Log.d("테스트", "observePlayerCommands")
        lifecycleScope.launch {
            playerViewModel.playerCommand.collect { command ->
                Log.d("테스트", "command $command")
                musicService?.let { service ->
                    when (command) {
                        is PlayerViewModel.PlayerCommand.Play -> service.play(command.track, command.tracks)
                        PlayerViewModel.PlayerCommand.TogglePlay -> service.togglePlayPause()
                        PlayerViewModel.PlayerCommand.SkipNext -> service.skipNext()
                        PlayerViewModel.PlayerCommand.SkipPrevious -> service.skipPrevious()
                        is PlayerViewModel.PlayerCommand.Seek -> service.seekTo(command.position)
                    }
                } ?: Log.d("테스트", "Service not bound yet")
            }
        }
    }
}

@Composable
fun MainScreen(playerViewModel: PlayerViewModel) {
    val navController = rememberNavController()
    val searchViewModel: SearchViewViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        bottomBar = {
            // PlayerScreen 에서는 BottomNavigation + MiniPlayer 숨김
            if (currentDestination != "player") {
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
                PlayerScreen(playerViewModel)
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