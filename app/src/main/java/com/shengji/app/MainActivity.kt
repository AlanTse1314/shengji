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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shengji.app.ui.navigation.Screen
import com.shengji.app.ui.screens.*
import com.shengji.app.theme.ShengJiTheme
import com.shengji.app.viewmodel.GameViewModel

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
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShengJiApplication
    val database = app.database

    // 注册游戏结束回调
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
                        summary = "庄家队伍${winnerTeam}，级牌$finalLevel"
                    )
                )
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNewGame = { navController.navigate(Screen.Game.route) },
                onMultiplayer = { navController.navigate(Screen.Multiplayer.route) },
                onHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(Screen.Multiplayer.route) {
            MultiplayerScreen(
                onBack = { navController.popBackStack() },
                onCreateRoom = {
                    // TODO: 创建房间后进入联机游戏界面
                    navController.navigate(Screen.Game.route)
                },
                onJoinRoom = { hostAddress ->
                    // TODO: 加入房间后进入联机游戏界面
                    navController.navigate(Screen.Game.route)
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                database = database,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                viewModel = gameViewModel,
                onBack = {
                    gameViewModel.handleEvent(com.shengji.app.data.GameEvent.Restart)
                    navController.popBackStack()
                }
            )
        }
    }
}
