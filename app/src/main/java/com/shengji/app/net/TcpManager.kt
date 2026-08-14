package com.shengji.app.net

import android.util.Log
import com.google.gson.Gson
import com.shengji.app.data.*
import com.shengji.app.enums.*
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket

/**
 * TCP 通信管理器
 *
 * 房主 = TCP 服务端（接受多个客户端连接）
 * 加入者 = TCP 客户端（连接到房主）
 *
 * 消息格式：4 字节长度头 + JSON body
 */
class TcpManager {

    companion object {
        private const val TAG = "TcpManager"
        private const val TCP_PORT = 9877
        private const val HEARTBEAT_INTERVAL = 2000L
        private const val CONNECTION_TIMEOUT = 5000
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    var isHost = false
        private set

    @Volatile
    var isConnected = false
        private set

    private var serverSocket: ServerSocket? = null
    private val clientConnections = mutableListOf<BufferedWriter>()
    private var hostSocket: Socket? = null
    private var hostWriter: BufferedWriter? = null

    /** 消息接收回调 */
    var onMessageReceived: ((TcpMessage) -> Unit)? = null

    /** 连接状态变化回调 */
    var onConnectionChanged: ((Boolean) -> Unit)? = null

    /** 已连接玩家数（房主用） */
    val connectedPlayerCount: Int get() = clientConnections.size + 1  // +1 包含房主自己

    // ═══ 房主（服务端） ═══

    /**
     * 创建房间（启动 TCP 服务端）
     */
    fun createRoom(deviceName: String) {
        isHost = true
        isConnected = true

        scope.launch {
            try {
                serverSocket = ServerSocket(TCP_PORT)
                serverSocket!!.reuseAddress = true
                Log.d(TAG, "TCP 服务端启动，端口: $TCP_PORT")

                onConnectionChanged?.invoke(true)

                while (isConnected) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        clientSocket.soTimeout = CONNECTION_TIMEOUT
                        handleClientConnection(clientSocket, deviceName)
                    } catch (e: Exception) {
                        if (isConnected) Log.e(TAG, "接受连接失败", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "服务端启动失败", e)
                isConnected = false
                onConnectionChanged?.invoke(false)
            }
        }

        // 心跳
        scope.launch {
            while (isConnected) {
                delay(HEARTBEAT_INTERVAL)
                broadcastToClients(TcpMessage(type = TcpMessageType.HEARTBEAT))
            }
        }
    }

    /**
     * 处理客户端连接
     */
    private fun handleClientConnection(socket: Socket, hostDeviceName: String) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

        // 先计算玩家索引，再添加到列表
        val playerIndex = synchronized(clientConnections) {
            clientConnections.size + 1  // +1 因为房主是 0
        }

        synchronized(clientConnections) {
            clientConnections.add(writer)
        }

        scope.launch {
            try {
                // 发送加入响应
                val response = TcpMessage(
                    type = TcpMessageType.JOIN_RESPONSE,
                    payload = gson.toJson(JoinResponsePayload(
                        success = true,
                        playerIndex = playerIndex,
                        message = "欢迎加入！"
                    )),
                    playerIndex = playerIndex,
                    deviceName = hostDeviceName
                )
                writeMessage(writer, response)

                // 通知其他玩家
                broadcastToClients(TcpMessage(
                    type = TcpMessageType.PLAYER_JOINED,
                    playerIndex = playerIndex,
                    deviceName = ""
                ), exclude = writer)

                // 持续接收消息
                while (isConnected) {
                    val line = reader.readLine() ?: break
                    val message = gson.fromJson(line, TcpMessage::class.java)
                    onMessageReceived?.invoke(message)
                }
            } catch (e: Exception) {
                Log.d(TAG, "客户端断开: ${e.message}")
            } finally {
                synchronized(clientConnections) {
                    clientConnections.remove(writer)
                }
                socket.close()
                broadcastToClients(TcpMessage(
                    type = TcpMessageType.PLAYER_LEFT,
                    playerIndex = playerIndex
                ))
            }
        }
    }

    /**
     * 向所有客户端广播消息
     */
    fun broadcastToClients(message: TcpMessage, exclude: BufferedWriter? = null) {
        val json = gson.toJson(message)
        synchronized(clientConnections) {
            clientConnections.forEach { writer ->
                if (writer != exclude) {
                    try {
                        writeMessage(writer, json)
                    } catch (e: Exception) {
                        Log.e(TAG, "广播失败", e)
                    }
                }
            }
        }
    }

    // ═══ 加入者（客户端） ═══

    /**
     * 加入房间（连接到房主）
     */
    fun joinRoom(hostAddress: String, deviceName: String) {
        isHost = false

        scope.launch {
            try {
                val socket = Socket(hostAddress, TCP_PORT)
                socket.soTimeout = CONNECTION_TIMEOUT
                hostSocket = socket

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                hostWriter = writer

                // 发送加入请求
                val joinMsg = TcpMessage(
                    type = TcpMessageType.JOIN,
                    deviceName = deviceName
                )
                writeMessage(writer, joinMsg)

                // 等待响应
                val responseLine = withTimeout(5000) { reader.readLine() }
                val response = gson.fromJson(responseLine, TcpMessage::class.java)

                if (response.type == TcpMessageType.JOIN_RESPONSE) {
                    isConnected = true
                    onConnectionChanged?.invoke(true)
                    Log.d(TAG, "加入房间成功")

                    // 持续接收消息
                    while (isConnected) {
                        try {
                            val line = reader.readLine() ?: break
                            val message = gson.fromJson(line, TcpMessage::class.java)
                            onMessageReceived?.invoke(message)
                        } catch (e: Exception) {
                            if (isConnected) Log.e(TAG, "接收消息失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加入房间失败", e)
                isConnected = false
                onConnectionChanged?.invoke(false)
            }
        }

        // 心跳
        scope.launch {
            while (isConnected) {
                delay(HEARTBEAT_INTERVAL)
                try {
                    sendToHost(TcpMessage(type = TcpMessageType.HEARTBEAT))
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * 向房主发送消息
     */
    fun sendToHost(message: TcpMessage) {
        hostWriter?.let { writer ->
            try {
                writeMessage(writer, message)
            } catch (e: Exception) {
                Log.e(TAG, "发送失败", e)
            }
        }
    }

    // ═══ 通用 ═══

    /**
     * 发送游戏状态给所有人（房主用）
     */
    fun broadcastGameState(gameState: GameState) {
        val stateJson = gson.toJson(gameState.toSerializable())
        broadcastToClients(TcpMessage(
            type = TcpMessageType.GAME_STATE,
            payload = stateJson
        ))
    }

    /**
     * 发送玩家操作（加入者用）
     */
    fun sendPlayerAction(action: PlayerActionPayload) {
        sendToHost(TcpMessage(
            type = TcpMessageType.PLAYER_ACTION,
            payload = gson.toJson(action)
        ))
    }

    /**
     * 发送开始游戏指令（房主用）
     */
    fun broadcastStartGame() {
        broadcastToClients(TcpMessage(type = TcpMessageType.START_GAME))
    }

    private fun writeMessage(writer: BufferedWriter, message: TcpMessage) {
        writeMessage(writer, gson.toJson(message))
    }

    private fun writeMessage(writer: BufferedWriter, json: String) {
        writer.write(json)
        writer.newLine()
        writer.flush()
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        isConnected = false
        isHost = false

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        try {
            hostSocket?.close()
        } catch (_: Exception) {}

        synchronized(clientConnections) {
            clientConnections.clear()
        }
        serverSocket = null
        hostSocket = null
        hostWriter = null

        onConnectionChanged?.invoke(false)
        Log.d(TAG, "TCP 断开")
    }

    fun release() {
        disconnect()
        scope.cancel()
    }
}

// ═══ GameState 序列化辅助 ═══

/**
 * 可序列化的 GameState（用于网络传输）
 */
data class SerializableGameState(
    val phase: String,
    val mainSuit: String?,
    val currentLevel: String,
    val bottomCards: List<Int>,  // deckIndex
    val isBottomDug: Boolean,
    val teamScores: List<Int>,
    val roundNumber: Int,
    val players: List<SerializablePlayer>,
    val currentTurnIndex: Int,
    val dealerTeam: Int,
    val isLeadPhase: Boolean
)

data class SerializablePlayer(
    val playerId: String,
    val displayName: String,
    val playerType: String,
    val handCardIndices: List<Int>,  // deckIndex
    val score: Int,
    val teamIndex: Int
)

fun GameState.toSerializable(): SerializableGameState {
    return SerializableGameState(
        phase = phase.name,
        mainSuit = mainSuit?.name,
        currentLevel = currentLevel.name,
        bottomCards = bottomCards.map { it.deckIndex },
        isBottomDug = isBottomDug,
        teamScores = teamScores,
        roundNumber = roundNumber,
        players = players.map { it.toSerializable() },
        currentTurnIndex = currentTurnIndex,
        dealerTeam = dealerTeam,
        isLeadPhase = isLeadPhase
    )
}

fun Player.toSerializable(): SerializablePlayer {
    return SerializablePlayer(
        playerId = playerId,
        displayName = displayName,
        playerType = playerType.name,
        handCardIndices = handCards.map { it.deckIndex },
        score = score,
        teamIndex = teamIndex
    )
}

/**
 * 反序列化：SerializableGameState → GameState
 */
fun SerializableGameState.toGameState(): GameState {
    val allCards = DeckGenerator.generateDoubleDeck()
    val cardMap = allCards.associateBy { it.deckIndex }

    val gamePhase = try { GamePhase.valueOf(phase) } catch (_: Exception) { GamePhase.IDLE }
    val gameMainSuit = mainSuit?.let { try { Suit.valueOf(it) } catch (_: Exception) { null } }
    val gameCurrentLevel = try { Rank.valueOf(currentLevel) } catch (_: Exception) { Rank.TWO }

    val gameBottomCards = bottomCards.mapNotNull { cardMap[it] }

    val gamePlayers = players.map { sp ->
        val handCards = sp.handCardIndices.mapNotNull { idx ->
            cardMap[idx]?.let { card ->
                if (CardEvaluator.isMainTrump(card, gameMainSuit, gameCurrentLevel))
                    card.asMainTrump()
                else card
            }
        }
        Player(
            playerId = sp.playerId,
            displayName = sp.displayName,
            playerType = try { PlayerType.valueOf(sp.playerType) } catch (_: Exception) { PlayerType.AI },
            handCards = handCards,
            score = sp.score,
            teamIndex = sp.teamIndex
        )
    }

    return GameState(
        phase = gamePhase,
        mainSuit = gameMainSuit,
        currentLevel = gameCurrentLevel,
        bottomCards = gameBottomCards,
        isBottomDug = isBottomDug,
        teamScores = teamScores,
        roundNumber = roundNumber,
        players = gamePlayers,
        currentTurnIndex = currentTurnIndex,
        dealerTeam = dealerTeam,
        isLeadPhase = isLeadPhase,
        dealingCardIndex = -1
    )
}
