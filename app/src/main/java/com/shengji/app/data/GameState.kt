package com.shengji.app.data

import com.shengji.app.enums.GamePhase
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 游戏全局状态
 *
 * 通过 [copy] 方法实现不可变状态更新，所有 UI 和逻辑层通过读取此状态驱动渲染和决策。
 *
 * @property phase 当前游戏阶段，控制 UI 显示和可用操作
 * @property mainSuit 主花色，叫主后确定；null 表示无主局
 * @property currentLevel 当前级牌（如打 5 则为 [Rank.FIVE]），决定级牌归属和升级目标
 * @property bottomCards 底牌列表（8 张），扣底后全程盖住，结算时翻开
 * @property isBottomDug 是否已抠底（闲家赢最后一轮则抠底）
 * @property teamScores 各队得分，索引 0/1 对应两支队伍
 * @property roundNumber 当前出牌轮次计数
 * @property players 四位玩家列表，索引 0 为当前客户端玩家
 * @property currentTurnIndex 当前出牌玩家在 [players] 中的索引
 * @property leadingSuit 当前轮首出花色（已废弃，使用 currentLeadCombo.suit）
 * @property dealerTeam 庄家队伍编号（0 或 1）
 * @property isLeadPhase 当前是否为首出阶段（true=等待首出，false=跟牌进行中）
 * @property currentLeadCombo 当前轮首出牌组合，null 表示本轮尚未有人出牌
 * @property currentRoundPlays 当前轮各家出牌记录，格式为 (玩家索引, 出的牌列表)
 * @property lastRoundPlays 上一轮出牌记录，用于 UI 展示上轮出牌
 * @property lastRoundWinner 上一轮赢家索引，-1 表示无上轮
 * @property dealingCardIndex 当前发到第几张牌，-1 表示发牌完成
 * @property dealingTargetPlayerIndex 当前发牌目标玩家索引，用于飞牌动画
 * @property dealingCurrentCard 当前正在飞的牌，用于动画显示
 * @property totalCardsToDeal 本局总共要发的牌数
 * @property roundTransitionMessage 轮次过渡提示消息（如"你赢了！"），短暂显示后清除
 * @property isBottomExchangePhase 是否在换底牌阶段（庄家拿底牌后选 8 张扣回）
 * @property exchangePairs 底牌索引 → 手牌索引的配对映射，用于换底牌 UI
 * @property callMainDisplayCards 亮主展示的牌（动画用）
 * @property callMainDisplayPlayer 亮主的玩家名称
 * @property callMainDisplaySuit 亮主确定的花色名称
 * @property currentCallerIndex 当前叫主者索引，-1 表示本轮没人叫主
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
    val lastRoundPlays: List<Pair<Int, List<Card>>> = emptyList(),
    val lastRoundWinner: Int = -1,
    val dealingCardIndex: Int = -1,
    val dealingTargetPlayerIndex: Int = -1,
    val dealingCurrentCard: Card? = null,
    val totalCardsToDeal: Int = 100,
    val roundTransitionMessage: String? = null,
    val isBottomExchangePhase: Boolean = false,
    val exchangePairs: Map<Int, Int> = emptyMap(),
    val callMainDisplayCards: List<Card>? = null,
    val callMainDisplayPlayer: String? = null,
    val callMainDisplaySuit: String? = null,
    val currentCallerIndex: Int = -1
) {
    /**
     * 当前出牌玩家
     *
     * 通过 [currentTurnIndex] 从 [players] 中获取，越界时返回 null。
     */
    val currentPlayer: Player?
        get() = players.getOrNull(currentTurnIndex)

    /**
     * 庄家玩家
     *
     * 遍历 [players] 查找 [Player.isDealer] 为 true 的玩家。
     */
    val dealer: Player?
        get() = players.firstOrNull { it.isDealer }

    /**
     * 级牌显示名称
     *
     * 如打 5 时返回 "5"，打 A 时返回 "A"。
     */
    val levelDisplay: String
        get() = currentLevel.display
}
