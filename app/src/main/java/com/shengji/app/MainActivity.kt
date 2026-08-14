package com.shengji.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shengji.app.net.TcpManager
import com.shengji.app.net.UdpDiscovery
import com.shengji.app.ui.navigation.Screen
import com.shengji.app.ui.screens.*
import com.shengji.app.theme.ShengJiTheme
import com.shengji.app.viewmodel.GameViewModel
import com.shengji.app.viewmodel.NetworkGameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShengJiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShengJiApp()
                }
            }
        }
    }
}

@Composable
fun ShengJiApp() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()
    val networkGameViewModel: NetworkGameViewModel = viewModel()
    val app = LocalContext.current.applicationContext as ShengJiApplication
    val database = app.database
    val context = LocalContext.current

    // 网络组件（每次进入联机页面时创建新实例）
    val tcpManager = remember { TcpManager() }
    val udpDiscovery = remember { UdpDiscovery(context) }

    // 初始化数据库DAO
    LaunchedEffect(database) {
        gameViewModel.setProgressDao(database.gameProgressDao())
    }

    // 注册单机游戏结束回调
    LaunchedEffect(database) {
        gameViewModel.onGameOver = { startTime, winnerTeam, finalLevel, team0Score, team1Score ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                database.gameRecordDao().insertRecord(
                    com.shengji.app.db.GameRecordEntity(
                        startTime = startTime,
                        endTime = System.currentTimeMillis(),
                        winnerTeam = winnerTeam,
                        finalLevel = finalLevel,
                        team0Score = team0Score,
                        team1Score = team1Score,
                        summary = "庄家队伍$winnerTeam，级牌$finalLevel"
                    )
                )
            }
        }
    }

    // 监听游戏状态变化，自动保存进度
    val gameState by gameViewModel.state.collectAsState()
    LaunchedEffect(gameState) {
        // 游戏进行中时自动保存（延迟保存，避免频繁写入）
        if (gameState.phase != com.shengji.app.enums.GamePhase.IDLE &&
            gameState.phase != com.shengji.app.enums.GamePhase.GAME_OVER) {
            kotlinx.coroutines.delay(1000) // 延迟1秒保存
            gameViewModel.saveProgress()
        }
    }

    // 是否有保存的进度
    val hasSavedProgress by gameViewModel.hasSavedProgress.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // ═══ 首页 ═══
        composable(Screen.Home.route) {
            HomeScreen(
                hasSavedProgress = hasSavedProgress,
                onContinueGame = {
                    gameViewModel.restoreProgress()
                    navController.navigate(Screen.Game.route)
                },
                onNewGame = {
                    // 开始新游戏前清除旧进度
                    gameViewModel.handleEvent(com.shengji.app.data.GameEvent.Restart)
                    navController.navigate(Screen.Game.route)
                },
                onMultiplayer = { navController.navigate(Screen.Multiplayer.route) },
                onHistory = { navController.navigate(Screen.History.route) }
            )
        }

        // ═══ 联机大厅 ═══
        composable(Screen.Multiplayer.route) {
            MultiplayerScreen(
                onBack = {
                    networkGameViewModel.disconnect()
                    navController.popBackStack()
                },
                onCreateRoom = {
                    val deviceName = android.os.Build.MODEL ?: "未知设备"
                    networkGameViewModel.createRoom(deviceName, tcpManager, udpDiscovery)
                    navController.navigate(Screen.NetworkGame.route)
                },
                onJoinRoom = { hostAddress ->
                    val deviceName = android.os.Build.MODEL ?: "未知设备"
                    networkGameViewModel.joinRoom(hostAddress, deviceName, tcpManager)
                    navController.navigate(Screen.NetworkGame.route)
                }
            )
        }

        // ═══ 历史记录 ═══
        composable(Screen.History.route) {
            HistoryScreen(
                database = database,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══ 单机游戏 ═══
        composable(Screen.Game.route) {
            GameScreen(
                viewModel = gameViewModel,
                onBack = {
                    // 退出时保存进度
                    gameViewModel.saveProgress()
                    navController.popBackStack()
                }
            )
        }

        // ═══ 联机游戏 ═══
        composable(Screen.NetworkGame.route) {
            NetworkGameScreen(
                viewModel = networkGameViewModel,
                onStartGame = {
                    networkGameViewModel.handleEvent(com.shengji.app.data.GameEvent.Deal)
                },
                onBack = {
                    networkGameViewModel.disconnect()
                    networkGameViewModel.handleEvent(com.shengji.app.data.GameEvent.Restart)
                    navController.popBackStack()
                }
            )
        }
    }
}
