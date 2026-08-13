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
import com.shengji.app.viewmodel.GameViewModel
import kotlinx.coroutines.delay

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

    // 监听阶段变化
    LaunchedEffect(state.phase) {
        when (state.phase) {
            GamePhase.CALL_MAIN -> {
                if (state.players.isNotEmpty()) showCallMainDialog = true
            }
            GamePhase.GAME_OVER -> { showSettlementDialog = true }
            else -> { showCallMainDialog = false; showSettlementDialog = false }
        }
    }

    // 清除选牌
    LaunchedEffect(state.phase, state.roundNumber, state.isLeadPhase, state.currentTurnIndex) {
        selectedIndices = emptySet()
    }

    val humanPlayer = state.players.getOrNull(0)
    val aiRight = state.players.getOrNull(1)
    val aiTop = state.players.getOrNull(2)
    val aiLeft = state.players.getOrNull(3)

    // 显示当前轮出牌，如果没有则显示上一轮出牌
    val playsByVisual = if (state.currentRoundPlays.isNotEmpty()) {
        state.currentRoundPlays.toMap()
    } else {
        state.lastRoundPlays.toMap()
    }

    // 屏幕适配 - 使用更合理的尺寸计算
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val screenHeight = config.screenHeightDp.dp
    val density = LocalDensity.current
    
    // 根据屏幕宽度和高度综合计算卡牌尺寸
    val cardWidth = when {
        screenWidth < 360.dp -> 32.dp  // 小屏手机
        screenWidth < 480.dp -> 40.dp  // 普通手机
        screenWidth < 600.dp -> 48.dp  // 大屏手机
        screenWidth < 800.dp -> 56.dp  // 小平板
        else -> 64.dp                   // 大平板
    }
    val overlap = cardWidth * 0.5f
    
    // 字体缩放比例（基于屏幕宽度）
    val fontScale = when {
        screenWidth < 360.dp -> 0.8f
        screenWidth < 480.dp -> 1.0f
        screenWidth < 600.dp -> 1.1f
        screenWidth < 800.dp -> 1.2f
        else -> 1.4f
    }
    
    // 通用间距缩放
    val spacingScale = when {
        screenWidth < 360.dp -> 0.8f
        screenWidth < 480.dp -> 1.0f
        screenWidth < 600.dp -> 1.1f
        else -> 1.2f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ═══ 状态栏 ═══
            GameStatusBar(state = state, onExit = { showExitDialog = true })

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
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    aiTop?.let {
                        AiHandView(
                            cardCount = it.handCardCount,
                            label = "对家(2) ${it.displayName}",
                            cardWidth = 32.dp,
                            isCurrentTurn = state.currentTurnIndex == 2
                        )
                    }
                }

                // ── 中间行：左家 + 出牌区 + 右家 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左家
                    Box(
                        modifier = Modifier.width(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        aiLeft?.let {
                            AiHandView(
                                cardCount = it.handCardCount,
                                label = "左家(3) ${it.displayName}",
                                cardWidth = 24.dp,
                                isCurrentTurn = state.currentTurnIndex == 3
                            )
                        }
                    }

                    // 出牌区
                    PlayArea(
                        state = state,
                        playsByVisual = playsByVisual,
                        cardWidth = (cardWidth * 0.8f).coerceAtMost(44.dp),
                        modifier = Modifier.weight(1f)
                    )

                    // 右家
                    Box(
                        modifier = Modifier.width(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        aiRight?.let {
                            AiHandView(
                                cardCount = it.handCardCount,
                                label = "右家(1) ${it.displayName}",
                                cardWidth = 24.dp,
                                isCurrentTurn = state.currentTurnIndex == 1
                            )
                        }
                    }
                }

                // ── 底牌展示区 ──
                if (state.phase == GamePhase.DIG_BOTTOM ||
                    (state.phase >= GamePhase.ROUND_SETTLE && state.bottomCards.isNotEmpty())) {
                    BottomCardsArea(state = state, cardWidth = cardWidth * 0.5f)
                }

                // ── 人类玩家手牌 ──
                humanPlayer?.let { hp ->
                    val sortedHand = hp.sortedHand(state.currentLevel)
                    val isAiCtrl = hp.isAiControlled
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HandCardsRow(
                            cards = sortedHand,
                            selectedIndices = if (isAiCtrl) emptySet() else selectedIndices,
                            faceUp = true,
                            cardWidth = cardWidth,
                            overlap = overlap,
                            maxWidth = screenWidth - 16.dp,
                            onCardClick = if (isAiCtrl) null else { idx ->
                                val card = sortedHand[idx]
                                // 换底牌阶段：点击手牌用于配对交换
                                if (state.isBottomExchangePhase) {
                                    viewModel.handleEvent(GameEvent.SelectForExchange(idx, false))
                                }
                                // 发牌阶段：点击级牌需要二次确认亮牌
                                else if (state.phase == GamePhase.DEAL &&
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
                        exchangePairs = state.exchangePairs,
                        onSelectCard = { idx, isBottom -> viewModel.handleEvent(GameEvent.SelectForExchange(idx, isBottom)) },
                        onConfirm = { viewModel.handleEvent(GameEvent.ConfirmBottomExchange) },
                        onCancel = { viewModel.handleEvent(GameEvent.CancelBottomExchange) },
                        cardWidth = cardWidth * 0.6f,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ═══ 亮主展示 ═══
            state.callMainDisplayCards?.let { cards ->
                CallMainDisplayOverlay(
                    cards = cards,
                    playerName = state.callMainDisplayPlayer ?: "",
                    suitName = state.callMainDisplaySuit ?: "",
                    cardWidth = cardWidth * 0.8f
                )
            }

            // ═══ 操作按钮区 ═══
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
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
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
                    cardWidth = cardWidth * 0.5f
                )
            }
        }

        // ═══ 飞牌动画（发牌阶段） ═══
        if (state.phase == GamePhase.DEAL && state.dealingTargetPlayerIndex >= 0) {
            DealingFlyingCard(
                targetPlayerIndex = state.dealingTargetPlayerIndex,
                card = state.dealingCurrentCard,
                cardWidth = cardWidth * 0.65f,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }

        // ═══ 错误提示弹窗（顶部） ═══
        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
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

        // ═══ 轮次过渡提示（顶部） ═══
        state.roundTransitionMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
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
private fun GameStatusBar(state: GameState, onExit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .statusBarsPadding() // 避开手机状态栏
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onExit,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text("← 退出", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.width(8.dp))

        InfoChip(label = "级牌", value = state.currentLevel.display)

        Spacer(modifier = Modifier.width(6.dp))

        InfoChip(
            label = "主花色",
            value = state.mainSuit?.displayName ?: "无主",
            valueColor = if (state.mainSuit == Suit.HEART || state.mainSuit == Suit.DIAMOND)
                Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(1f))

        InfoChip(label = "轮次", value = "${state.roundNumber}")

        Spacer(modifier = Modifier.width(6.dp))

        InfoChip(label = "A队", value = "${state.teamScores.getOrElse(0) { 0 }}")

        Spacer(modifier = Modifier.width(4.dp))

        InfoChip(label = "B队", value = "${state.teamScores.getOrElse(1) { 0 }}")
    }
}

@Composable
private fun PlayArea(
    state: GameState,
    playsByVisual: Map<Int, List<Card>>,
    cardWidth: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 对家出牌（上方）- 玩家2
        PlayAreaSlot(
            label = "对家(2)",
            cards = playsByVisual[2] ?: emptyList(),
            cardWidth = cardWidth,
            layout = PlayAreaLayout.HORIZONTAL,
            isCurrentTurn = state.currentTurnIndex == 2
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 中间行：左家 + 信息 + 右家
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左家出牌（左侧）- 玩家3
            PlayAreaSlot(
                label = "左家(3)",
                cards = playsByVisual[3] ?: emptyList(),
                cardWidth = cardWidth,
                layout = PlayAreaLayout.HORIZONTAL,
                isCurrentTurn = state.currentTurnIndex == 3
            )

            // 中间信息
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(50.dp)
            ) {
                if (state.phase == GamePhase.PLAYING) {
                    Text(
                        text = if (state.isLeadPhase) "首出" else "跟牌",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.currentLeadCombo != null) {
                        val comboName = when (state.currentLeadCombo!!.comboType) {
                            ComboType.SINGLE -> "单张"
                            ComboType.PAIR -> "对子"
                            ComboType.TRACTOR -> "拖拉机"
                            ComboType.SLINGSHOT -> "甩牌"
                            else -> ""
                        }
                        Text(
                            text = comboName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (state.phase == GamePhase.DIG_BOTTOM) {
                    Text(
                        text = "抠底",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 右家出牌（右侧）
            // 右家出牌（右侧）- 玩家1
            PlayAreaSlot(
                label = "右家(1)",
                cards = playsByVisual[1] ?: emptyList(),
                cardWidth = cardWidth,
                layout = PlayAreaLayout.HORIZONTAL,
                isCurrentTurn = state.currentTurnIndex == 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 自己出牌（下方）- 玩家0
        PlayAreaSlot(
            label = "你(0)",
            cards = playsByVisual[0] ?: emptyList(),
            cardWidth = cardWidth,
            layout = PlayAreaLayout.HORIZONTAL,
            isCurrentTurn = state.currentTurnIndex == 0
        )
    }
}

enum class PlayAreaLayout {
    HORIZONTAL, VERTICAL
}

@Composable
private fun PlayAreaSlot(
    label: String,
    cards: List<Card>,
    cardWidth: Dp = 40.dp,
    layout: PlayAreaLayout = PlayAreaLayout.HORIZONTAL,
    isCurrentTurn: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedVisibility(
            visible = cards.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        ) {
            if (cards.isNotEmpty()) {
                HandCardsRow(
                    cards = cards,
                    faceUp = true,
                    cardWidth = cardWidth,
                    overlap = cardWidth * 0.4f
                )
            }
        }
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .width(cardWidth * 1.5f)
                    .height(cardWidth * 1.2f)
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
private fun BottomCardsArea(state: GameState, cardWidth: Dp = 32.dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "底牌（${state.bottomCards.size}张）",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            state.bottomCards.forEach { card ->
                CardView(card = card, faceUp = state.isBottomDug, width = cardWidth)
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}

@Composable
private fun BottomExchangeArea(
    bottomCards: List<Card>,
    handCards: List<Card>,
    exchangePairs: Map<Int, Int>,
    onSelectCard: (Int, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cardWidth: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val completedPairs = exchangePairs.filter { it.value >= 0 }
    val pendingBottomIndex = exchangePairs.entries.find { it.value == -1 }?.key
    val pairedHandIndices = completedPairs.values.toSet()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 底牌选择区域
        Text(
            text = if (pendingBottomIndex != null) "请点击手牌与底牌配对" else "点击底牌开始配对交换（已配对 ${completedPairs.size} 对）",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))

        // 底牌展示（可点击选择/配对）
        Row(horizontalArrangement = Arrangement.Center) {
            bottomCards.forEachIndexed { index, card ->
                val isPaired = index in completedPairs
                val isPending = index == pendingBottomIndex
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .then(
                            when {
                                isPending -> Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                isPaired -> Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                else -> Modifier
                            }
                        )
                        .clickable { onSelectCard(index, true) }
                ) {
                    CardView(card = card, faceUp = true, width = cardWidth)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 手牌选择区域
        Text(
            text = "选择手牌交换（已配对 ${completedPairs.size} 张）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        // 手牌展示（可滚动，显示全部手牌）
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            handCards.forEachIndexed { index, card ->
                val isPaired = index in pairedHandIndices
                Box(
                    modifier = Modifier
                        .padding(1.dp)
                        .then(
                            if (isPaired) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelectCard(index, false) }
                ) {
                    CardView(card = card, faceUp = true, width = cardWidth * 0.8f)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("不换")
            }
            Button(
                onClick = onConfirm,
                enabled = pendingBottomIndex == null
            ) {
                Text("确认换 ${completedPairs.size} 对")
            }
        }
    }
}

@Composable
private fun CallMainDisplayOverlay(
    cards: List<Card>,
    playerName: String,
    suitName: String,
    cardWidth: Dp = 32.dp
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$playerName 亮主",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                cards.forEach { card ->
                    CardView(card = card, faceUp = true, width = cardWidth)
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "主花色: $suitName",
                style = MaterialTheme.typography.labelLarge,
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
    modifier: Modifier = Modifier
) {
    val selectedCards = humanPlayer?.let { hp ->
        val hand = hp.sortedHand(state.currentLevel)
        selectedIndices.filter { it in hand.indices }.map { hand[it] }
    } ?: emptyList()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.phase) {
            GamePhase.IDLE -> {
                Button(
                    onClick = onDeal,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("🃏 开始游戏", style = MaterialTheme.typography.titleMedium)
                }
            }

            GamePhase.DEAL -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.mainSuit != null) "已叫主·等反主" else "发牌中·点级牌亮主",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.mainSuit != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "主: ${state.mainSuit!!.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            GamePhase.CALL_MAIN -> {
                Text(
                    text = "可反主或跳过",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GamePhase.PLAYING -> {
                // 换底牌阶段
                if (state.isBottomExchangePhase) {
                    Text(
                        text = "选择底牌和手牌进行交换",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    val isHumanTurn = state.currentTurnIndex == 0

                    // 托管按钮
                    OutlinedButton(
                        onClick = onToggleAiControl,
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isAiControlled)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isAiControlled) "托管中" else "自己玩",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (isAiControlled) {
                        Text(
                            text = "AI代打中",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (selectedCards.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onDeselect,
                            modifier = Modifier.padding(end = 4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("取消", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = {
                                if (state.isLeadPhase) onLeadPlay(selectedCards) else onFollowPlay(selectedCards)
                            },
                            enabled = isHumanTurn,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (state.isLeadPhase) "出牌" else "跟牌",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else {
                        Text(
                            text = if (isHumanTurn) {
                                if (state.isLeadPhase) "选牌出牌" else "选牌跟牌"
                            } else "等待...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            GamePhase.DIG_BOTTOM -> {
                Button(
                    onClick = onConfirmDigBottom,
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("抠底", style = MaterialTheme.typography.labelLarge)
                }
            }

            GamePhase.ROUND_SETTLE -> {
                Button(
                    onClick = onConfirmSettle,
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("继续", style = MaterialTheme.typography.labelLarge)
                }
            }

            GamePhase.GAME_OVER -> {
                Button(
                    onClick = onRestart,
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("再来一局", style = MaterialTheme.typography.labelLarge)
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
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { current.toFloat() / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "发牌进度: $current / $total",
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
    val stackCount = ((remainingCards.toFloat() / totalCards) * 6).toInt().coerceIn(1, 6)
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

    val targetOffsetX = when (targetPlayerIndex) {
        1 -> screenWidth * 0.32f
        3 -> -screenWidth * 0.32f
        else -> 0.dp
    }
    val targetOffsetY = when (targetPlayerIndex) {
        0 -> screenHeight * 0.32f
        2 -> -screenHeight * 0.32f
        else -> 0.dp
    }

    val currentX = targetOffsetX * progress.value
    val currentY = targetOffsetY * progress.value
    val scale = 0.7f + 0.3f * progress.value
    val alpha = 0.5f + 0.5f * progress.value

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
                faceUp = targetPlayerIndex == 0 && card != null,
                width = cardWidth
            )
        }
    }
}
