package com.shengji.app.net


/**
 * 网络消息协议
 *
 * 所有 TCP/UDP 消息的统一格式。
 */

// ═══ UDP 广播消息 ═══

/**
 * UDP 广播包（设备发现用）
 */
data class BroadcastMessage(
    val deviceName: String,
    val hostAddress: String,
    val roomStatus: String,  // "waiting", "playing", "full"
    val playerCount: Int,
    val maxPlayers: Int = 4,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 发现到的房间信息
 */
data class DiscoveredRoom(
    val deviceName: String,
    val hostAddress: String,
    val roomStatus: String,
    val playerCount: Int,
    val maxPlayers: Int,
    var lastSeen: Long = System.currentTimeMillis()
) {
    val isAlive: Boolean
        get() = System.currentTimeMillis() - lastSeen < 3000
}

// ═══ TCP 消息 ═══

/**
 * TCP 消息类型枚举
 */
enum class TcpMessageType {
    /** 加入请求 */
    JOIN,
    /** 加入响应 */
    JOIN_RESPONSE,
    /** 玩家操作（出牌、叫主等） */
    PLAYER_ACTION,
    /** 全量游戏状态同步 */
    GAME_STATE,
    /** 玩家加入通知 */
    PLAYER_JOINED,
    /** 玩家离开通知 */
    PLAYER_LEFT,
    /** 心跳 */
    HEARTBEAT,
    /** 房间关闭 */
    ROOM_CLOSED,
    /** 开始游戏 */
    START_GAME
}

/**
 * TCP 消息包装
 */
data class TcpMessage(
    val type: TcpMessageType,
    val payload: String = "",      // JSON payload
    val playerIndex: Int = -1,
    val deviceName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 加入响应 payload
 */
data class JoinResponsePayload(
    val success: Boolean,
    val playerIndex: Int,
    val message: String = ""
)

/**
 * 玩家操作 payload
 */
data class PlayerActionPayload(
    val actionType: String,  // "Deal", "CallMain", "PassCallMain", "LeadPlay", "FollowPlay", etc.
    val cardIndices: List<Int> = emptyList(),  // deckIndex 列表
    val extra: String = ""  // 额外参数（JSON 字符串）
)
