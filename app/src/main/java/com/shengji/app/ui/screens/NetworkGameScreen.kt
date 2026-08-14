package com.shengji.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shengji.app.data.*
import com.shengji.app.enums.*
import com.shengji.app.ui.utils.rememberScreenSize
import com.shengji.app.viewmodel.NetworkGameViewModel
import kotlinx.coroutines.delay

/**
 * 联机游戏界面 - 老人友好
 *
 * 状态流转：
 * CONNECTING → CONNECTED(等待) → IN_GAME(游戏中)
 */
@Composable
fun NetworkGameScreen(
    viewModel: NetworkGameViewModel,
    onStartGame: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val playerNames by viewModel.playerNames.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val screenSize = rememberScreenSize()

    var showExitDialog by remember { mutableStateOf(false) }

    // 大字体
    val titleSize = (screenSize.titleTextSize.value * 1.3f).sp
    val buttonSize = (screenSize.buttonTextSize.value * 1.2f).sp
    val labelSize = (screenSize.statusTextSize.value * 1.2f).sp

    when (connectionState) {
        NetworkGameViewModel.ConnectionState.DISCONNECTED -> {
            ErrorScreen(
                message = errorMessage ?: "连接断开了",
                screenSize = screenSize,
                onBack = onBack
            )
        }
        NetworkGameViewModel.ConnectionState.CONNECTING -> {
            ConnectingScreen(screenSize = screenSize)
        }
        NetworkGameViewModel.ConnectionState.CONNECTED -> {
            WaitingScreen(
                playerNames = playerNames,
                screenSize = screenSize,
                onStartGame = onStartGame,
                onExit = { showExitDialog = true }
            )
        }
        NetworkGameViewModel.ConnectionState.IN_GAME -> {
            GamePlayScreen(
                state = state,
                viewModel = viewModel,
                screenSize = screenSize,
                onExit = { showExitDialog = true }
            )
        }
    }

    // 退出确认
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "退出游戏",
                    fontSize = buttonSize,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定要退出吗？\n其他玩家会收到通知",
                    fontSize = labelSize
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.disconnect()
                        onBack()
                    },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("确定退出", fontSize = labelSize)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("继续玩", fontSize = labelSize)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════
// 各状态界面
// ═══════════════════════════════════════════

/**
 * 连接中界面
 */
@Composable
private fun ConnectingScreen(screenSize: com.shengji.app.ui.utils.ScreenSize) {
    val titleSize = (screenSize.titleTextSize.value * 1.3f).sp
    val labelSize = (screenSize.statusTextSize.value * 1.2f).sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(72.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "正在连接...",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "请稍等",
                fontSize = labelSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 连接错误界面
 */
@Composable
private fun ErrorScreen(
    message: String,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    onBack: () -> Unit
) {
    val titleSize = (screenSize.titleTextSize.value * 1.3f).sp
    val buttonSize = (screenSize.buttonTextSize.value * 1.2f).sp
    val labelSize = (screenSize.statusTextSize.value * 1.2f).sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "😞",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "连接失败",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = labelSize,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(screenSize.buttonHeightLarge * 1.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "返回",
                    fontSize = buttonSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 等待房间界面 - 极简设计
 */
@Composable
private fun WaitingScreen(
    playerNames: List<String>,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    onStartGame: () -> Unit,
    onExit: () -> Unit
) {
    val titleSize = (screenSize.titleTextSize.value * 1.3f).sp
    val buttonSize = (screenSize.buttonTextSize.value * 1.2f).sp
    val labelSize = (screenSize.statusTextSize.value * 1.2f).sp

    // 闪烁动画
    var dotVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(800)
            dotVisible = !dotVisible
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部返回
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onExit,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("← 退出", fontSize = buttonSize, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // 房间状态大图标
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (dotVisible) "📡" else "📻",
                fontSize = 48.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "等待朋友加入",
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "请确保朋友在同一 WiFi 下",
            fontSize = labelSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 玩家列表 - 大卡片
        Text(
            text = "已加入的人：",
            fontSize = labelSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4个玩家位置
        for (i in 0 until 4) {
            PlayerSlot(
                index = i,
                name = playerNames.getOrNull(i),
                isHost = i == 0,
                fontSize = buttonSize,
                screenSize = screenSize
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // 开始按钮
        val canStart = playerNames.size >= 2
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(screenSize.buttonHeightLarge * 1.3f),
            shape = RoundedCornerShape(16.dp),
            enabled = canStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canStart) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = if (canStart) "🎮 开始游戏" else "等待朋友加入...",
                fontSize = buttonSize,
                fontWeight = FontWeight.Bold,
                color = if (canStart) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!canStart) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "至少需要2人才能开始",
                fontSize = labelSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 玩家位置槽位
 */
@Composable
private fun PlayerSlot(
    index: Int,
    name: String?,
    isHost: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    screenSize: com.shengji.app.ui.utils.ScreenSize
) {
    val isOccupied = name != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOccupied)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOccupied) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isOccupied) "👤" else "⏳",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 名字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOccupied) name!! else "等待加入...",
                    fontSize = fontSize,
                    fontWeight = if (isOccupied) FontWeight.Bold else FontWeight.Normal,
                    color = if (isOccupied)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // 标签
            if (isHost) {
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = " 房主 ",
                        fontSize = (fontSize.value * 0.75f).sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (isOccupied) {
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = " 已加入 ",
                        fontSize = (fontSize.value * 0.75f).sp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 游戏进行中界面 - 简化版
 */
@Composable
private fun GamePlayScreen(
    state: GameState,
    viewModel: NetworkGameViewModel,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    onExit: () -> Unit
) {
    val selectedBottomIndices by viewModel.selectedBottomIndices.collectAsState()
    val selectedHandIndices by viewModel.selectedHandIndices.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var showError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            showError = it
            delay(2000)
            showError = null
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(state.phase, state.roundNumber, state.isLeadPhase, state.currentTurnIndex) {
        selectedIndices = emptySet()
    }

    val humanPlayer = remember(state.players) { state.players.getOrNull(0) }
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp

    val playsByVisual = remember(state.currentRoundPlays, state.lastRoundPlays) {
        if (state.currentRoundPlays.isNotEmpty()) state.currentRoundPlays.toMap()
        else state.lastRoundPlays.toMap()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 状态栏
            SimpleStatusBar(state = state, screenSize = screenSize, onExit = onExit)

            // 游戏主区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // AI对家
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = screenSize.spacingSmall),
                    contentAlignment = Alignment.Center
                ) {
                    state.players.getOrNull(2)?.let {
                        com.shengji.app.ui.components.AiHandView(
                            cardCount = it.handCardCount,
                            label = "对家 ${it.displayName}",
                            cardWidth = screenSize.aiCardWidth,
                            isCurrentTurn = state.currentTurnIndex == 2,
                            screenSize = screenSize
                        )
                    }
                }

                // 中间行
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.width(screenSize.aiHandAreaWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        state.players.getOrNull(3)?.let {
                            com.shengji.app.ui.components.AiHandView(
                                cardCount = it.handCardCount,
                                label = "左家 ${it.displayName}",
                                cardWidth = screenSize.aiCardWidth * 0.85f,
                                isCurrentTurn = state.currentTurnIndex == 3,
                                screenSize = screenSize
                            )
                        }
                    }

                    PlayArea(
                        state = state,
                        playsByVisual = playsByVisual,
                        cardWidth = screenSize.playAreaCardWidth,
                        screenSize = screenSize,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier.width(screenSize.aiHandAreaWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        state.players.getOrNull(1)?.let {
                            com.shengji.app.ui.components.AiHandView(
                                cardCount = it.handCardCount,
                                label = "右家 ${it.displayName}",
                                cardWidth = screenSize.aiCardWidth * 0.85f,
                                isCurrentTurn = state.currentTurnIndex == 1,
                                screenSize = screenSize
                            )
                        }
                    }
                }

                // 底牌
                if (state.phase == GamePhase.DIG_BOTTOM ||
                    (state.phase >= GamePhase.ROUND_SETTLE && state.bottomCards.isNotEmpty())) {
                    BottomCardsArea(state = state, cardWidth = screenSize.bottomCardWidth)
                }

                // 人类手牌
                if (!state.isBottomExchangePhase) {
                    humanPlayer?.let { hp ->
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = screenSize.spacingSmall),
                            contentAlignment = Alignment.Center
                        ) {
                            com.shengji.app.ui.components.HandCardsRow(
                                cards = hp.sortedHand(state.currentLevel),
                                selectedIndices = selectedIndices,
                                faceUp = true,
                                cardWidth = screenSize.cardWidth,
                                overlap = screenSize.cardWidth * screenSize.cardOverlapRatio,
                                maxWidth = screenWidth - 16.dp,
                                onCardClick = if (hp.isAiControlled) null else { idx ->
                                    selectedIndices = if (idx in selectedIndices) selectedIndices - idx else selectedIndices + idx
                                }
                            )
                        }
                    }
                }

                // 换底牌
                if (state.isBottomExchangePhase) {
                    BottomExchangeArea(
                        bottomCards = state.bottomCards,
                        handCards = humanPlayer?.sortedHand(state.currentLevel) ?: emptyList(),
                        selectedBottomIndices = selectedBottomIndices,
                        selectedHandIndices = selectedHandIndices,
                        onSelectCard = { idx, isBottom -> viewModel.handleEvent(GameEvent.SelectForExchange(idx, isBottom)) },
                        onConfirm = { viewModel.handleEvent(GameEvent.ConfirmBottomExchange) },
                        onCancel = { viewModel.handleEvent(GameEvent.CancelBottomExchange) },
                        cardWidth = screenSize.exchangeCardWidth,
                        screenSize = screenSize,
                        modifier = Modifier.padding(vertical = screenSize.spacingSmall)
                    )
                }
            }

            // 操作按钮（换底牌阶段隐藏，避免挤占换牌按钮空间）
            if (!state.isBottomExchangePhase) {
                SimpleActionButtons(
                    state = state,
                    selectedIndices = selectedIndices,
                    humanPlayer = humanPlayer,
                    viewModel = viewModel,
                    screenSize = screenSize,
                    onDeselect = { selectedIndices = emptySet() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = screenSize.spacingMedium)
                )
            }
        }

        // 错误提示
        showError?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 轮次提示
        state.roundTransitionMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 分数
        if (state.phase == GamePhase.PLAYING || state.phase == GamePhase.DIG_BOTTOM) {
            val attackerTeam = 1 - state.dealerTeam
            val attackerScore = state.teamScores.getOrElse(attackerTeam) { 0 }
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp).align(Alignment.TopEnd),
                contentAlignment = Alignment.CenterEnd
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "闲家: ${attackerScore}分",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * 简化状态栏
 */
@Composable
private fun SimpleStatusBar(
    state: GameState,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    onExit: () -> Unit
) {
    val labelSize = (screenSize.statusTextSize.value * 1.1f).sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onExit,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("← 退出", fontSize = labelSize)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 联机标识
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "联机", fontSize = labelSize, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "级牌: ${state.currentLevel.display}",
            fontSize = labelSize,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "主: ${state.mainSuit?.displayName ?: "无主"}",
            fontSize = labelSize
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "A:${state.teamScores.getOrElse(0) { 0 }} B:${state.teamScores.getOrElse(1) { 0 }}",
            fontSize = labelSize,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 简化操作按钮
 */
@Composable
private fun SimpleActionButtons(
    state: GameState,
    selectedIndices: Set<Int>,
    humanPlayer: Player?,
    viewModel: NetworkGameViewModel,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    onDeselect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedCards = remember(humanPlayer, selectedIndices, state.currentLevel) {
        humanPlayer?.let { hp ->
            val hand = hp.sortedHand(state.currentLevel)
            selectedIndices.filter { it in hand.indices }.map { hand[it] }
        } ?: emptyList()
    }

    val isHumanTurn = state.currentTurnIndex == 0
    val hasSelectedCards = selectedCards.isNotEmpty()
    val buttonSize = (screenSize.buttonTextSize.value * 1.1f).sp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.phase) {
            GamePhase.DEAL -> {
                Text(
                    text = if (state.mainSuit != null) "已叫主" else "发牌中...",
                    fontSize = buttonSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            GamePhase.CALL_MAIN -> {
                Text(text = "等待叫主", fontSize = buttonSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GamePhase.PLAYING -> {
                if (state.isBottomExchangePhase) {
                    Text(text = "选择底牌交换", fontSize = buttonSize, color = MaterialTheme.colorScheme.primary)
                } else if (hasSelectedCards) {
                    OutlinedButton(
                        onClick = onDeselect,
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("取消", fontSize = buttonSize)
                    }
                    Button(
                        onClick = {
                            if (state.isLeadPhase) viewModel.handleEvent(GameEvent.LeadPlay(selectedCards))
                            else viewModel.handleEvent(GameEvent.FollowPlay(selectedCards))
                        },
                        enabled = isHumanTurn,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (state.isLeadPhase) "出牌" else "跟牌",
                            fontSize = buttonSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = if (isHumanTurn) {
                            if (state.isLeadPhase) "👆 选牌出牌" else "👆 选牌跟牌"
                        } else "等待...",
                        fontSize = buttonSize,
                        color = if (isHumanTurn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            GamePhase.DIG_BOTTOM -> {
                Button(
                    onClick = { viewModel.handleEvent(GameEvent.ConfirmDigBottom) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("抠底", fontSize = buttonSize, fontWeight = FontWeight.Bold)
                }
            }
            GamePhase.ROUND_SETTLE -> {
                Button(
                    onClick = { viewModel.handleEvent(GameEvent.ConfirmSettle) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("继续", fontSize = buttonSize, fontWeight = FontWeight.Bold)
                }
            }
            GamePhase.GAME_OVER -> {
                Button(
                    onClick = { viewModel.handleEvent(GameEvent.Restart) },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("再来一局", fontSize = buttonSize, fontWeight = FontWeight.Bold)
                }
            }
            else -> {}
        }
    }
}
