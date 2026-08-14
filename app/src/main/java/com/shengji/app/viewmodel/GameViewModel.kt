package com.shengji.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shengji.app.data.*
import com.shengji.app.db.GameProgressDao
import com.shengji.app.db.GameProgressEntity
import com.shengji.app.enums.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 游戏 ViewModel
 */
class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val teamCollectedPoints = mutableListOf(0, 0)
    private var currentMainSuit: Suit? = null
    private var currentCallerIndex: Int = -1
    private var firstCallerIndex: Int = -1  // 第一个亮牌者（获得坐庄权利）
    private var currentDealerIndex: Int = -1  // 当前庄家索引
    private var gameStartTime: Long = 0L
    private var lastRoundComboType: ComboType = ComboType.SINGLE

    private val _validationMessage = MutableStateFlow<String?>(null)
    val validationMessage: StateFlow<String?> = _validationMessage.asStateFlow()

    private val _soundEvent = MutableStateFlow<SoundEvent?>(null)
    val soundEvent: StateFlow<SoundEvent?> = _soundEvent.asStateFlow()

    private val _hasSavedProgress = MutableStateFlow(false)
    val hasSavedProgress: StateFlow<Boolean> = _hasSavedProgress.asStateFlow()

    private val gson = Gson()
    private var progressDao: GameProgressDao? = null

    var logCallback: ((String) -> Unit)? = null
    var onGameOver: (suspend (startTime: Long, winnerTeam: Int, finalLevel: String, team0Score: Int, team1Score: Int) -> Unit)? = null

    sealed class SoundEvent {
        data object PlayCard : SoundEvent()
        data object Win : SoundEvent()
        data object Lose : SoundEvent()
        data object Invalid : SoundEvent()
    }

    private fun log(msg: String) { logCallback?.invoke(msg) }

    fun clearValidationMessage() { _validationMessage.value = null }
    fun clearSoundEvent() { _soundEvent.value = null }

    /**
     * 设置数据库DAO（在Application初始化时调用）
     */
    fun setProgressDao(dao: GameProgressDao) {
        progressDao = dao
        // 检查是否有保存的进度
        viewModelScope.launch {
            _hasSavedProgress.value = dao.hasProgress() > 0
        }
    }

    fun handleEvent(event: GameEvent) {
        when (event) {
            is GameEvent.Deal -> handleDeal()
            is GameEvent.ShowCard -> handleShowCard(event.card)
            is GameEvent.CallMain -> handleCallMain(event.cards)
            is GameEvent.PassCallMain -> handlePassCallMain()
            is GameEvent.LeadPlay -> handleLeadPlay(event.cards)
            is GameEvent.FollowPlay -> handleFollowPlay(event.cards)
            is GameEvent.ConfirmDigBottom -> handleDigBottom()
            is GameEvent.ConfirmSettle -> handleSettle()
            is GameEvent.Restart -> handleRestart()
            is GameEvent.ToggleAiControl -> handleToggleAiControl()
            is GameEvent.SelectForExchange -> handleSelectForExchange(event.cardIndex, event.isBottomCard)
            is GameEvent.ConfirmBottomExchange -> handleConfirmBottomExchange()
            is GameEvent.CancelBottomExchange -> handleCancelBottomExchange()
        }
    }

    // ==================== 发牌（动画） ====================

    private fun handleDeal() {
        gameStartTime = System.currentTimeMillis()
        val dealResult = DealUseCase.dealWithSeed(gameStartTime)
        currentMainSuit = null
        currentCallerIndex = -1
        firstCallerIndex = -1  // 重置第一个亮牌者
        // currentDealerIndex 不在这里重置，由结算逻辑更新
        teamCollectedPoints[0] = 0
        teamCollectedPoints[1] = 0
        lastRoundComboType = ComboType.SINGLE
        
        // 重置记牌器
        AiPlayer.resetTracker()

        // 初始化空手牌状态
        val emptyPlayers = dealResult.players.map { it.copy(handCards = emptyList()) }
        _state.value = GameState(
            phase = GamePhase.DEAL,
            players = emptyPlayers,
            bottomCards = dealResult.bottomCards,
            currentLevel = Rank.TWO,
            dealerTeam = 0,
            isLeadPhase = true,
            dealingCardIndex = 0,
            totalCardsToDeal = 100 // 4 * 25
        )

        log("=== 开始发牌 ===")

        // 启动发牌动画 - 轮流发牌（一人一张），牌从中间飞出
        viewModelScope.launch {
            val cardsPerPlayer = dealResult.players.map { it.handCards.toMutableList() }
            val totalCards = cardsPerPlayer.sumOf { it.size }

            for (cardIndex in 0 until totalCards) {
                val playerIndex = cardIndex % 4
                if (cardsPerPlayer[playerIndex].isEmpty()) continue

                val card = cardsPerPlayer[playerIndex].removeAt(0)

                // 飞牌动画 + 同时更新手牌（单次状态更新）
                val currentPlayers = _state.value.players.toMutableList()
                val player = currentPlayers[playerIndex]
                currentPlayers[playerIndex] = player.copy(
                    handCards = player.handCards + card
                )
                _state.value = _state.value.copy(
                    players = currentPlayers,
                    dealingCardIndex = cardIndex + 1,
                    dealingTargetPlayerIndex = playerIndex,
                    dealingCurrentCard = card
                )

                delay(150) // 发牌动画间隔

                // 清除飞牌状态
                _state.value = _state.value.copy(
                    dealingTargetPlayerIndex = -1,
                    dealingCurrentCard = null
                )

                // AI 自动亮牌（有一定概率）
                if (playerIndex != 0 && card.rank == _state.value.currentLevel && !card.isJoker) {
                    val shouldShow = (Math.random() < 0.3)
                    if (shouldShow && currentMainSuit == null) {
                        handleShowCardForAI(card, playerIndex)
                    }
                }
            }

            // 发牌完成
            log("=== 发牌完成 ===")
            dealResult.players.forEach { p ->
                log("  ${p.displayName}: ${p.sortedHand().joinToString { it.displayName }}")
            }

            _state.value = _state.value.copy(
                phase = GamePhase.CALL_MAIN,
                dealingCardIndex = -1
            )
            log("\n=== 叫主/反主阶段 ===")

            // AI 自动反主
            processAiCallMain()
        }
    }

    // ==================== 亮牌（发牌时） ====================

    private fun handleShowCard(card: Card) {
        val state = _state.value
        // 只在发牌阶段或出牌阶段（未确定主花色时）可以亮牌
        if (state.phase != GamePhase.DEAL && state.phase != GamePhase.PLAYING) return
        // 出牌阶段已经有主花色了，不能再亮
        if (state.phase == GamePhase.PLAYING && state.mainSuit != null) return
        // 发牌阶段已有人叫主，不能再亮（只能在CALL_MAIN阶段反主）
        if (state.phase == GamePhase.DEAL && currentMainSuit != null) {
            _validationMessage.value = "已有人叫主，等发牌完成后可反主"
            _soundEvent.value = SoundEvent.Invalid
            return
        }

        // 只能亮级牌
        if (card.rank != state.currentLevel || card.isJoker) {
            _validationMessage.value = "只能亮级牌"
            _soundEvent.value = SoundEvent.Invalid
            return
        }

        // 检查玩家手牌中是否有这张牌
        val humanPlayer = state.players.getOrNull(0) ?: return
        if (humanPlayer.handCards.none { it.deckIndex == card.deckIndex }) {
            _validationMessage.value = "你没有这张牌"
            _soundEvent.value = SoundEvent.Invalid
            return
        }

        // 设置主花色
        currentMainSuit = card.suit
        // 记录第一个亮牌者（获得坐庄权利）
        if (firstCallerIndex == -1) {
            firstCallerIndex = 0  // 人类玩家索引
        }
        // currentCallerIndex 用于跟踪当前叫主者（用于反主判断），不决定庄家
        currentCallerIndex = 0

        val playerName = state.players.getOrNull(0)?.displayName ?: "你"
        _state.value = state.copy(
            mainSuit = currentMainSuit,
            currentCallerIndex = 0,
            callMainDisplayCards = listOf(card),
            callMainDisplayPlayer = playerName,
            callMainDisplaySuit = card.suit.displayName
        )
        // 只在上方弹窗展示，不显示底部提示
        log("  你亮牌: ${card.displayName} → ${card.suit.displayName} 为主")

        // 缩短展示时间
        viewModelScope.launch {
            delay(2000)
            if (_state.value.callMainDisplayCards != null) {
                _state.value = _state.value.copy(
                    callMainDisplayCards = null,
                    callMainDisplayPlayer = null,
                    callMainDisplaySuit = null
                )
            }
        }
    }

    private fun handleShowCardForAI(card: Card, playerIndex: Int) {
        currentMainSuit = card.suit
        // 记录第一个亮牌者（获得坐庄权利）
        if (firstCallerIndex == -1) {
            firstCallerIndex = playerIndex
        }
        // currentCallerIndex 用于跟踪当前叫主者（用于反主判断），不决定庄家
        currentCallerIndex = playerIndex
        val playerName = _state.value.players.getOrNull(playerIndex)?.displayName ?: "AI"
        _state.value = _state.value.copy(
            mainSuit = currentMainSuit,
            currentCallerIndex = playerIndex,
            callMainDisplayCards = listOf(card),
            callMainDisplayPlayer = playerName,
            callMainDisplaySuit = card.suit.displayName
        )
        log("  ${playerName} 亮牌: ${card.displayName} → ${card.suit.displayName} 为主")

        // 缩短展示时间
        viewModelScope.launch {
            delay(2000)
            if (_state.value.callMainDisplayCards != null) {
                _state.value = _state.value.copy(
                    callMainDisplayCards = null,
                    callMainDisplayPlayer = null,
                    callMainDisplaySuit = null
                )
            }
        }
    }

    // ==================== 叫主/反主 ====================

    private fun processAiCallMain() {
        viewModelScope.launch {
            val players = _state.value.players.toMutableList()
            for (i in 1..3) {
                val aiCards = AiPlayer.chooseCallMain(
                    players[i].handCards, _state.value.currentLevel, currentMainSuit, currentCallerIndex
                )
                if (aiCards != null) {
                    val result = CallMainUseCase.tryCallMain(
                        aiCards, i, _state.value.currentLevel, currentMainSuit, currentCallerIndex
                    )
                    if (result.success) {
                        currentMainSuit = result.mainSuit
                        // currentCallerIndex 用于反主优先级判断，不决定庄家
                        currentCallerIndex = i
                        val suitName = result.mainSuit?.displayName ?: "无主"
                        val playerName = players[i].displayName

                        // 展示亮主牌
                        _state.value = _state.value.copy(
                            mainSuit = currentMainSuit,
                            currentCallerIndex = i,
                            callMainDisplayCards = aiCards,
                            callMainDisplayPlayer = playerName,
                            callMainDisplaySuit = suitName
                        )
                        log("  ${playerName} 反主: ${aiCards.joinToString { it.displayName }} → ${result.message}")

                        delay(1500) // 缩短展示时间

                        // 清除展示
                        _state.value = _state.value.copy(
                            callMainDisplayCards = null,
                            callMainDisplayPlayer = null,
                            callMainDisplaySuit = null
                        )
                        delay(300)
                    }
                }
            }
            _state.value = _state.value.copy(mainSuit = currentMainSuit, currentCallerIndex = currentCallerIndex)
        }
    }

    private fun handleCallMain(cards: List<Card>) {
        // 自己亮的主自己不能反主
        if (currentCallerIndex == 0) {
            _validationMessage.value = "你已亮主，不能反自己"
            _soundEvent.value = SoundEvent.Invalid
            return
        }
        val result = CallMainUseCase.tryCallMain(
            cards, 0, _state.value.currentLevel, currentMainSuit, currentCallerIndex
        )
        if (result.success) {
            currentMainSuit = result.mainSuit
            currentCallerIndex = 0
            val suitName = result.mainSuit?.displayName ?: "无主"
            val playerName = _state.value.players.getOrNull(0)?.displayName ?: "你"

            // 展示亮主牌
            _state.value = _state.value.copy(
                mainSuit = currentMainSuit,
                currentCallerIndex = 0,
                callMainDisplayCards = cards,
                callMainDisplayPlayer = playerName,
                callMainDisplaySuit = suitName
            )
            log("  你反主: ${cards.joinToString { it.displayName }} → ${result.message}")

            // 缩短展示时间后开始游戏
            viewModelScope.launch {
                delay(2000)
                _state.value = _state.value.copy(
                    callMainDisplayCards = null,
                    callMainDisplayPlayer = null,
                    callMainDisplaySuit = null
                )
                markMainTrumpAndStartPlaying()
            }
        } else {
            _validationMessage.value = result.message
            _soundEvent.value = SoundEvent.Invalid
            log("  反主失败: ${result.message}")
        }
    }

    private fun handlePassCallMain() {
        if (currentCallerIndex == -1) {
            // 没人亮主，重新发牌
            log("  没人亮主，重新发牌")
            _validationMessage.value = "没人亮主，重新发牌"
            val savedLevel = _state.value.currentLevel
            viewModelScope.launch {
                delay(1000) // 缩短等待时间
                reDeal(savedLevel)
            }
            return
        }
        markMainTrumpAndStartPlaying()
    }

    private fun reDeal(level: Rank) {
        gameStartTime = System.currentTimeMillis()
        val dealResult = DealUseCase.dealWithSeed(gameStartTime)
        currentMainSuit = null
        currentCallerIndex = -1
        firstCallerIndex = -1  // 重置第一个亮牌者
        teamCollectedPoints[0] = 0
        teamCollectedPoints[1] = 0
        lastRoundComboType = ComboType.SINGLE

        AiPlayer.resetTracker()

        val emptyPlayers = dealResult.players.map { it.copy(handCards = emptyList()) }
        _state.value = GameState(
            phase = GamePhase.DEAL,
            players = emptyPlayers,
            bottomCards = dealResult.bottomCards,
            currentLevel = level,
            dealerTeam = 0,
            isLeadPhase = true,
            dealingCardIndex = 0,
            totalCardsToDeal = 100
        )

        log("=== 重新发牌（打 ${level.display}）===")

        viewModelScope.launch {
            val cardsPerPlayer = dealResult.players.map { it.handCards.toMutableList() }
            val totalCards = cardsPerPlayer.sumOf { it.size }

            for (cardIndex in 0 until totalCards) {
                val playerIndex = cardIndex % 4
                if (cardsPerPlayer[playerIndex].isEmpty()) continue

                val card = cardsPerPlayer[playerIndex].removeAt(0)

                // 飞牌动画 + 同时更新手牌（单次状态更新）
                val currentPlayers = _state.value.players.toMutableList()
                val player = currentPlayers[playerIndex]
                currentPlayers[playerIndex] = player.copy(
                    handCards = player.handCards + card
                )
                _state.value = _state.value.copy(
                    players = currentPlayers,
                    dealingCardIndex = cardIndex + 1,
                    dealingTargetPlayerIndex = playerIndex,
                    dealingCurrentCard = card
                )

                delay(150) // 发牌动画间隔

                // 清除飞牌状态
                _state.value = _state.value.copy(
                    dealingTargetPlayerIndex = -1,
                    dealingCurrentCard = null
                )

                if (playerIndex != 0 && card.rank == _state.value.currentLevel && !card.isJoker) {
                    val shouldShow = (Math.random() < 0.3)
                    if (shouldShow && currentMainSuit == null) {
                        handleShowCardForAI(card, playerIndex)
                    }
                }
            }

            log("=== 发牌完成 ===")
            dealResult.players.forEach { p ->
                log("  ${p.displayName}: ${p.sortedHand().joinToString { it.displayName }}")
            }

            _state.value = _state.value.copy(
                phase = GamePhase.CALL_MAIN,
                dealingCardIndex = -1
            )
            log("\n=== 叫主/反主阶段 ===")
            processAiCallMain()
        }
    }

    private fun markMainTrumpAndStartPlaying() {
        markMainTrumpForAll()

        val state = _state.value.copy(
            callMainDisplayCards = null,
            callMainDisplayPlayer = null,
            callMainDisplaySuit = null
        )

        // 确定庄家索引
        val dealerIndex = if (currentDealerIndex == -1) {
            // 第一局：第一个亮牌者坐庄
            firstCallerIndex.coerceAtLeast(0)
        } else {
            // 后续局：使用结算时确定的庄家
            currentDealerIndex
        }
        // 更新当前庄家索引
        currentDealerIndex = dealerIndex

        val dealerPlayer = state.players.getOrNull(dealerIndex)
        val newDealerTeam = dealerPlayer?.teamIndex ?: 0

        // 更新庄家标记
        val updatedPlayers = state.players.mapIndexed { idx, p ->
            p.copy(isDealer = idx == dealerIndex)
        }

        if (dealerIndex == 0) {
            // 人类是庄家，进入换底牌阶段
            _state.value = state.copy(
                players = updatedPlayers,
                mainSuit = currentMainSuit,
                dealerTeam = newDealerTeam,
                isBottomExchangePhase = true,
                phase = GamePhase.PLAYING,
                isLeadPhase = true,
                currentTurnIndex = 0
            )
            log("\n=== 换底牌阶段 ===")
            log("  主花色: ${currentMainSuit?.displayName ?: "无主"}")
            log("  你是庄家，可以选择换底牌")
        } else {
            // AI 是庄家，AI 自动换底牌
            log("\n=== AI 庄家换底牌 ===")
            log("  庄家: ${dealerPlayer?.displayName}，主花色: ${currentMainSuit?.displayName ?: "无主"}")

            val newBottomCards = state.bottomCards.toMutableList()
            val newHand = dealerPlayer!!.handCards.toMutableList()
            val aiExchange = aiExchangeBottomCards(newHand, newBottomCards, state.currentLevel)

            for ((handIdx, bottomIdx) in aiExchange) {
                val temp = newBottomCards[bottomIdx]
                newBottomCards[bottomIdx] = newHand[handIdx]
                newHand[handIdx] = temp
            }

            val finalPlayers = updatedPlayers.toMutableList()
            finalPlayers[dealerIndex] = finalPlayers[dealerIndex].copy(handCards = newHand)

            _state.value = state.copy(
                players = finalPlayers,
                bottomCards = newBottomCards,
                mainSuit = currentMainSuit,
                dealerTeam = newDealerTeam,
                phase = GamePhase.PLAYING,
                isLeadPhase = true,
                currentTurnIndex = dealerIndex
            )

            log("  AI 换底牌完成，交换了 ${aiExchange.size} 对")

            // AI 庄家首出
            if (dealerPlayer.handCards.isNotEmpty()) {
                viewModelScope.launch {
                    delay(500)
                    processAiLead(dealerIndex)
                }
            }
        }
    }

    /**
     * AI 庄家换底牌策略（增强版）：
     * - 优先把副牌中的小牌（非分牌）与底牌中的分牌交换
     * - 其次把副牌中的小牌与底牌中的大牌交换
     * - 如果底牌有主牌，优先拿到手中
     * - 考虑花色控制力，优先交换弱势花色的牌
     */
    private fun aiExchangeBottomCards(
        hand: MutableList<Card>,
        bottomCards: MutableList<Card>,
        currentLevel: Rank
    ): List<Pair<Int, Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()

        // 评估手牌中每张牌的价值
        val handValues = hand.mapIndexed { idx, card ->
            val strength = HeuristicEvaluator.cardStrength(card, null, currentLevel)
            val isWeak = !card.isMainTrump && card.pointValue == 0 && strength < 15.0
            idx to (strength to isWeak)
        }

        // 找手牌中最弱的牌（优先换出）
        val weakCards = handValues
            .filter { it.second.second } // 是弱牌
            .sortedBy { it.second.first } // 按牌力排序
            .map { it.first }
            .toMutableList()

        // 如果弱牌不够，加上非主牌的分牌
        if (weakCards.size < 8) {
            val pointCards = hand.indices
                .filter { !hand[it].isMainTrump && it !in weakCards }
                .sortedBy { HeuristicEvaluator.cardStrength(hand[it], null, currentLevel) }
            weakCards.addAll(pointCards)
        }

        // 找底牌中值得换的牌（主牌 > 分牌 > 大牌）
        val bottomCandidates = bottomCards.indices.sortedByDescending { idx ->
            val card = bottomCards[idx]
            var priority = 0.0
            if (card.isMainTrump) priority += 30.0  // 主牌最优先
            priority += card.pointValue * 2.5       // 分牌优先
            priority += card.rank.order * 1.2       // 大牌优先
            // 级牌加成
            if (card.rank == currentLevel) priority += 15.0
            priority
        }.toMutableList()

        // 最多换8张
        val maxExchange = minOf(8, weakCards.size, bottomCandidates.size)
        for (i in 0 until maxExchange) {
            pairs.add(weakCards[i] to bottomCandidates[i])
        }

        return pairs
    }

    private fun markMainTrumpForAll() {
        val players = _state.value.players.map { player ->
            player.copy(
                handCards = CardEvaluator.markMainTrump(player.handCards, currentMainSuit, _state.value.currentLevel)
            )
        }
        _state.value = _state.value.copy(players = players)
    }

    // ==================== 出牌 ====================

    private fun handleLeadPlay(cards: List<Card>) {
        val state = _state.value
        if (state.phase != GamePhase.PLAYING || !state.isLeadPhase) return
        val player = state.currentPlayer ?: return
        if (state.currentTurnIndex != 0) return

        val validation = PlayValidation.validateLead(cards, player.handCards, currentMainSuit, state.currentLevel)
        if (!validation.isValid) {
            _validationMessage.value = validation.message
            _soundEvent.value = SoundEvent.Invalid
            return
        }

        val combo = PlayCombo(
            cards = cards,
            comboType = PlayValidation.identifyCombo(cards, currentMainSuit, state.currentLevel),
            suit = CardEvaluator.effectiveSuit(cards.first(), currentMainSuit, state.currentLevel)
        )

        val newHand = player.handCards.filter { card -> cards.none { it.deckIndex == card.deckIndex } }
        updatePlayerHand(0, newHand)
        _soundEvent.value = SoundEvent.PlayCard
        
        // 记录出牌
        AiPlayer.recordPlay(0, cards)

        // 立即显示玩家出牌
        _state.value = _state.value.copy(
            isLeadPhase = false,
            currentLeadCombo = combo,
            currentRoundPlays = listOf(0 to cards)
        )

        // AI 跟牌（带延迟动画）
        viewModelScope.launch {
            val roundPlays = mutableListOf<Pair<Int, List<Card>>>(0 to cards)
            val leadIndex = 0
            
            for (offset in 1..3) {
                val aiIndex = (leadIndex + offset) % 4
                val aiPlayer = _state.value.players.getOrNull(aiIndex) ?: continue
                if (aiPlayer.handCards.isEmpty()) continue
                if (aiPlayer.playerType == PlayerType.HUMAN && !aiPlayer.isAiControlled) continue

                delay(300) // AI跟牌延迟
                
                val pointsInRound = roundPlays.sumOf { (_, c) -> CardEvaluator.totalPoints(c) }
                val isTeammateWinning = checkTeammateWinning(aiIndex, roundPlays)
                
                val aiCards = AiPlayer.chooseFollow(
                    handCards = aiPlayer.handCards,
                    leadCombo = combo,
                    mainSuit = currentMainSuit,
                    currentLevel = _state.value.currentLevel,
                    pointsInRound = pointsInRound,
                    teamScores = teamCollectedPoints.toList(),
                    myTeam = aiPlayer.teamIndex,
                    isTeammateWinning = isTeammateWinning
                )

                if (aiCards.isNotEmpty()) {
                    val aiNewHand = aiPlayer.handCards.filter { c -> aiCards.none { it.deckIndex == c.deckIndex } }
                    updatePlayerHand(aiIndex, aiNewHand)
                    roundPlays.add(aiIndex to aiCards)
                    
                    // 记录出牌
                    AiPlayer.recordPlay(aiIndex, aiCards)
                    
                    // 立即更新状态，UI 会重组显示
                    _state.value = _state.value.copy(currentRoundPlays = roundPlays.toList())
                    _soundEvent.value = SoundEvent.PlayCard
                }
            }

            delay(400) // 出牌完成等待
            resolveCurrentRound(combo, roundPlays)
        }
    }

    private fun handleFollowPlay(cards: List<Card>) {
        val state = _state.value
        if (state.phase != GamePhase.PLAYING || state.isLeadPhase) return
        val player = state.currentPlayer ?: return
        if (state.currentTurnIndex != 0) return
        val leadCombo = state.currentLeadCombo ?: return

        val validation = PlayValidation.validateFollow(cards, player.handCards, leadCombo, currentMainSuit, state.currentLevel)
        if (!validation.isValid) {
            _validationMessage.value = validation.message
            _soundEvent.value = SoundEvent.Invalid
            return
        }

        val newHand = player.handCards.filter { card -> cards.none { it.deckIndex == card.deckIndex } }
        updatePlayerHand(0, newHand)
        _soundEvent.value = SoundEvent.PlayCard
        
        // 记录出牌
        AiPlayer.recordPlay(0, cards)

        // 立即显示出牌
        val roundPlays = state.currentRoundPlays.toMutableList()
        roundPlays.add(0 to cards)
        _state.value = _state.value.copy(currentRoundPlays = roundPlays)

        // AI 跟牌
        viewModelScope.launch {
            val leadIndex = state.currentRoundPlays.firstOrNull()?.first ?: 0

            for (offset in 1..3) {
                val aiIndex = (leadIndex + offset) % 4
                if (roundPlays.any { it.first == aiIndex }) continue

                val aiPlayer = _state.value.players.getOrNull(aiIndex) ?: continue
                if (aiPlayer.handCards.isEmpty()) continue
                if (aiPlayer.playerType == PlayerType.HUMAN && !aiPlayer.isAiControlled) continue

                delay(300) // AI跟牌延迟
                
                val pointsInRound = roundPlays.sumOf { (_, c) -> CardEvaluator.totalPoints(c) }
                val isTeammateWinning = checkTeammateWinning(aiIndex, roundPlays)
                
                val aiCards = AiPlayer.chooseFollow(
                    handCards = aiPlayer.handCards,
                    leadCombo = leadCombo,
                    mainSuit = currentMainSuit,
                    currentLevel = _state.value.currentLevel,
                    pointsInRound = pointsInRound,
                    teamScores = teamCollectedPoints.toList(),
                    myTeam = aiPlayer.teamIndex,
                    isTeammateWinning = isTeammateWinning
                )

                if (aiCards.isNotEmpty()) {
                    val aiNewHand = aiPlayer.handCards.filter { c -> aiCards.none { it.deckIndex == c.deckIndex } }
                    updatePlayerHand(aiIndex, aiNewHand)
                    roundPlays.add(aiIndex to aiCards)
                    
                    // 记录出牌
                    AiPlayer.recordPlay(aiIndex, aiCards)
                    
                    _state.value = _state.value.copy(currentRoundPlays = roundPlays.toList())
                    _soundEvent.value = SoundEvent.PlayCard
                }
            }

            delay(400) // 出牌完成等待
            resolveCurrentRound(leadCombo, roundPlays)
        }
    }
    
    /**
     * 检查当前是否队友在赢
     */
    private fun checkTeammateWinning(myIndex: Int, roundPlays: List<Pair<Int, List<Card>>>): Boolean {
        if (roundPlays.isEmpty()) return false
        
        val myTeam = _state.value.players.getOrNull(myIndex)?.teamIndex ?: return false
        
        // 找当前最大的牌
        val currentWinner = roundPlays.maxByOrNull { (_, cards) ->
            cards.maxOf { it.rank.order }
        }?.first ?: return false
        
        val winnerTeam = _state.value.players.getOrNull(currentWinner)?.teamIndex ?: return false
        
        return myTeam == winnerTeam
    }

    private fun resolveCurrentRound(leadCombo: PlayCombo, roundPlays: List<Pair<Int, List<Card>>>) {
        if (roundPlays.isEmpty()) return

        lastRoundComboType = leadCombo.comboType

        val result = RoundResolver.resolveRound(roundPlays, leadCombo, currentMainSuit, _state.value.currentLevel)
        val winnerTeam = _state.value.players[result.winnerIndex].teamIndex
        teamCollectedPoints[winnerTeam] = (teamCollectedPoints[winnerTeam] + result.points).coerceAtMost(200)

        log("  → 轮次赢家: ${_state.value.players[result.winnerIndex].displayName}，得分: ${result.points}")

        // 更新状态显示最终出牌结果（保留显示出牌，不清空）
        val winnerName = _state.value.players[result.winnerIndex].displayName
        _state.value = _state.value.copy(
            currentRoundPlays = roundPlays,
            lastRoundPlays = roundPlays,
            lastRoundWinner = result.winnerIndex,
            roundTransitionMessage = "本轮赢家: $winnerName，获得 ${result.points} 分"
        )

        val allHandsEmpty = _state.value.players.all { it.handCards.isEmpty() }

        // 延迟后进入下一轮
        viewModelScope.launch {
            delay(1000) // 轮次过渡时间
            
            // 清空过渡消息
            _state.value = _state.value.copy(roundTransitionMessage = null)
            
            if (allHandsEmpty) {
                // 所有手牌出完，进入抠底阶段
                _state.value = _state.value.copy(
                    phase = GamePhase.DIG_BOTTOM,
                    currentTurnIndex = result.winnerIndex,
                    roundNumber = _state.value.roundNumber + 1,
                    isLeadPhase = true,
                    currentLeadCombo = null,
                    currentRoundPlays = emptyList()
                )
                log("\n=== 抠底阶段 ===")
            } else {
                // 清空当前轮出牌，准备下一轮
                _state.value = _state.value.copy(
                    currentTurnIndex = result.winnerIndex,
                    roundNumber = _state.value.roundNumber + 1,
                    isLeadPhase = true,
                    currentLeadCombo = null,
                    currentRoundPlays = emptyList()
                )

                val winner = _state.value.players[result.winnerIndex]
                if ((winner.playerType == PlayerType.AI || winner.isAiControlled) && winner.handCards.isNotEmpty()) {
                    delay(200) // 轮次切换延迟
                    processAiLead(result.winnerIndex)
                }
            }
        }
    }

    private fun processAiLead(aiIndex: Int) {
        viewModelScope.launch {
            delay(200) // AI首出延迟

            val state = _state.value
            val aiPlayer = state.players.getOrNull(aiIndex) ?: return@launch

            val aiCards = AiPlayer.chooseLead(
                handCards = aiPlayer.handCards,
                mainSuit = currentMainSuit,
                currentLevel = state.currentLevel,
                teamScores = teamCollectedPoints.toList(),
                myTeam = aiPlayer.teamIndex
            )
            if (aiCards.isEmpty()) return@launch

            val combo = PlayCombo(
                cards = aiCards,
                comboType = PlayValidation.identifyCombo(aiCards, currentMainSuit, state.currentLevel),
                suit = CardEvaluator.effectiveSuit(aiCards.first(), currentMainSuit, state.currentLevel)
            )

            val newHand = aiPlayer.handCards.filter { card -> aiCards.none { it.deckIndex == card.deckIndex } }
            updatePlayerHand(aiIndex, newHand)
            _soundEvent.value = SoundEvent.PlayCard

            // 记录出牌
            AiPlayer.recordPlay(aiIndex, aiCards)

            _state.value = _state.value.copy(
                isLeadPhase = false,
                currentLeadCombo = combo,
                currentRoundPlays = listOf(aiIndex to aiCards)
            )

            // 按顺序让后续玩家跟牌
            val roundPlays = mutableListOf<Pair<Int, List<Card>>>(aiIndex to aiCards)

            for (offset in 1..3) {
                val nextIndex = (aiIndex + offset) % 4
                val nextPlayer = _state.value.players.getOrNull(nextIndex) ?: continue
                if (nextPlayer.handCards.isEmpty()) continue

                // 如果是人类玩家且非托管，等待其手动出牌
                if (nextPlayer.playerType == PlayerType.HUMAN && !nextPlayer.isAiControlled) {
                    _state.value = _state.value.copy(currentTurnIndex = nextIndex)
                    return@launch
                }

                delay(400) // 缩短AI跟牌延迟

                val pointsInRound = roundPlays.sumOf { (_, c) -> CardEvaluator.totalPoints(c) }
                val isTeammateWinning = checkTeammateWinning(nextIndex, roundPlays)

                val followCards = AiPlayer.chooseFollow(
                    handCards = nextPlayer.handCards,
                    leadCombo = combo,
                    mainSuit = currentMainSuit,
                    currentLevel = _state.value.currentLevel,
                    pointsInRound = pointsInRound,
                    teamScores = teamCollectedPoints.toList(),
                    myTeam = nextPlayer.teamIndex,
                    isTeammateWinning = isTeammateWinning
                )

                if (followCards.isNotEmpty()) {
                    val followNewHand = nextPlayer.handCards.filter { c -> followCards.none { it.deckIndex == c.deckIndex } }
                    updatePlayerHand(nextIndex, followNewHand)
                    roundPlays.add(nextIndex to followCards)

                    // 记录出牌
                    AiPlayer.recordPlay(nextIndex, followCards)

                    _state.value = _state.value.copy(currentRoundPlays = roundPlays.toList())
                    _soundEvent.value = SoundEvent.PlayCard
                }
            }

            delay(500) // 缩短出牌完成等待
            resolveCurrentRound(combo, roundPlays)
        }
    }

    private fun updatePlayerHand(playerIndex: Int, newHand: List<Card>) {
        val players = _state.value.players.toMutableList()
        players[playerIndex] = players[playerIndex].copy(handCards = newHand)
        _state.value = _state.value.copy(players = players)
    }

    private fun pointsInRound(cards: List<Card>): Int = CardEvaluator.totalPoints(cards)

    // ==================== 抠底 ====================

    private fun handleDigBottom() {
        val state = _state.value
        val lastRoundWinner = state.currentTurnIndex
        val comboType = lastRoundComboType
        val lastRoundCards = state.lastRoundPlays.flatMap { it.second }

        val result = DigBottomUseCase.digBottom(lastRoundWinner, comboType, lastRoundCards, state.bottomCards)
        val winnerTeam = state.players[result.diggerIndex].teamIndex
        teamCollectedPoints[winnerTeam] = (teamCollectedPoints[winnerTeam] + result.actualPoints).coerceAtMost(200)

        log("  抠底: ${state.players[result.diggerIndex].displayName}")
        log("  底牌分值: ${result.bottomPoints}，倍数: ×${result.multiplier}，实际得分: ${result.actualPoints}")

        _state.value = state.copy(
            phase = GamePhase.ROUND_SETTLE,
            isBottomDug = true,
            teamScores = teamCollectedPoints.toList()
        )

        log("\n=== 结算阶段 ===")
        log("  队伍0得分: ${teamCollectedPoints[0]}")
        log("  队伍1得分: ${teamCollectedPoints[1]}")

        handleSettle()
    }

    // ==================== 结算 ====================

    private fun handleSettle() {
        val state = _state.value
        val attackerScore = teamCollectedPoints[1 - state.dealerTeam].coerceIn(0, 200)
        val result = ScoringUseCase.settle(
            attackerTotalScore = attackerScore,
            dealerTeam = state.dealerTeam,
            currentLevel = state.currentLevel
        )

        log("\n  ${result.summary}")

        // 更新下一局的庄家
        if (result.dealerChanged) {
            // 庄队失败，闲家上庄（庄家的下家）
            currentDealerIndex = (currentDealerIndex + 1) % 4
            log("  庄家失败，下一位玩家坐庄")
        } else {
            // 庄队胜利，庄家的队友坐庄
            currentDealerIndex = (currentDealerIndex + 2) % 4
            log("  庄家胜利，队友坐庄")
        }

        _state.value = state.copy(
            phase = GamePhase.GAME_OVER,
            currentLevel = result.newLevel,
            dealerTeam = result.newDealerTeam,
            teamScores = teamCollectedPoints.toList()
        )

        val humanTeam = state.players[0].teamIndex
        val humanWon = if (result.dealerChanged) {
            humanTeam != state.dealerTeam
        } else {
            humanTeam == state.dealerTeam
        }
        _soundEvent.value = if (humanWon) SoundEvent.Win else SoundEvent.Lose

        viewModelScope.launch {
            onGameOver?.invoke(
                gameStartTime,
                result.newDealerTeam,
                result.newLevel.display,
                teamCollectedPoints[0],
                teamCollectedPoints[1]
            )
        }
    }

    // ==================== 托管 ====================

    private fun handleToggleAiControl() {
        val state = _state.value
        val humanPlayer = state.players.getOrNull(0) ?: return

        val newAiControlled = !humanPlayer.isAiControlled
        val players = state.players.toMutableList()
        players[0] = humanPlayer.copy(isAiControlled = newAiControlled)
        _state.value = state.copy(players = players)

        log("\n=== ${if (newAiControlled) "开启 AI 托管" else "接管回自己操作"} ===")

        if (newAiControlled && state.currentTurnIndex == 0 && state.phase == GamePhase.PLAYING) {
            if (state.isLeadPhase) {
                val aiCards = AiPlayer.chooseLead(
                    handCards = humanPlayer.handCards,
                    mainSuit = currentMainSuit,
                    currentLevel = state.currentLevel,
                    teamScores = teamCollectedPoints.toList(),
                    myTeam = humanPlayer.teamIndex
                )
                if (aiCards.isNotEmpty()) handleLeadPlay(aiCards)
            } else {
                val leadCombo = state.currentLeadCombo
                if (leadCombo != null) {
                    val pointsInRound = state.currentRoundPlays.sumOf { (_, c) -> CardEvaluator.totalPoints(c) }
                    val isTeammateWinning = checkTeammateWinning(0, state.currentRoundPlays)
                    val aiCards = AiPlayer.chooseFollow(
                        handCards = humanPlayer.handCards,
                        leadCombo = leadCombo,
                        mainSuit = currentMainSuit,
                        currentLevel = state.currentLevel,
                        pointsInRound = pointsInRound,
                        teamScores = teamCollectedPoints.toList(),
                        myTeam = humanPlayer.teamIndex,
                        isTeammateWinning = isTeammateWinning
                    )
                    if (aiCards.isNotEmpty()) handleFollowPlay(aiCards)
                }
            }
        }
    }

    // ==================== 底牌交换 ====================

    // 选中的底牌索引列表
    private val _selectedBottomIndices = MutableStateFlow<List<Int>>(emptyList())
    val selectedBottomIndices: StateFlow<List<Int>> = _selectedBottomIndices.asStateFlow()

    // 选中的手牌索引列表
    private val _selectedHandIndices = MutableStateFlow<List<Int>>(emptyList())
    val selectedHandIndices: StateFlow<List<Int>> = _selectedHandIndices.asStateFlow()

    private fun handleSelectForExchange(cardIndex: Int, isBottomCard: Boolean) {
        val state = _state.value
        if (!state.isBottomExchangePhase) return

        if (isBottomCard) {
            // 点击底牌：切换选中状态
            val current = _selectedBottomIndices.value.toMutableList()
            if (cardIndex in current) {
                current.remove(cardIndex)
            } else {
                current.add(cardIndex)
            }
            _selectedBottomIndices.value = current
        } else {
            // 点击手牌：切换选中状态
            val current = _selectedHandIndices.value.toMutableList()
            if (cardIndex in current) {
                current.remove(cardIndex)
            } else {
                current.add(cardIndex)
            }
            _selectedHandIndices.value = current
        }
    }

    private fun handleConfirmBottomExchange() {
        val state = _state.value
        if (!state.isBottomExchangePhase) return

        val humanPlayer = state.players.getOrNull(0) ?: return
        val bottomIndices = _selectedBottomIndices.value
        val handIndices = _selectedHandIndices.value

        // 检查选中数量是否相等
        if (bottomIndices.size != handIndices.size) {
            _validationMessage.value = "选中的底牌和手牌数量必须相同"
            _soundEvent.value = SoundEvent.Invalid
            return
        }

        if (bottomIndices.isEmpty()) {
            // 不换底牌，直接进入出牌阶段
            _state.value = state.copy(
                isBottomExchangePhase = false,
                exchangePairs = emptyMap(),
                phase = GamePhase.PLAYING,
                isLeadPhase = true,
                currentTurnIndex = 0
            )
            clearExchangeSelection()
            return
        }

        // 执行交换
        val newBottomCards = state.bottomCards.toMutableList()
        val newHand = humanPlayer.handCards.toMutableList()

        // 按顺序配对交换
        for (i in bottomIndices.indices) {
            val bottomIdx = bottomIndices[i]
            val handIdx = handIndices[i]
            val temp = newBottomCards[bottomIdx]
            newBottomCards[bottomIdx] = newHand[handIdx]
            newHand[handIdx] = temp
        }

        val players = state.players.toMutableList()
        players[0] = humanPlayer.copy(handCards = newHand)

        _state.value = state.copy(
            players = players,
            bottomCards = newBottomCards,
            isBottomExchangePhase = false,
            exchangePairs = emptyMap(),
            phase = GamePhase.PLAYING,
            isLeadPhase = true,
            currentTurnIndex = 0
        )

        log("  换底牌完成，交换了 ${bottomIndices.size} 张牌")
        clearExchangeSelection()
    }

    private fun handleCancelBottomExchange() {
        val state = _state.value
        _state.value = state.copy(
            isBottomExchangePhase = false,
            exchangePairs = emptyMap(),
            phase = GamePhase.PLAYING,
            isLeadPhase = true,
            currentTurnIndex = 0
        )
        clearExchangeSelection()
    }

    private fun clearExchangeSelection() {
        _selectedBottomIndices.value = emptyList()
        _selectedHandIndices.value = emptyList()
    }

    // ==================== 重开 ====================

    private fun handleRestart() {
        _state.value = GameState()
        teamCollectedPoints[0] = 0
        teamCollectedPoints[1] = 0
        currentMainSuit = null
        currentCallerIndex = -1
        firstCallerIndex = -1  // 重置第一个亮牌者
        currentDealerIndex = -1  // 重置当前庄家
        lastRoundComboType = ComboType.SINGLE
        gameStartTime = 0L
        _validationMessage.value = null
        _soundEvent.value = null
        
        // 重置记牌器
        AiPlayer.resetTracker()
        
        // 清除保存的进度
        viewModelScope.launch {
            progressDao?.clearProgress()
            _hasSavedProgress.value = false
        }
        
        log("\n=== 游戏重置 ===")
    }

    // ==================== 进度保存/恢复 ====================

    /**
     * 保存当前游戏进度
     */
    fun saveProgress() {
        val dao = progressDao ?: return
        val state = _state.value

        // 只在游戏进行中保存
        if (state.phase == GamePhase.IDLE || state.phase == GamePhase.GAME_OVER) return

        viewModelScope.launch {
            try {
                val progress = GameProgressEntity(
                    id = 1,
                    phase = state.phase.name,
                    mainSuit = state.mainSuit?.name,
                    currentLevel = state.currentLevel.name,
                    bottomCardsJson = gson.toJson(state.bottomCards.map { it.deckIndex }),
                    isBottomDug = state.isBottomDug,
                    teamScoresJson = gson.toJson(state.teamScores),
                    roundNumber = state.roundNumber,
                    playersJson = gson.toJson(state.players.map { player ->
                        mapOf(
                            "playerId" to player.playerId,
                            "displayName" to player.displayName,
                            "playerType" to player.playerType.name,
                            "handCards" to player.handCards.map { it.deckIndex },
                            "score" to player.score,
                            "isDealer" to player.isDealer,
                            "teamIndex" to player.teamIndex,
                            "isAiControlled" to player.isAiControlled
                        )
                    }),
                    currentTurnIndex = state.currentTurnIndex,
                    dealerTeam = state.dealerTeam,
                    isLeadPhase = state.isLeadPhase,
                    currentLeadComboJson = state.currentLeadCombo?.let {
                        gson.toJson(mapOf(
                            "cards" to it.cards.map { c -> c.deckIndex },
                            "comboType" to it.comboType.name,
                            "suit" to it.suit?.name
                        ))
                    },
                    currentRoundPlaysJson = gson.toJson(state.currentRoundPlays.map { (idx, cards) ->
                        mapOf("playerIndex" to idx, "cardIndices" to cards.map { it.deckIndex })
                    }),
                    lastRoundPlaysJson = gson.toJson(state.lastRoundPlays.map { (idx, cards) ->
                        mapOf("playerIndex" to idx, "cardIndices" to cards.map { it.deckIndex })
                    }),
                    lastRoundWinner = state.lastRoundWinner,
                    isBottomExchangePhase = state.isBottomExchangePhase,
                    vmMainSuit = currentMainSuit?.name,
                    vmCallerIndex = currentCallerIndex,
                    vmFirstCallerIndex = firstCallerIndex,
                    vmDealerIndex = currentDealerIndex,
                    vmTeamScoresJson = gson.toJson(teamCollectedPoints),
                    vmLastRoundComboType = lastRoundComboType.name
                )

                dao.saveProgress(progress)
                _hasSavedProgress.value = true
                log("  进度已保存")
            } catch (e: Exception) {
                log("  保存进度失败: ${e.message}")
            }
        }
    }

    /**
     * 恢复保存的游戏进度
     */
    fun restoreProgress() {
        val dao = progressDao ?: return

        viewModelScope.launch {
            try {
                val progress = dao.getProgress() ?: return@launch

                // 恢复游戏阶段
                val phase = GamePhase.valueOf(progress.phase)
                val mainSuit = progress.mainSuit?.let { Suit.valueOf(it) }
                val currentLevel = Rank.valueOf(progress.currentLevel)

                // 恢复底牌
                val bottomCardIndices: List<Int> = gson.fromJson(
                    progress.bottomCardsJson,
                    object : TypeToken<List<Int>>() {}.type
                )
                val allCards = DeckGenerator.generateDoubleDeck()
                val cardMap = allCards.associateBy { it.deckIndex }
                val bottomCards = bottomCardIndices.mapNotNull { cardMap[it] }

                // 恢复队伍分数
                val teamScores: List<Int> = gson.fromJson(
                    progress.teamScoresJson,
                    object : TypeToken<List<Int>>() {}.type
                )

                // 恢复玩家
                val playersData: List<Map<String, Any>> = gson.fromJson(
                    progress.playersJson,
                    object : TypeToken<List<Map<String, Any>>>() {}.type
                )
                val players = playersData.map { data ->
                    val handCardIndices = (data["handCards"] as List<*>).map { (it as Double).toInt() }
                    val handCards = handCardIndices.mapNotNull { idx ->
                        cardMap[idx]?.let { card ->
                            if (CardEvaluator.isMainTrump(card, mainSuit, currentLevel))
                                card.asMainTrump()
                            else card
                        }
                    }
                    Player(
                        playerId = data["playerId"] as String,
                        displayName = data["displayName"] as String,
                        playerType = PlayerType.valueOf(data["playerType"] as String),
                        handCards = handCards,
                        score = (data["score"] as Double).toInt(),
                        isDealer = data["isDealer"] as Boolean,
                        teamIndex = (data["teamIndex"] as Double).toInt(),
                        isAiControlled = data["isAiControlled"] as Boolean
                    )
                }

                // 恢复出牌记录
                val currentRoundPlaysData: List<Map<String, Any>> = gson.fromJson(
                    progress.currentRoundPlaysJson,
                    object : TypeToken<List<Map<String, Any>>>() {}.type
                )
                val currentRoundPlays = currentRoundPlaysData.map { data ->
                    val playerIndex = (data["playerIndex"] as Double).toInt()
                    val cardIndices = (data["cardIndices"] as List<*>).map { (it as Double).toInt() }
                    val cards = cardIndices.mapNotNull { cardMap[it] }
                    playerIndex to cards
                }

                val lastRoundPlaysData: List<Map<String, Any>> = gson.fromJson(
                    progress.lastRoundPlaysJson,
                    object : TypeToken<List<Map<String, Any>>>() {}.type
                )
                val lastRoundPlays = lastRoundPlaysData.map { data ->
                    val playerIndex = (data["playerIndex"] as Double).toInt()
                    val cardIndices = (data["cardIndices"] as List<*>).map { (it as Double).toInt() }
                    val cards = cardIndices.mapNotNull { cardMap[it] }
                    playerIndex to cards
                }

                // 恢复首出牌型
                val currentLeadCombo = progress.currentLeadComboJson?.let { json ->
                    val data: Map<String, Any> = gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
                    val comboCardIndices = (data["cards"] as List<*>).map { (it as Double).toInt() }
                    val comboCards = comboCardIndices.mapNotNull { cardMap[it] }
                    val comboType = ComboType.valueOf(data["comboType"] as String)
                    val comboSuit = (data["suit"] as String?)?.let { Suit.valueOf(it) }
                    PlayCombo(comboCards, comboType, comboSuit)
                }

                // 恢复游戏状态
                _state.value = GameState(
                    phase = phase,
                    mainSuit = mainSuit,
                    currentLevel = currentLevel,
                    bottomCards = bottomCards,
                    isBottomDug = progress.isBottomDug,
                    teamScores = teamScores,
                    roundNumber = progress.roundNumber,
                    players = players,
                    currentTurnIndex = progress.currentTurnIndex,
                    dealerTeam = progress.dealerTeam,
                    isLeadPhase = progress.isLeadPhase,
                    currentLeadCombo = currentLeadCombo,
                    currentRoundPlays = currentRoundPlays,
                    lastRoundPlays = lastRoundPlays,
                    lastRoundWinner = progress.lastRoundWinner,
                    isBottomExchangePhase = progress.isBottomExchangePhase,
                    dealingCardIndex = -1 // 恢复时发牌已完成
                )

                // 恢复ViewModel内部状态
                currentMainSuit = progress.vmMainSuit?.let { Suit.valueOf(it) }
                currentCallerIndex = progress.vmCallerIndex
                firstCallerIndex = progress.vmFirstCallerIndex
                currentDealerIndex = progress.vmDealerIndex
                lastRoundComboType = ComboType.valueOf(progress.vmLastRoundComboType)
                val vmTeamScores: List<Int> = gson.fromJson(
                    progress.vmTeamScoresJson,
                    object : TypeToken<List<Int>>() {}.type
                )
                teamCollectedPoints.clear()
                teamCollectedPoints.addAll(vmTeamScores)

                // 重新标记主牌
                markMainTrumpForAll()

                log("  进度已恢复")
            } catch (e: Exception) {
                log("  恢复进度失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
