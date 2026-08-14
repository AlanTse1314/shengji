package com.shengji.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shengji.app.data.*
import com.shengji.app.enums.*
import com.shengji.app.ui.components.*
import com.shengji.app.ui.utils.rememberScreenSize
import com.shengji.app.viewmodel.GameViewModel
import kotlinx.coroutines.delay

/**
 * 亮牌气泡数据
 */
private data class CallMainBubbleData(
    val playerIndex: Int,
    val playerName: String,
    val cardName: String,
    val suitName: String
)

/**
 * 游戏主对局界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val validationMsg by viewModel.validationMessage.collectAsState()
    val soundEvent by viewModel.soundEvent.collectAsState()
    val selectedBottomIndices by viewModel.selectedBottomIndices.collectAsState()
    val selectedHandIndices by viewModel.selectedHandIndices.collectAsState()
    val context = LocalContext.current

    // 音效管理器
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) { onDispose { soundManager.release() } }

    // 音效触发
    LaunchedEffect(soundEvent) {
        when (soundEvent) {
            is GameViewModel.SoundEvent.PlayCard -> soundManager.playCard()
            is GameViewModel.SoundEvent.Win -> soundManager.win()
            is GameViewModel.SoundEvent.Lose -> soundManager.lose()
            is GameViewModel.SoundEvent.Invalid -> soundManager.invalid()
            null -> {}
        }
        viewModel.clearSoundEvent()
    }

    // 错误提示状态
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(validationMsg) {
        validationMsg?.let {
            errorMessage = it
            delay(2000)
            errorMessage = null
            viewModel.clearValidationMessage()
        }
    }

    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var showCallMainDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCardConfirmDialog by remember { mutableStateOf<Card?>(null) }

    val humanPlayer = remember(state.players) { state.players.getOrNull(0) }
    val aiRight = remember(state.players) { state.players.getOrNull(1) }
    val aiTop = remember(state.players) { state.players.getOrNull(2) }
    val aiLeft = remember(state.players) { state.players.getOrNull(3) }

    // ═══ 出牌超时提示 ═══
    var showTurnHint by remember { mutableStateOf(false) }
    // 监听轮到人类玩家出牌，3秒后弹出提示（换底牌阶段不提示）
    LaunchedEffect(state.currentTurnIndex, state.isLeadPhase, state.phase, state.isBottomExchangePhase) {
        showTurnHint = false
        if (state.phase == GamePhase.PLAYING &&
            state.currentTurnIndex == 0 &&
            !state.isBottomExchangePhase &&
            !(humanPlayer?.isAiControlled ?: false)
        ) {
            delay(3000)
            // 再次检查仍然是人类的回合
            if (state.currentTurnIndex == 0 && state.phase == GamePhase.PLAYING && !state.isBottomExchangePhase) {
                showTurnHint = true
            }
        }
    }

    // ═══ 亮牌气泡提示 ═══
    var callMainBubble by remember { mutableStateOf<CallMainBubbleData?>(null) }
    LaunchedEffect(state.callMainDisplayCards, state.callMainDisplayPlayer) {
        val cards = state.callMainDisplayCards
        val player = state.callMainDisplayPlayer
        val suit = state.callMainDisplaySuit
        if (cards != null && player != null && suit != null) {
            // 找到亮牌玩家的位置索引
            val playerIndex = state.players.indexOfFirst { it.displayName == player }
            callMainBubble = CallMainBubbleData(
                playerIndex = playerIndex,
                playerName = player,
                cardName = cards.joinToString { it.displayName },
                suitName = suit
            )
            delay(2500)
            callMainBubble = null
        } else {
            callMainBubble = null
        }
    }

    // 监听阶段变化（自己已亮主则不弹反主窗口）
    LaunchedEffect(state.phase, state.currentCallerIndex) {
        when (state.phase) {
            GamePhase.CALL_MAIN -> {
                if (state.players.isNotEmpty() && state.currentCallerIndex != 0) showCallMainDialog = true
            }
            GamePhase.GAME_OVER -> { showSettlementDialog = true }
            else -> { showCallMainDialog = false; showSettlementDialog = false }
        }
    }

    // 清除选牌
    LaunchedEffect(state.phase, state.roundNumber, state.isLeadPhase, state.currentTurnIndex) {
        selectedIndices = emptySet()
        showTurnHint = false // 轮次变化时清除超时提示
    }

    // 显示当前轮出牌，如果没有则显示上一轮出牌（使用remember缓存）
    val playsByVisual = remember(state.currentRoundPlays, state.lastRoundPlays) {
        if (state.currentRoundPlays.isNotEmpty()) {
            state.currentRoundPlays.toMap()
        } else {
            state.lastRoundPlays.toMap()
        }
    }

    // 屏幕适配 - 使用统一的尺寸管理
    val screenSize = rememberScreenSize()
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val screenHeight = config.screenHeightDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ═══ 状态栏 ═══
            GameStatusBar(state = state, onExit = { showExitDialog = true }, screenSize = screenSize)

            // ═══ 主游戏区域 ═══
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ── AI 对家 (P2) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = screenSize.spacingSmall),
                    contentAlignment = Alignment.Center
                ) {
                    aiTop?.let {
                        AiHandView(
                            cardCount = it.handCardCount,
                            label = "对家(2) ${it.displayName}",
                            cardWidth = screenSize.aiCardWidth,
                            isCurrentTurn = state.currentTurnIndex == 2,
                            screenSize = screenSize
                        )
                    }
                }

                // ── 中间行：左家 + 出牌区 + 右家 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左家
                    Box(
                        modifier = Modifier.width(screenSize.aiHandAreaWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        aiLeft?.let {
                            AiHandView(
                                cardCount = it.handCardCount,
                                label = "左家(3) ${it.displayName}",
                                cardWidth = screenSize.aiCardWidth * 0.85f,
                                isCurrentTurn = state.currentTurnIndex == 3,
                                screenSize = screenSize
                            )
                        }
                    }

                    // 出牌区
                    PlayArea(
                        state = state,
                        playsByVisual = playsByVisual,
                        cardWidth = screenSize.playAreaCardWidth,
                        screenSize = screenSize,
                        modifier = Modifier.weight(1f)
                    )

                    // 右家
                    Box(
                        modifier = Modifier.width(screenSize.aiHandAreaWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        aiRight?.let {
                            AiHandView(
                                cardCount = it.handCardCount,
                                label = "右家(1) ${it.displayName}",
                                cardWidth = screenSize.aiCardWidth * 0.85f,
                                isCurrentTurn = state.currentTurnIndex == 1,
                                screenSize = screenSize
                            )
                        }
                    }
                }

                // ── 底牌展示区 ──
                if (state.phase == GamePhase.DIG_BOTTOM ||
                    (state.phase >= GamePhase.ROUND_SETTLE && state.bottomCards.isNotEmpty())) {
                    BottomCardsArea(state = state, cardWidth = screenSize.bottomCardWidth)
                }

                // ── 人类玩家手牌（换底牌阶段时隐藏，因为换牌界面已显示手牌）──
                if (!state.isBottomExchangePhase) {
                    humanPlayer?.let { hp ->
                        val sortedHand = hp.sortedHand(state.currentLevel)
                        val isAiCtrl = hp.isAiControlled
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HandCardsRow(
                                cards = sortedHand,
                                selectedIndices = if (isAiCtrl) emptySet() else selectedIndices,
                                faceUp = true,
                                cardWidth = screenSize.cardWidth,
                                overlap = screenSize.cardWidth * screenSize.cardOverlapRatio,
                                maxWidth = screenWidth - 16.dp,
                                onCardClick = if (isAiCtrl) null else { idx ->
                                    val card = sortedHand[idx]
                                    // 发牌阶段：点击级牌需要二次确认亮牌
                                    if (state.phase == GamePhase.DEAL &&
                                        card.rank == state.currentLevel &&
                                        !card.isJoker
                                    ) {
                                        showCardConfirmDialog = card
                                    }
                                    // 出牌阶段：点击级牌需要二次确认亮牌
                                    else if (state.phase == GamePhase.PLAYING &&
                                        card.rank == state.currentLevel &&
                                        !card.isJoker &&
                                        state.mainSuit == null
                                    ) {
                                        showCardConfirmDialog = card
                                    } else {
                                        selectedIndices = if (idx in selectedIndices) selectedIndices - idx else selectedIndices + idx
                                    }
                                }
                            )
                        }
                    }
                }

                // ── 发牌进度 ──
                if (state.phase == GamePhase.DEAL && state.dealingCardIndex >= 0) {
                    DealingProgressBar(
                        current = state.dealingCardIndex,
                        total = state.totalCardsToDeal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // ── 换底牌界面 ──
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

            // ═══ 亮主展示 ═══
            state.callMainDisplayCards?.let { cards ->
                CallMainDisplayOverlay(
                    cards = cards,
                    playerName = state.callMainDisplayPlayer ?: "",
                    suitName = state.callMainDisplaySuit ?: "",
                    cardWidth = screenSize.callMainCardWidth,
                    screenSize = screenSize
                )
            }

            // ═══ 操作按钮区（换底牌阶段隐藏，避免挤占换牌按钮空间） ═══
            if (!state.isBottomExchangePhase) {
                GameActionButtons(
                    state = state,
                    selectedIndices = selectedIndices,
                    humanPlayer = humanPlayer,
                    onDeal = { viewModel.handleEvent(GameEvent.Deal) },
                    onLeadPlay = { cards -> viewModel.handleEvent(GameEvent.LeadPlay(cards)) },
                    onFollowPlay = { cards -> viewModel.handleEvent(GameEvent.FollowPlay(cards)) },
                    onConfirmDigBottom = { viewModel.handleEvent(GameEvent.ConfirmDigBottom) },
                    onConfirmSettle = {
                        showSettlementDialog = false
                        viewModel.handleEvent(GameEvent.ConfirmSettle)
                    },
                    onRestart = {
                        showSettlementDialog = false
                        viewModel.handleEvent(GameEvent.Restart)
                    },
                    onDeselect = { selectedIndices = emptySet() },
                    onToggleAiControl = { viewModel.handleEvent(GameEvent.ToggleAiControl) },
                    isAiControlled = humanPlayer?.isAiControlled ?: false,
                    screenSize = screenSize,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = screenSize.spacingMedium)
                )
            }
        }

        // ═══ 中央牌堆（发牌阶段） ═══
        if (state.phase == GamePhase.DEAL && state.dealingCardIndex >= 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CentralDeckView(
                    remainingCards = state.totalCardsToDeal - state.dealingCardIndex,
                    totalCards = state.totalCardsToDeal,
                    cardWidth = screenSize.centralDeckCardWidth
                )
            }
        }

        // ═══ 飞牌动画（发牌阶段） ═══
        if (state.phase == GamePhase.DEAL && state.dealingTargetPlayerIndex >= 0) {
            DealingFlyingCard(
                targetPlayerIndex = state.dealingTargetPlayerIndex,
                card = state.dealingCurrentCard,
                cardWidth = screenSize.flyingCardWidth,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }

        // ═══ 错误提示弹窗（顶部，带动画） ═══
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            errorMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
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
            }
        }

        // ═══ 轮次过渡提示（顶部，带动画） ═══
        AnimatedVisibility(
            visible = state.roundTransitionMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            state.roundTransitionMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
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
            }
        }

        // ═══ 出牌超时提示（轮到玩家3秒未出牌） ═══
        AnimatedVisibility(
            visible = showTurnHint,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier.padding(top = 60.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "⏰ 轮到你出牌了",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
        }

        // ═══ 亮牌气泡提示（从亮牌玩家位置弹出） ═══
        callMainBubble?.let { bubble ->
            val bubbleAlignment = when (bubble.playerIndex) {
                0 -> Alignment.BottomCenter
                1 -> Alignment.CenterEnd
                2 -> Alignment.TopCenter
                3 -> Alignment.CenterStart
                else -> Alignment.Center
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (bubble.playerIndex == 0) 120.dp else 0.dp,
                        top = if (bubble.playerIndex == 2) 60.dp else 0.dp,
                        end = if (bubble.playerIndex == 1) 70.dp else 0.dp,
                        start = if (bubble.playerIndex == 3) 70.dp else 0.dp
                    ),
                contentAlignment = bubbleAlignment
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🃏 亮主",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Text(
                            text = "${bubble.cardName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${bubble.suitName}为主",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }

        // ═══ 闲家分数显示（右上角） ═══
        if (state.phase == GamePhase.PLAYING || state.phase == GamePhase.DIG_BOTTOM) {
            val attackerTeam = 1 - state.dealerTeam
            val attackerScore = state.teamScores.getOrElse(attackerTeam) { 0 }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.CenterEnd
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
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

    // ═══ 弹窗 ═══
    if (showCallMainDialog) {
        CallMainDialog(
            handCards = humanPlayer?.handCards ?: emptyList(),
            currentLevel = state.currentLevel,
            onCallMain = { cards ->
                showCallMainDialog = false
                viewModel.handleEvent(GameEvent.CallMain(cards))
            },
            onPass = {
                showCallMainDialog = false
                viewModel.handleEvent(GameEvent.PassCallMain)
            }
        )
    }

    // 亮牌确认弹窗
    showCardConfirmDialog?.let { card ->
        AlertDialog(
            onDismissRequest = { showCardConfirmDialog = null },
            title = { Text("亮牌确认") },
            text = { Text("确定要亮 ${card.displayName} 吗？\n${card.suit.displayName} 将成为主花色") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleEvent(GameEvent.ShowCard(card))
                    showCardConfirmDialog = null
                }) { Text("确定亮牌") }
            },
            dismissButton = {
                TextButton(onClick = { showCardConfirmDialog = null }) { Text("取消") }
            }
        )
    }

    if (showSettlementDialog && state.phase == GamePhase.GAME_OVER) {
        val settleResult = remember(state) {
            ScoringUseCase.settle(
                attackerTotalScore = state.teamScores.getOrElse(1 - state.dealerTeam) { 0 },
                dealerTeam = state.dealerTeam,
                currentLevel = state.currentLevel
            )
        }
        SettlementDialog(
            result = settleResult,
            currentLevel = state.currentLevel,
            dealerTeam = state.dealerTeam,
            onConfirm = {
                showSettlementDialog = false
                viewModel.handleEvent(GameEvent.Restart)
            }
        )
    }

    // 退出确认
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出游戏") },
            text = { Text("确定要退出当前对局吗？进度将丢失。") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.handleEvent(GameEvent.Restart)
                    onBack()
                }) { Text("确定退出") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("继续游戏") }
            }
        )
    }
}

// ═══════════════════════════════════════════
// 子组件
// ═══════════════════════════════════════════

@Composable
private fun GameStatusBar(state: GameState, onExit: () -> Unit, screenSize: com.shengji.app.ui.utils.ScreenSize) {
    // 缓存常用值
    val currentLevelDisplay = remember(state.currentLevel) { state.currentLevel.display }
    val mainSuitDisplay = remember(state.mainSuit) { state.mainSuit?.displayName ?: "无主" }
    val isRedSuit = remember(state.mainSuit) {
        state.mainSuit == Suit.HEART || state.mainSuit == Suit.DIAMOND
    }
    val mainSuitColor = if (isRedSuit) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
    val roundNumberText = remember(state.roundNumber) { "${state.roundNumber}" }
    val team0Score = remember(state.teamScores) { "${state.teamScores.getOrElse(0) { 0 }}" }
    val team1Score = remember(state.teamScores) { "${state.teamScores.getOrElse(1) { 0 }}" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .statusBarsPadding()
            .padding(horizontal = screenSize.spacingMedium, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onExit,
            contentPadding = PaddingValues(horizontal = screenSize.chipPaddingHorizontal, vertical = 0.dp)
        ) {
            Text("← 退出", style = MaterialTheme.typography.bodySmall.copy(fontSize = screenSize.statusTextSize))
        }

        Spacer(modifier = Modifier.width(screenSize.chipSpacing))

        InfoChip(
            label = "级",
            value = currentLevelDisplay,
            screenSize = screenSize
        )

        Spacer(modifier = Modifier.width(screenSize.chipSpacing))

        InfoChip(
            label = "主",
            value = mainSuitDisplay,
            valueColor = mainSuitColor,
            screenSize = screenSize
        )

        Spacer(modifier = Modifier.weight(1f))

        InfoChip(
            label = "轮",
            value = roundNumberText,
            screenSize = screenSize
        )

        Spacer(modifier = Modifier.width(screenSize.chipSpacing))

        InfoChip(
            label = "A",
            value = team0Score,
            screenSize = screenSize
        )

        Spacer(modifier = Modifier.width(screenSize.chipSpacing))

        InfoChip(
            label = "B",
            value = team1Score,
            screenSize = screenSize
        )
    }
}

@Composable
internal fun PlayArea(
    state: GameState,
    playsByVisual: Map<Int, List<Card>>,
    cardWidth: Dp = 40.dp,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    modifier: Modifier = Modifier
) {
    // 缓存当前轮出牌数据
    val player0Cards = remember(playsByVisual) { playsByVisual[0] ?: emptyList() }
    val player1Cards = remember(playsByVisual) { playsByVisual[1] ?: emptyList() }
    val player2Cards = remember(playsByVisual) { playsByVisual[2] ?: emptyList() }
    val player3Cards = remember(playsByVisual) { playsByVisual[3] ?: emptyList() }

    // 缓存牌型名称
    val comboName = remember(state.currentLeadCombo) {
        when (state.currentLeadCombo?.comboType) {
            ComboType.SINGLE -> "单张"
            ComboType.PAIR -> "对子"
            ComboType.TRACTOR -> "拖拉机"
            ComboType.SLINGSHOT -> "甩牌"
            else -> ""
        }
    }

    Column(
        modifier = modifier.padding(screenSize.spacingSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 对家出牌（上方）- 玩家2
        PlayAreaSlot(
            label = "对家(2)",
            cards = player2Cards,
            cardWidth = cardWidth,
            isCurrentTurn = state.currentTurnIndex == 2,
            screenSize = screenSize
        )

        Spacer(modifier = Modifier.height(screenSize.spacingSmall))

        // 中间行：左家 + 信息 + 右家
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左家出牌（左侧）- 玩家3
            PlayAreaSlot(
                label = "左家(3)",
                cards = player3Cards,
                cardWidth = cardWidth,
                isCurrentTurn = state.currentTurnIndex == 3,
                screenSize = screenSize
            )

            // 中间信息
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(screenSize.playAreaInfoWidth)
            ) {
                if (state.phase == GamePhase.PLAYING) {
                    Text(
                        text = if (state.isLeadPhase) "首出" else "跟牌",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = screenSize.labelTextSize),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (comboName.isNotEmpty()) {
                        Text(
                            text = comboName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = screenSize.labelTextSize),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (state.phase == GamePhase.DIG_BOTTOM) {
                    Text(
                        text = "抠底",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = screenSize.labelTextSize),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 右家出牌（右侧）- 玩家1
            PlayAreaSlot(
                label = "右家(1)",
                cards = player1Cards,
                cardWidth = cardWidth,
                isCurrentTurn = state.currentTurnIndex == 1,
                screenSize = screenSize
            )
        }

        Spacer(modifier = Modifier.height(screenSize.spacingSmall))

        // 自己出牌（下方）- 玩家0
        PlayAreaSlot(
            label = "你(0)",
            cards = player0Cards,
            cardWidth = cardWidth,
            isCurrentTurn = state.currentTurnIndex == 0,
            screenSize = screenSize
        )
    }
}

enum class PlayAreaLayout {
    HORIZONTAL, VERTICAL
}

@Composable
internal fun PlayAreaSlot(
    label: String,
    cards: List<Card>,
    cardWidth: Dp = 40.dp,
    isCurrentTurn: Boolean = false,
    screenSize: com.shengji.app.ui.utils.ScreenSize
) {
    // 缓存尺寸计算
    val emptyBoxWidth = remember(cardWidth) { cardWidth * 1.5f }
    val emptyBoxHeight = remember(cardWidth) { cardWidth * 1.2f }
    val cardOverlap = remember(cardWidth) { cardWidth * 0.4f }
    val hasCards = remember(cards) { cards.isNotEmpty() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = screenSize.labelTextSize),
            fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedVisibility(
            visible = hasCards,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        ) {
            if (hasCards) {
                HandCardsRow(
                    cards = cards,
                    faceUp = true,
                    cardWidth = cardWidth,
                    overlap = cardOverlap
                )
            }
        }
        if (!hasCards) {
            Box(
                modifier = Modifier
                    .width(emptyBoxWidth)
                    .height(emptyBoxHeight)
                    .background(
                        if (isCurrentTurn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCurrentTurn) "..." else "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentTurn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
internal fun BottomCardsArea(state: GameState, cardWidth: Dp = 32.dp) {
    // 缓存底牌数量文本
    val bottomCardsText = remember(state.bottomCards.size) { "底牌（${state.bottomCards.size}张）" }
    val spacerWidth = remember { 2.dp }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = bottomCardsText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            state.bottomCards.forEach { card ->
                CardView(card = card, faceUp = state.isBottomDug, width = cardWidth)
                Spacer(modifier = Modifier.width(spacerWidth))
            }
        }
    }
}

@Composable
internal fun BottomExchangeArea(
    bottomCards: List<Card>,
    handCards: List<Card>,
    selectedBottomIndices: List<Int>,
    selectedHandIndices: List<Int>,
    onSelectCard: (Int, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cardWidth: Dp = 32.dp,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    modifier: Modifier = Modifier
) {
    val selectedBottomCount = selectedBottomIndices.size
    val selectedHandCount = selectedHandIndices.size
    val canConfirm = selectedBottomCount > 0 && selectedBottomCount == selectedHandCount

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 底牌选择区域
        Text(
            text = "选择底牌（已选 $selectedBottomCount 张）",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = screenSize.labelTextSize),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(screenSize.spacingSmall))

        // 底牌展示（可点击选择）
        Row(horizontalArrangement = Arrangement.Center) {
            bottomCards.forEachIndexed { index, card ->
                val isSelected = index in selectedBottomIndices
                CardView(
                    card = card,
                    faceUp = true,
                    width = cardWidth,
                    selected = isSelected,
                    onClick = { onSelectCard(index, true) }
                )
            }
        }

        Spacer(modifier = Modifier.height(screenSize.spacingSmall))

        // 手牌选择区域 - 使用固定高度+垂直滚动
        Text(
            text = "选择手牌（已选 $selectedHandCount 张）",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = screenSize.labelTextSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(screenSize.spacingSmall))

        // 手牌展示（可水平滚动，限制最大高度）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenSize.cardWidth * 2.2f) // 限制最大高度
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                handCards.forEachIndexed { index, card ->
                    val isSelected = index in selectedHandIndices
                    CardView(
                        card = card,
                        faceUp = true,
                        width = cardWidth * 0.75f,
                        selected = isSelected,
                        onClick = { onSelectCard(index, false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(screenSize.spacingMedium))

        // 操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(screenSize.spacingLarge)
        ) {
            OutlinedButton(
                onClick = onCancel,
                contentPadding = PaddingValues(
                    horizontal = screenSize.buttonPaddingHorizontal,
                    vertical = screenSize.buttonPaddingVertical
                )
            ) {
                Text("不换", style = MaterialTheme.typography.labelMedium.copy(fontSize = screenSize.labelTextSize))
            }
            Button(
                onClick = onConfirm,
                enabled = canConfirm || selectedBottomCount == 0,
                contentPadding = PaddingValues(
                    horizontal = screenSize.buttonPaddingHorizontal,
                    vertical = screenSize.buttonPaddingVertical
                )
            ) {
                Text(
                    if (canConfirm) "确认换 $selectedBottomCount 张" else "跳过换牌",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = screenSize.labelTextSize)
                )
            }
        }
    }
}

@Composable
private fun CallMainDisplayOverlay(
    cards: List<Card>,
    playerName: String,
    suitName: String,
    cardWidth: Dp = 32.dp,
    screenSize: com.shengji.app.ui.utils.ScreenSize
) {
    // 缓存文本
    val titleText = remember(playerName) { "$playerName 亮主" }
    val suitText = remember(suitName) { "主花色: $suitName" }
    val cardShape = remember { RoundedCornerShape(12.dp) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenSize.spacingLarge * 2, vertical = screenSize.spacingSmall),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = cardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(screenSize.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = screenSize.titleTextSize),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(screenSize.spacingSmall))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                cards.forEach { card ->
                    CardView(card = card, faceUp = true, width = cardWidth)
                    Spacer(modifier = Modifier.width(screenSize.spacingSmall))
                }
            }
            Spacer(modifier = Modifier.height(screenSize.spacingSmall))
            Text(
                text = suitText,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = screenSize.scoreTextSize),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GameActionButtons(
    state: GameState,
    selectedIndices: Set<Int>,
    humanPlayer: Player?,
    onDeal: () -> Unit,
    onLeadPlay: (List<Card>) -> Unit,
    onFollowPlay: (List<Card>) -> Unit,
    onConfirmDigBottom: () -> Unit,
    onConfirmSettle: () -> Unit,
    onRestart: () -> Unit,
    onDeselect: () -> Unit,
    onToggleAiControl: () -> Unit,
    isAiControlled: Boolean,
    screenSize: com.shengji.app.ui.utils.ScreenSize,
    modifier: Modifier = Modifier
) {
    // 缓存选中的牌
    val selectedCards = remember(humanPlayer, selectedIndices, state.currentLevel) {
        humanPlayer?.let { hp ->
            val hand = hp.sortedHand(state.currentLevel)
            selectedIndices.filter { it in hand.indices }.map { hand[it] }
        } ?: emptyList()
    }

    // 缓存常用值
    val isHumanTurn = remember(state.currentTurnIndex) { state.currentTurnIndex == 0 }
    val hasSelectedCards = remember(selectedCards) { selectedCards.isNotEmpty() }
    val horizontalPadding8 = remember {
        PaddingValues(horizontal = screenSize.buttonPaddingHorizontal * 0.67f, vertical = screenSize.buttonPaddingVertical)
    }
    val horizontalPadding12 = remember {
        PaddingValues(horizontal = screenSize.buttonPaddingHorizontal, vertical = screenSize.buttonPaddingVertical)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.phase) {
            GamePhase.IDLE -> {
                Button(
                    onClick = onDeal,
                    modifier = Modifier.height(screenSize.buttonHeightLarge)
                ) {
                    Text("🃏 开始游戏", style = MaterialTheme.typography.titleMedium.copy(fontSize = screenSize.titleTextSize))
                }
            }

            GamePhase.DEAL -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.mainSuit != null) "已叫主·等反主" else "发牌中·点级牌亮主",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = screenSize.buttonTextSize),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.mainSuit != null) {
                        Spacer(modifier = Modifier.height(screenSize.spacingSmall))
                        Text(
                            text = "主: ${state.mainSuit!!.displayName}",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = screenSize.scoreTextSize),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            GamePhase.CALL_MAIN -> {
                Text(
                    text = "可反主或跳过",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = screenSize.buttonTextSize),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GamePhase.PLAYING -> {
                // 换底牌阶段
                if (state.isBottomExchangePhase) {
                    Text(
                        text = "选择底牌和手牌进行交换",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = screenSize.buttonTextSize),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // 托管按钮
                    OutlinedButton(
                        onClick = onToggleAiControl,
                        modifier = Modifier.padding(end = screenSize.spacingSmall),
                        contentPadding = horizontalPadding8,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isAiControlled)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isAiControlled) "托管中" else "电脑代打",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = screenSize.labelTextSize)
                        )
                    }

                    if (isAiControlled) {
                        Text(
                            text = "AI代打中",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = screenSize.statusTextSize),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (hasSelectedCards) {
                        OutlinedButton(
                            onClick = onDeselect,
                            modifier = Modifier.padding(end = screenSize.spacingSmall),
                            contentPadding = horizontalPadding8
                        ) {
                            Text("取消", style = MaterialTheme.typography.labelMedium.copy(fontSize = screenSize.labelTextSize))
                        }
                        Button(
                            onClick = {
                                if (state.isLeadPhase) onLeadPlay(selectedCards) else onFollowPlay(selectedCards)
                            },
                            enabled = isHumanTurn,
                            contentPadding = horizontalPadding12
                        ) {
                            Text(
                                text = if (state.isLeadPhase) "出牌" else "跟牌",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = screenSize.labelTextSize)
                            )
                        }
                    } else {
                        // 轮次提示 - 当前回合时高亮显示
                        val turnText = if (isHumanTurn) {
                            if (state.isLeadPhase) "👆 选牌出牌" else "👆 选牌跟牌"
                        } else "等待..."
                        val turnColor = if (isHumanTurn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHumanTurn)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = turnText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = screenSize.statusTextSize),
                                fontWeight = if (isHumanTurn) FontWeight.Bold else FontWeight.Normal,
                                color = turnColor
                            )
                        }
                    }
                }
            }

            GamePhase.DIG_BOTTOM -> {
                Button(
                    onClick = onConfirmDigBottom,
                    modifier = Modifier.height(screenSize.buttonHeightMedium),
                    contentPadding = horizontalPadding12
                ) {
                    Text("抠底", style = MaterialTheme.typography.labelLarge.copy(fontSize = screenSize.buttonTextSize))
                }
            }

            GamePhase.ROUND_SETTLE -> {
                Button(
                    onClick = onConfirmSettle,
                    modifier = Modifier.height(screenSize.buttonHeightMedium),
                    contentPadding = horizontalPadding12
                ) {
                    Text("继续", style = MaterialTheme.typography.labelLarge.copy(fontSize = screenSize.buttonTextSize))
                }
            }

            GamePhase.GAME_OVER -> {
                Button(
                    onClick = onRestart,
                    modifier = Modifier.height(screenSize.buttonHeightMedium),
                    contentPadding = horizontalPadding12
                ) {
                    Text("再来一局", style = MaterialTheme.typography.labelLarge.copy(fontSize = screenSize.buttonTextSize))
                }
            }
        }
    }
}

@Composable
private fun DealingProgressBar(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    // 缓存进度文本
    val progressText = remember(current, total) { "发牌进度: $current / $total" }
    val progressValue = remember(current, total) { current.toFloat() / total.toFloat() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { progressValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = progressText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════
// 发牌动画组件
// ═══════════════════════════════════════════

/**
 * 中央牌堆 - 发牌阶段显示在屏幕中央
 */
@Composable
private fun CentralDeckView(
    remainingCards: Int,
    totalCards: Int,
    cardWidth: Dp = 40.dp
) {
    // 缓存堆叠层数计算
    val stackCount = remember(remainingCards, totalCards) {
        ((remainingCards.toFloat() / totalCards) * 6).toInt().coerceIn(1, 6)
    }

    Box(contentAlignment = Alignment.Center) {
        repeat(stackCount) { i ->
            Box(
                modifier = Modifier.offset(
                    x = (i * 2).dp,
                    y = (i * -2).dp
                )
            ) {
                CardBack(width = cardWidth)
            }
        }
    }
}

/**
 * 飞牌动画 - 牌从中央飞向目标玩家
 */
@Composable
private fun DealingFlyingCard(
    targetPlayerIndex: Int,
    card: Card?,
    cardWidth: Dp,
    screenWidth: Dp,
    screenHeight: Dp
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(targetPlayerIndex) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            animationSpec = tween(160, easing = FastOutSlowInEasing) // 缩短动画时间
        )
    }

    // 缓存目标偏移计算
    val targetOffsetX = remember(targetPlayerIndex, screenWidth) {
        when (targetPlayerIndex) {
            1 -> screenWidth * 0.32f
            3 -> -screenWidth * 0.32f
            else -> 0.dp
        }
    }
    val targetOffsetY = remember(targetPlayerIndex, screenHeight) {
        when (targetPlayerIndex) {
            0 -> screenHeight * 0.32f
            2 -> -screenHeight * 0.32f
            else -> 0.dp
        }
    }

    val currentX = targetOffsetX * progress.value
    val currentY = targetOffsetY * progress.value
    val scale = 0.7f + 0.3f * progress.value
    val alpha = 0.5f + 0.5f * progress.value

    // 缓存是否显示牌面
    val faceUp = remember(targetPlayerIndex, card) { targetPlayerIndex == 0 && card != null }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = currentX, y = currentY)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            CardView(
                card = card,
                faceUp = faceUp,
                width = cardWidth
            )
        }
    }
}
