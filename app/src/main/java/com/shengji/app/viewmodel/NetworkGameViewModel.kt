package com.shengji.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.shengji.app.data.*
import com.shengji.app.enums.*
import com.shengji.app.net.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 联机游戏 ViewModel
 *
 * 架构：
 * - 房主 = 服务端，负责游戏逻辑计算，广播状态给所有客户端
 * - 客户端 = 只负责显示和发送操作，接收房主的状态更新
 */
class NetworkGameViewModel : ViewModel() {

    companion object {
        private const val TAG = "NetworkGameVM"
    }

    // ═══ 基础状态 ═══

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _playerNames = MutableStateFlow<List<String>>(emptyList())
    val playerNames: StateFlow<List<String>> = _playerNames.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _soundEvent = MutableStateFlow<GameViewModel.SoundEvent?>(null)
    val soundEvent: StateFlow<GameViewModel.SoundEvent?> = _soundEvent.asStateFlow()

    // ═══ 网络组件 ═══

    private var tcpManager: TcpManager? = null
    private var udpDiscovery: UdpDiscovery? = null
    private var isHost = false
    private var myPlayerIndex = -1
    private var hostAddress = ""
    private var currentDeviceName = ""

    // ═══ 游戏逻辑（仅房主使用）═══

    private val teamCollectedPoints = mutableListOf(0, 0)
    private var currentMainSuit: Suit? = null
    private var currentCallerIndex: Int = -1
    private var firstCallerIndex: Int = -1
    private var currentDealerIndex: Int = -1
    private var lastRoundComboType: ComboType = ComboType.SINGLE

    var logCallback: ((String) -> Unit)? = null

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, IN_GAME
    }

    private fun log(msg: String) { logCallback?.invoke(msg) }

    fun clearErrorMessage() { _errorMessage.value = null }
    fun clearSoundEvent() { _soundEvent.value = null }

    // ═══════════════════════════════════════════════════
    // 房主 - 创建房间
    // ═══════════════════════════════════════════════════

    fun createRoom(deviceName: String, tcp: TcpManager, udp: UdpDiscovery) {
        tcpManager = tcp
        udpDiscovery = udp
        isHost = true
        myPlayerIndex = 0
        currentDeviceName = deviceName

        _connectionState.value = ConnectionState.CONNECTING
        _playerNames.value = listOf(deviceName)

        // 设置 TCP 回调
        tcp.onMessageReceived = { message -> handleTcpMessage(message) }
        tcp.onConnectionChanged = { connected ->
            _connectionState.value = if (connected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        }

        // 启动 TCP 服务端
        tcp.createRoom(deviceName)

        // 启动 UDP 广播
        udp.start(deviceName, "waiting", 1)

        _connectionState.value = ConnectionState.CONNECTED
        Log.d(TAG, "房间已创建，等待玩家加入")
    }

    // ═══════════════════════════════════════════════════
    // 客户端 - 加入房间
    // ═══════════════════════════════════════════════════

    fun joinRoom(hostAddr: String, deviceName: String, tcp: TcpManager) {
        tcpManager = tcp
        isHost = false
        hostAddress = hostAddr

        _connectionState.value = ConnectionState.CONNECTING

        // 设置 TCP 回调
        tcp.onMessageReceived = { message -> handleTcpMessage(message) }
        tcp.onConnectionChanged = { connected ->
            _connectionState.value = if (connected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        }

        // 连接到房主
        tcp.joinRoom(hostAddr, deviceName)

        Log.d(TAG, "正在加入房间: $hostAddr")
    }

    // ═══════════════════════════════════════════════════
    // TCP 消息处理
    // ═══════════════════════════════════════════════════

    private fun handleTcpMessage(message: TcpMessage) {
        when (message.type) {
            TcpMessageType.JOIN_RESPONSE -> handleJoinResponse(message)
            TcpMessageType.PLAYER_JOINED -> handlePlayerJoined(message)
            TcpMessageType.PLAYER_LEFT -> handlePlayerLeft(message)
            TcpMessageType.GAME_STATE -> handleGameStateUpdate(message)
            TcpMessageType.PLAYER_ACTION -> handlePlayerAction(message)
            TcpMessageType.START_GAME -> handleStartGame()
            TcpMessageType.HEARTBEAT -> { /* 心跳，忽略 */ }
            else -> Log.w(TAG, "未知消息类型: ${message.type}")
        }
    }

    private fun handleJoinResponse(message: TcpMessage) {
        try {
            val response = Gson().fromJson(message.payload, JoinResponsePayload::class.java)
            if (response.success) {
                myPlayerIndex = response.playerIndex
                _connectionState.value = ConnectionState.CONNECTED
                Log.d(TAG, "加入成功，我的索引: $myPlayerIndex")
            } else {
                _errorMessage.value = response.message
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析加入响应失败", e)
        }
    }

    private fun handlePlayerJoined(message: TcpMessage) {
        Log.d(TAG, "玩家加入: 索引=${message.playerIndex}")
        // 更新玩家列表（简化处理，实际应该同步完整列表）
    }

    private fun handlePlayerLeft(message: TcpMessage) {
        Log.d(TAG, "玩家离开: 索引=${message.playerIndex}")
        _errorMessage.value = "玩家 ${message.playerIndex} 已离开"
    }

    /**
     * 客户端接收游戏状态更新
     */
    private fun handleGameStateUpdate(message: TcpMessage) {
        try {
            val serializable = Gson().fromJson(message.payload, SerializableGameState::class.java)
            val gameState = serializable.toGameState()
            _state.value = gameState
            Log.d(TAG, "收到状态更新: phase=${gameState.phase}")
        } catch (e: Exception) {
            Log.e(TAG, "解析游戏状态失败", e)
        }
    }

    /**
     * 房主接收玩家操作
     */
    private fun handlePlayerAction(message: TcpMessage) {
        if (!isHost) return

        try {
            val action = Gson().fromJson(message.payload, PlayerActionPayload::class.java)
            val playerIndex = message.playerIndex

            Log.d(TAG, "收到玩家操作: player=$playerIndex, action=${action.actionType}")

            when (action.actionType) {
                "ShowCard" -> {
                    val card = findCardByDeckIndex(action.cardIndices.firstOrNull() ?: -1)
                    if (card != null) handleShowCardForPlayer(card, playerIndex)
                }
                "CallMain" -> {
                    val cards = action.cardIndices.mapNotNull { findCardByDeckIndex(it) }
                    if (cards.size == 2) handleCallMainForPlayer(cards, playerIndex)
                }
                "PassCallMain" -> handlePassCallMain()
                "LeadPlay" -> {
                    val cards = action.cardIndices.mapNotNull { findCardByDeckIndex(it) }
                    if (cards.isNotEmpty()) handleLeadPlayForPlayer(cards, playerIndex)
                }
                "FollowPlay" -> {
                    val cards = action.cardIndices.mapNotNull { findCardByDeckIndex(it) }
                    if (cards.isNotEmpty()) handleFollowPlayForPlayer(cards, playerIndex)
                }
                "ConfirmDigBottom" -> handleDigBottom()
                "ConfirmSettle" -> handleSettle()
                "ConfirmBottomExchange" -> handleConfirmBottomExchangeForPlayer(playerIndex)
                "CancelBottomExchange" -> handleCancelBottomExchange()
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理玩家操作失败", e)
        }
    }

    private fun handleStartGame() {
        Log.d(TAG, "收到开始游戏指令")
        // 客户端等待房主发送初始状态
    }

    // ═══════════════════════════════════════════════════
    // 状态同步
    // ═══════════════════════════════════════════════════

    /**
     * 房主广播游戏状态给所有客户端
     */
    private fun broadcastState() {
        if (!isHost) return
        tcpManager?.broadcastGameState(_state.value)
    }

    /**
     * 客户端发送操作给房主
     */
    private fun sendAction(actionType: String, cardIndices: List<Int> = emptyList(), extra: String = "") {
        if (isHost) {
            // 房主直接处理
            return
        }
        tcpManager?.sendPlayerAction(PlayerActionPayload(actionType, cardIndices, extra))
    }

    // ═══════════════════════════════════════════════════
    // 游戏事件处理（对外接口）
    // ═══════════════════════════════════════════════════

    fun handleEvent(event: GameEvent) {
        when (event) {
            is GameEvent.Deal -> handleDeal()
            is GameEvent.ShowCard -> {
                if (isHost) {
                    handleShowCard(event.card)
                } else {
                    sendAction("ShowCard", listOf(event.card.deckIndex))
                }
            }
            is GameEvent.CallMain -> {
                if (isHost) {
                    handleCallMain(event.cards)
                } else {
                    sendAction("CallMain", event.cards.map { it.deckIndex })
                }
            }
            is GameEvent.PassCallMain -> {
                if (isHost) {
                    handlePassCallMain()
                } else {
                    sendAction("PassCallMain")
                }
            }
            is GameEvent.LeadPlay -> {
                if (isHost) {
                    handleLeadPlay(event.cards)
                } else {
                    sendAction("LeadPlay", event.cards.map { it.deckIndex })
                }
            }
            is GameEvent.FollowPlay -> {
                if (isHost) {
                    handleFollowPlay(event.cards)
                } else {
                    sendAction("FollowPlay", event.cards.map { it.deckIndex })
                }
            }
            is GameEvent.ConfirmDigBottom -> {
                if (isHost) {
                    handleDigBottom()
                } else {
                    sendAction("ConfirmDigBottom")
                }
            }
            is GameEvent.ConfirmSettle -> {
                if (isHost) {
                    handleSettle()
                } else {
                    sendAction("ConfirmSettle")
                }
            }
            is GameEvent.Restart -> handleRestart()
            is GameEvent.ToggleAiControl -> {
                if (isHost) handleToggleAiControl()
            }
            is GameEvent.SelectForExchange -> {
                if (isHost) {
                    handleSelectForExchange(event.cardIndex, event.isBottomCard)
                } else {
                    // 客户端换牌逻辑需要特殊处理
                    handleSelectForExchange(event.cardIndex, event.isBottomCard)
                }
            }
            is GameEvent.ConfirmBottomExchange -> {
                if (isHost) {
                    handleConfirmBottomExchange()
                } else {
                    sendAction("ConfirmBottomExchange")
                }
            }
            is GameEvent.CancelBottomExchange -> {
                if (isHost) {
                    handleCancelBottomExchange()
                } else {
                    sendAction("CancelBottomExchange")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 游戏逻辑（房主执行）
    // ═══════════════════════════════════════════════════

    private fun handleDeal() {
        if (!isHost) return

        // 切换到游戏状态
        _connectionState.value = ConnectionState.IN_GAME

        // 更新UDP广播状态为"游戏中"
        udpDiscovery?.updateBroadcastInfo(
            deviceName = currentDeviceName,
            roomStatus = "playing",
            playerCount = _state.value.players.size
        )

        gameStartTime = System.currentTimeMillis()
        val dealResult = DealUseCase.dealWithSeed(gameStartTime)
        currentMainSuit = null
        currentCallerIndex = -1
        firstCallerIndex = -1
        teamCollectedPoints[0] = 0
        teamCollectedPoints[1] = 0
        lastRoundComboType = ComboType.SINGLE
        AiPlayer.resetTracker()

        val emptyPlayers = dealResult.players.map { it.copy(handCards = emptyList()) }
        _state.value = GameState(
            phase = GamePhase.DEAL,
            players = emptyPlayers,
            bottomCards = dealResult.bottomCards,
            currentLevel = Rank.TWO,
            dealerTeam = 0,
            isLeadPhase = true,
            dealingCardIndex = 0,
            totalCardsToDeal = 100
        )

        broadcastState()

        log("=== 开始发牌 ===")

        viewModelScope.launch {
            val cardsPerPlayer = dealResult.players.map { it.handCards.toMutableList() }
            val totalCards = cardsPerPlayer.sumOf { it.size }

            for (cardIndex in 0 until totalCards) {
                val playerIndex = cardIndex % 4
                if (cardsPerPlayer[playerIndex].isEmpty()) continue

                val card = cardsPerPlayer[playerIndex].removeAt(0)
                val currentPlayers = _state.value.players.toMutableList()
                val player = currentPlayers[playerIndex]
                currentPlayers[playerIndex] = player.copy(handCards = player.handCards + card)
                _state.value = _state.value.copy(
                    players = currentPlayers,
                    dealingCardIndex = cardIndex + 1,
                    dealingTargetPlayerIndex = playerIndex,
                    dealingCurrentCard = card
                )

                broadcastState()
                delay(150) // 发牌动画间隔

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
            _state.value = _state.value.copy(phase = GamePhase.CALL_MAIN, dealingCardIndex = -1)
            broadcastState()

            processAiCallMain()
        }
    }

    private fun handleShowCard(card: Card) {
        if (!isHost) return
        handleShowCardForPlayer(card, 0)
    }

    private fun handleShowCardForPlayer(card: Card, playerIndex: Int) {
        val state = _state.value
        if (state.phase != GamePhase.DEAL && state.phase != GamePhase.PLAYING) return
        if (state.phase == GamePhase.PLAYING && state.mainSuit != null) return
        if (state.phase == GamePhase.DEAL && currentMainSuit != null) return
        if (card.rank != state.currentLevel || card.isJoker) return

        val player = state.players.getOrNull(playerIndex) ?: return
        if (player.handCards.none { it.deckIndex == card.deckIndex }) return

        currentMainSuit = card.suit
        if (firstCallerIndex == -1) firstCallerIndex = playerIndex
        currentCallerIndex = playerIndex

        _state.value = state.copy(
            mainSuit = currentMainSuit,
            currentCallerIndex = playerIndex,
            callMainDisplayCards = listOf(card),
            callMainDisplayPlayer = player.displayName,
            callMainDisplaySuit = card.suit.displayName
        )
        broadcastState()

        log("  ${player.displayName} 亮牌: ${card.displayName} → ${card.suit.displayName} 为主")

        viewModelScope.launch {
            delay(2000)
            if (_state.value.callMainDisplayCards != null) {
                _state.value = _state.value.copy(
                    callMainDisplayCards = null,
                    callMainDisplayPlayer = null,
                    callMainDisplaySuit = null
                )
                broadcastState()
            }
        }
    }

    private fun handleShowCardForAI(card: Card, playerIndex: Int) {
        handleShowCardForPlayer(card, playerIndex)
    }

    private fun handleCallMain(cards: List<Card>) {
        if (!isHost) return
        handleCallMainForPlayer(cards, 0)
    }

    private fun handleCallMainForPlayer(cards: List<Card>, playerIndex: Int) {
        // 自己亮的主自己不能反主
        if (currentCallerIndex == playerIndex) {
            _errorMessage.value = "你已亮主，不能反自己"
            _soundEvent.value = GameViewModel.SoundEvent.Invalid
            return
        }
        val result = CallMainUseCase.tryCallMain(
            cards, playerIndex, _state.value.currentLevel, currentMainSuit, currentCallerIndex
        )
        if (result.success) {
            currentMainSuit = result.mainSuit
            currentCallerIndex = playerIndex
            val suitName = result.mainSuit?.displayName ?: "无主"
            val playerName = _state.value.players.getOrNull(playerIndex)?.displayName ?: "玩家$playerIndex"

            _state.value = _state.value.copy(
                mainSuit = currentMainSuit,
                currentCallerIndex = playerIndex,
                callMainDisplayCards = cards,
                callMainDisplayPlayer = playerName,
                callMainDisplaySuit = suitName
            )
            broadcastState()
            log("  $playerName 反主: ${cards.joinToString { it.displayName }} → ${result.message}")

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
            _errorMessage.value = result.message
        }
    }

    private fun handlePassCallMain() {
        if (currentCallerIndex == -1) {
            _errorMessage.value = "没人亮主，重新发牌"
            val savedLevel = _state.value.currentLevel
            viewModelScope.launch {
                delay(1000)
                reDeal(savedLevel)
            }
            return
        }
        markMainTrumpAndStartPlaying()
    }

    private fun reDeal(level: Rank) {
        if (!isHost) return
        gameStartTime = System.currentTimeMillis()
        val dealResult = DealUseCase.dealWithSeed(gameStartTime)
        currentMainSuit = null
        currentCallerIndex = -1
        firstCallerIndex = -1
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
        broadcastState()

        viewModelScope.launch {
            val cardsPerPlayer = dealResult.players.map { it.handCards.toMutableList() }
            val totalCards = cardsPerPlayer.sumOf { it.size }

            for (cardIndex in 0 until totalCards) {
                val playerIndex = cardIndex % 4
                if (cardsPerPlayer[playerIndex].isEmpty()) continue

                val card = cardsPerPlayer[playerIndex].removeAt(0)
                val currentPlayers = _state.value.players.toMutableList()
                val player = currentPlayers[playerIndex]
                currentPlayers[playerIndex] = player.copy(handCards = player.handCards + card)
                _state.value = _state.value.copy(
                    players = currentPlayers,
                    dealingCardIndex = cardIndex + 1,
                    dealingTargetPlayerIndex = playerIndex,
                    dealingCurrentCard = card
                )
                broadcastState()
                delay(150) // 发牌动画间隔
                _state.value = _state.value.copy(dealingTargetPlayerIndex = -1, dealingCurrentCard = null)

                if (playerIndex != 0 && card.rank == _state.value.currentLevel && !card.isJoker) {
                    if (Math.random() < 0.3 && currentMainSuit == null) {
                        handleShowCardForAI(card, playerIndex)
                    }
                }
            }

            _state.value = _state.value.copy(phase = GamePhase.CALL_MAIN, dealingCardIndex = -1)
            broadcastState()
            processAiCallMain()
        }
    }

    private var gameStartTime: Long = 0L

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
                        currentCallerIndex = i
                        val suitName = result.mainSuit?.displayName ?: "无主"
                        val playerName = players[i].displayName

                        _state.value = _state.value.copy(
                            mainSuit = currentMainSuit,
                            currentCallerIndex = i,
                            callMainDisplayCards = aiCards,
                            callMainDisplayPlayer = playerName,
                            callMainDisplaySuit = suitName
                        )
                        broadcastState()
                        log("  $playerName 反主: ${aiCards.joinToString { it.displayName }} → ${result.message}")

                        delay(1500)
                        _state.value = _state.value.copy(
                            callMainDisplayCards = null,
                            callMainDisplayPlayer = null,
                            callMainDisplaySuit = null
                        )
                        broadcastState()
                        delay(300)
                    }
                }
            }
            _state.value = _state.value.copy(mainSuit = currentMainSuit, currentCallerIndex = currentCallerIndex)
            broadcastState()
        }
    }

    private fun markMainTrumpAndStartPlaying() {
        markMainTrumpForAll()

        val state = _state.value.copy(
            callMainDisplayCards = null,
            callMainDisplayPlayer = null,
            callMainDisplaySuit = null
        )

        val dealerIndex = if (currentDealerIndex == -1) {
            firstCallerIndex.coerceAtLeast(0)
        } else {
            currentDealerIndex
        }
        currentDealerIndex = dealerIndex

        val dealerPlayer = state.players.getOrNull(dealerIndex)
        val newDealerTeam = dealerPlayer?.teamIndex ?: 0

        val updatedPlayers = state.players.mapIndexed { idx, p ->
            p.copy(isDealer = idx == dealerIndex)
        }

        if (dealerIndex == 0) {
            _state.value = state.copy(
                players = updatedPlayers,
                mainSuit = currentMainSuit,
                dealerTeam = newDealerTeam,
                isBottomExchangePhase = true,
                phase = GamePhase.PLAYING,
                isLeadPhase = true,
                currentTurnIndex = 0
            )
            broadcastState()
        } else {
            // AI 庄家换底牌
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
            broadcastState()

            if (dealerPlayer.handCards.isNotEmpty()) {
                viewModelScope.launch {
                    delay(300) // AI首出延迟
                    processAiLead(dealerIndex)
                }
            }
        }
    }

    private fun aiExchangeBottomCards(
        hand: MutableList<Card>,
        bottomCards: MutableList<Card>,
        currentLevel: Rank
    ): List<Pair<Int, Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()

        val handCandidates = hand.indices
            .filter { !hand[it].isMainTrump && hand[it].pointValue == 0 }
            .sortedBy { HeuristicEvaluator.cardStrength(hand[it], null, currentLevel) }
            .toMutableList()

        if (handCandidates.size < 8) {
            val pointCards = hand.indices
                .filter { !hand[it].isMainTrump && it !in handCandidates }
                .sortedBy { HeuristicEvaluator.cardStrength(hand[it], null, currentLevel) }
            handCandidates.addAll(pointCards)
        }

        val bottomCandidates = bottomCards.indices.sortedByDescending { idx ->
            val card = bottomCards[idx]
            var priority = 0.0
            priority += card.pointValue * 2.5
            priority += card.rank.order * 1.2
            if (card.isMainTrump) priority += 30.0
            if (card.rank == currentLevel) priority += 15.0
            priority
        }.toMutableList()

        val maxExchange = minOf(8, handCandidates.size, bottomCandidates.size)
        for (i in 0 until maxExchange) {
            pairs.add(handCandidates[i] to bottomCandidates[i])
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

    private fun handleLeadPlay(cards: List<Card>) {
        if (!isHost) return
        handleLeadPlayForPlayer(cards, 0)
    }

    private fun handleLeadPlayForPlayer(cards: List<Card>, playerIndex: Int) {
        val state = _state.value
        if (state.phase != GamePhase.PLAYING || !state.isLeadPhase) return
        if (state.currentTurnIndex != playerIndex) return

        val player = state.players.getOrNull(playerIndex) ?: return

        if (playerIndex == 0) {
            val validation = PlayValidation.validateLead(cards, player.handCards, currentMainSuit, state.currentLevel)
            if (!validation.isValid) {
                _errorMessage.value = validation.message
                return
            }
        }

        val combo = PlayCombo(
            cards = cards,
            comboType = PlayValidation.identifyCombo(cards, currentMainSuit, state.currentLevel),
            suit = CardEvaluator.effectiveSuit(cards.first(), currentMainSuit, state.currentLevel)
        )

        val newHand = player.handCards.filter { card -> cards.none { it.deckIndex == card.deckIndex } }
        updatePlayerHand(playerIndex, newHand)
        AiPlayer.recordPlay(playerIndex, cards)

        _state.value = _state.value.copy(
            isLeadPhase = false,
            currentLeadCombo = combo,
            currentRoundPlays = listOf(playerIndex to cards)
        )
        broadcastState()

        viewModelScope.launch {
            val roundPlays = mutableListOf(playerIndex to cards)

            for (offset in 1..3) {
                val aiIndex = (playerIndex + offset) % 4
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
                    AiPlayer.recordPlay(aiIndex, aiCards)
                    _state.value = _state.value.copy(currentRoundPlays = roundPlays.toList())
                    broadcastState()
                }
            }

            delay(400) // 出牌完成等待
            resolveCurrentRound(combo, roundPlays)
        }
    }

    private fun handleFollowPlay(cards: List<Card>) {
        if (!isHost) return
        handleFollowPlayForPlayer(cards, 0)
    }

    private fun handleFollowPlayForPlayer(cards: List<Card>, playerIndex: Int) {
        val state = _state.value
        if (state.phase != GamePhase.PLAYING || state.isLeadPhase) return
        if (state.currentTurnIndex != playerIndex) return

        val player = state.players.getOrNull(playerIndex) ?: return
        val leadCombo = state.currentLeadCombo ?: return

        if (playerIndex == 0) {
            val validation = PlayValidation.validateFollow(cards, player.handCards, leadCombo, currentMainSuit, state.currentLevel)
            if (!validation.isValid) {
                _errorMessage.value = validation.message
                return
            }
        }

        val newHand = player.handCards.filter { card -> cards.none { it.deckIndex == card.deckIndex } }
        updatePlayerHand(playerIndex, newHand)
        AiPlayer.recordPlay(playerIndex, cards)

        val roundPlays = state.currentRoundPlays.toMutableList()
        roundPlays.add(playerIndex to cards)
        _state.value = _state.value.copy(currentRoundPlays = roundPlays)
        broadcastState()

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
                    AiPlayer.recordPlay(aiIndex, aiCards)
                    _state.value = _state.value.copy(currentRoundPlays = roundPlays.toList())
                    broadcastState()
                }
            }

            delay(400) // 出牌完成等待
            resolveCurrentRound(leadCombo, roundPlays)
        }
    }

    private fun checkTeammateWinning(myIndex: Int, roundPlays: List<Pair<Int, List<Card>>>): Boolean {
        if (roundPlays.isEmpty()) return false
        val myTeam = _state.value.players.getOrNull(myIndex)?.teamIndex ?: return false
        val currentWinner = roundPlays.maxByOrNull { (_, cards) -> cards.maxOf { it.rank.order } }?.first ?: return false
        val winnerTeam = _state.value.players.getOrNull(currentWinner)?.teamIndex ?: return false
        return myTeam == winnerTeam
    }

    private fun resolveCurrentRound(leadCombo: PlayCombo, roundPlays: List<Pair<Int, List<Card>>>) {
        if (roundPlays.isEmpty()) return

        lastRoundComboType = leadCombo.comboType

        val result = RoundResolver.resolveRound(roundPlays, leadCombo, currentMainSuit, _state.value.currentLevel)
        val winnerTeam = _state.value.players[result.winnerIndex].teamIndex
        teamCollectedPoints[winnerTeam] = (teamCollectedPoints[winnerTeam] + result.points).coerceAtMost(200)

        val winnerName = _state.value.players[result.winnerIndex].displayName
        _state.value = _state.value.copy(
            currentRoundPlays = roundPlays,
            lastRoundPlays = roundPlays,
            lastRoundWinner = result.winnerIndex,
            roundTransitionMessage = "本轮赢家: $winnerName，获得 ${result.points} 分"
        )
        broadcastState()

        val allHandsEmpty = _state.value.players.all { it.handCards.isEmpty() }

        viewModelScope.launch {
            delay(1000) // 轮次过渡时间

            _state.value = _state.value.copy(roundTransitionMessage = null)

            if (allHandsEmpty) {
                _state.value = _state.value.copy(
                    phase = GamePhase.DIG_BOTTOM,
                    currentTurnIndex = result.winnerIndex,
                    roundNumber = _state.value.roundNumber + 1,
                    isLeadPhase = true,
                    currentLeadCombo = null,
                    currentRoundPlays = emptyList()
                )
                broadcastState()
            } else {
                _state.value = _state.value.copy(
                    currentTurnIndex = result.winnerIndex,
                    roundNumber = _state.value.roundNumber + 1,
                    isLeadPhase = true,
                    currentLeadCombo = null,
                    currentRoundPlays = emptyList()
                )
                broadcastState()

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
            AiPlayer.recordPlay(aiIndex, aiCards)

            _state.value = _state.value.copy(
                isLeadPhase = false,
                currentLeadCombo = combo,
                currentRoundPlays = listOf(aiIndex to aiCards)
            )
            broadcastState()

            val roundPlays = mutableListOf(aiIndex to aiCards)

            for (offset in 1..3) {
                val nextIndex = (aiIndex + offset) % 4
                val nextPlayer = _state.value.players.getOrNull(nextIndex) ?: continue
                if (nextPlayer.handCards.isEmpty()) continue

                if (nextPlayer.playerType == PlayerType.HUMAN && !nextPlayer.isAiControlled) {
                    _state.value = _state.value.copy(currentTurnIndex = nextIndex)
                    broadcastState()
                    return@launch
                }

                delay(400)

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
                    AiPlayer.recordPlay(nextIndex, followCards)
                    _state.value = _state.value.copy(currentRoundPlays = roundPlays.toList())
                    broadcastState()
                }
            }

            delay(400) // 出牌完成等待
            resolveCurrentRound(combo, roundPlays)
        }
    }

    private fun updatePlayerHand(playerIndex: Int, newHand: List<Card>) {
        val players = _state.value.players.toMutableList()
        players[playerIndex] = players[playerIndex].copy(handCards = newHand)
        _state.value = _state.value.copy(players = players)
    }

    private fun handleDigBottom() {
        if (!isHost) return

        val state = _state.value
        val lastRoundWinner = state.currentTurnIndex
        val comboType = lastRoundComboType
        val lastRoundCards = state.lastRoundPlays.flatMap { it.second }

        val result = DigBottomUseCase.digBottom(lastRoundWinner, comboType, lastRoundCards, state.bottomCards)
        val winnerTeam = state.players[result.diggerIndex].teamIndex
        teamCollectedPoints[winnerTeam] = (teamCollectedPoints[winnerTeam] + result.actualPoints).coerceAtMost(200)

        _state.value = state.copy(
            phase = GamePhase.ROUND_SETTLE,
            isBottomDug = true,
            teamScores = teamCollectedPoints.toList()
        )
        broadcastState()

        handleSettle()
    }

    private fun handleSettle() {
        if (!isHost) return

        val state = _state.value
        val attackerScore = teamCollectedPoints[1 - state.dealerTeam].coerceIn(0, 200)
        val result = ScoringUseCase.settle(
            attackerTotalScore = attackerScore,
            dealerTeam = state.dealerTeam,
            currentLevel = state.currentLevel
        )

        if (result.dealerChanged) {
            currentDealerIndex = (currentDealerIndex + 1) % 4
        } else {
            currentDealerIndex = (currentDealerIndex + 2) % 4
        }

        _state.value = state.copy(
            phase = GamePhase.GAME_OVER,
            currentLevel = result.newLevel,
            dealerTeam = result.newDealerTeam,
            teamScores = teamCollectedPoints.toList()
        )
        broadcastState()

        val humanTeam = state.players[0].teamIndex
        val humanWon = if (result.dealerChanged) humanTeam != state.dealerTeam else humanTeam == state.dealerTeam
        _soundEvent.value = if (humanWon) GameViewModel.SoundEvent.Win else GameViewModel.SoundEvent.Lose
    }

    private fun handleToggleAiControl() {
        val state = _state.value
        val humanPlayer = state.players.getOrNull(0) ?: return

        val newAiControlled = !humanPlayer.isAiControlled
        val players = state.players.toMutableList()
        players[0] = humanPlayer.copy(isAiControlled = newAiControlled)
        _state.value = state.copy(players = players)
        broadcastState()
    }

    // ═══ 底牌交换 ═══

    private val _selectedBottomIndices = MutableStateFlow<List<Int>>(emptyList())
    val selectedBottomIndices: StateFlow<List<Int>> = _selectedBottomIndices.asStateFlow()

    private val _selectedHandIndices = MutableStateFlow<List<Int>>(emptyList())
    val selectedHandIndices: StateFlow<List<Int>> = _selectedHandIndices.asStateFlow()

    private fun handleSelectForExchange(cardIndex: Int, isBottomCard: Boolean) {
        val state = _state.value
        if (!state.isBottomExchangePhase) return

        if (isBottomCard) {
            val current = _selectedBottomIndices.value.toMutableList()
            if (cardIndex in current) current.remove(cardIndex) else current.add(cardIndex)
            _selectedBottomIndices.value = current
        } else {
            val current = _selectedHandIndices.value.toMutableList()
            if (cardIndex in current) current.remove(cardIndex) else current.add(cardIndex)
            _selectedHandIndices.value = current
        }
    }

    private fun handleConfirmBottomExchange() {
        if (!isHost) return
        handleConfirmBottomExchangeForPlayer(0)
    }

    private fun handleConfirmBottomExchangeForPlayer(playerIndex: Int) {
        val state = _state.value
        if (!state.isBottomExchangePhase) return

        val player = state.players.getOrNull(playerIndex) ?: return
        val bottomIndices = _selectedBottomIndices.value
        val handIndices = _selectedHandIndices.value

        if (bottomIndices.size != handIndices.size) {
            _errorMessage.value = "选中的底牌和手牌数量必须相同"
            return
        }

        if (bottomIndices.isEmpty()) {
            _state.value = state.copy(
                isBottomExchangePhase = false,
                exchangePairs = emptyMap(),
                phase = GamePhase.PLAYING,
                isLeadPhase = true,
                currentTurnIndex = 0
            )
            broadcastState()
            clearExchangeSelection()
            return
        }

        val newBottomCards = state.bottomCards.toMutableList()
        val newHand = player.handCards.toMutableList()

        for (i in bottomIndices.indices) {
            val bottomIdx = bottomIndices[i]
            val handIdx = handIndices[i]
            val temp = newBottomCards[bottomIdx]
            newBottomCards[bottomIdx] = newHand[handIdx]
            newHand[handIdx] = temp
        }

        val players = state.players.toMutableList()
        players[playerIndex] = player.copy(handCards = newHand)

        _state.value = state.copy(
            players = players,
            bottomCards = newBottomCards,
            isBottomExchangePhase = false,
            exchangePairs = emptyMap(),
            phase = GamePhase.PLAYING,
            isLeadPhase = true,
            currentTurnIndex = 0
        )
        broadcastState()
        clearExchangeSelection()
    }

    private fun handleCancelBottomExchange() {
        _state.value = _state.value.copy(
            isBottomExchangePhase = false,
            exchangePairs = emptyMap(),
            phase = GamePhase.PLAYING,
            isLeadPhase = true,
            currentTurnIndex = 0
        )
        broadcastState()
        clearExchangeSelection()
    }

    private fun clearExchangeSelection() {
        _selectedBottomIndices.value = emptyList()
        _selectedHandIndices.value = emptyList()
    }

    private fun handleRestart() {
        _state.value = GameState()
        teamCollectedPoints[0] = 0
        teamCollectedPoints[1] = 0
        currentMainSuit = null
        currentCallerIndex = -1
        firstCallerIndex = -1
        currentDealerIndex = -1
        lastRoundComboType = ComboType.SINGLE
        gameStartTime = 0L
        _errorMessage.value = null
        _soundEvent.value = null
        AiPlayer.resetTracker()
        clearExchangeSelection()
        broadcastState()
    }

    // ═══ 辅助方法 ═══

    private fun findCardByDeckIndex(deckIndex: Int): Card? {
        return _state.value.players.flatMap { it.handCards }.find { it.deckIndex == deckIndex }
            ?: _state.value.bottomCards.find { it.deckIndex == deckIndex }
    }

    fun disconnect() {
        tcpManager?.disconnect()
        udpDiscovery?.stop()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
