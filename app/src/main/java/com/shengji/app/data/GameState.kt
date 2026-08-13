package com.shengji.app.data

import com.shengji.app.enums.GamePhase
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 游戏全局状态
 *
 * @param phase 当前游戏阶段
 * @param mainSuit 主花色（叫主后确定）
 * @param currentLevel 当前级牌（升级的目标点数）
 * @param bottomCards 底牌列表
 * @param isBottomDug 是否已抠底
 * @param teamScores 各队得分（索引对应队伍编号）
 * @param roundNumber 当前出牌轮次
 * @param players 四位玩家列表
 * @param currentTurnIndex 当前出牌玩家索引
 * @param leadingSuit 当前轮首出花色
 * @param dealerTeam 庄家队伍编号（0 或 1）
 * @param isLeadPhase 当前是否为首出阶段（true=首出，false=跟牌中）
 * @param currentLeadCombo 当前轮首出牌型
 * @param currentRoundPlays 当前轮各家出牌记录
 */
data class GameState(
    val phase: GamePhase = GamePhase.IDLE,
    val mainSuit: Suit? = null,
    val currentLevel: Rank = Rank.TWO,
    val bottomCards: List<Card> = emptyList(),
    val isBottomDug: Boolean = false,
    val teamScores: List<Int> = listOf(0, 0),
    val roundNumber: Int = 0,
    val players: List<Player> = emptyList(),
    val currentTurnIndex: Int = 0,
    val leadingSuit: Suit? = null,
    val dealerTeam: Int = 0,
    val isLeadPhase: Boolean = true,
    val currentLeadCombo: PlayCombo? = null,
    val currentRoundPlays: List<Pair<Int, List<Card>>> = emptyList(),
    val lastRoundPlays: List<Pair<Int, List<Card>>> = emptyList(), // 上一轮出牌记录
    val lastRoundWinner: Int = -1, // 上一轮赢家索引
    val dealingCardIndex: Int = -1, // 当前发到第几张，-1 表示发牌完成
    val dealingTargetPlayerIndex: Int = -1, // 当前飞牌目标玩家索引
    val dealingCurrentCard: Card? = null, // 当前正在飞的牌（用于动画显示）
    val totalCardsToDeal: Int = 100, // 总共要发的牌数
    val roundTransitionMessage: String? = null, // 轮次过渡提示消息
    val isBottomExchangePhase: Boolean = false, // 是否在换底牌阶段
    val exchangePairs: Map<Int, Int> = emptyMap(), // 底牌索引 → 手牌索引 的配对映射
    val callMainDisplayCards: List<Card>? = null, // 亮主展示的牌
    val callMainDisplayPlayer: String? = null, // 亮主的玩家名称
    val callMainDisplaySuit: String? = null // 亮主确定的花色名称
) {
    /**
     * 当前出牌玩家
     */
    val currentPlayer: Player?
        get() = players.getOrNull(currentTurnIndex)

    /**
     * 庄家玩家
     */
    val dealer: Player?
        get() = players.firstOrNull { it.isDealer }

    /**
     * 级牌显示名称
     */
    val levelDisplay: String
        get() = currentLevel.display
}
