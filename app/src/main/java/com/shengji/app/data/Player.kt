package com.shengji.app.data

import com.shengji.app.enums.PlayerType
import com.shengji.app.enums.Rank

/**
 * 玩家数据类
 *
 * 封装单个玩家的全部状态，包括手牌、得分、队伍归属等。
 * 通过 [copy] 方法实现不可变状态更新。
 *
 * @property playerId 玩家唯一标识
 * @property displayName 显示名称（如 "玩家1"、"电脑A"）
 * @property playerType 玩家类型（人类 / AI），决定出牌方式
 * @property handCards 当前手牌列表
 * @property playedCards 本局已出的牌（按出牌顺序记录）
 * @property score 本局得分（收集的分牌分值）
 * @property isDealer 是否为本局庄家
 * @property teamIndex 所属队伍索引（0 或 1，同队两人为队友）
 * @property isAiControlled 是否由 AI 托管（人类玩家可随时切换为 AI 托管）
 */
data class Player(
    val playerId: String,
    val displayName: String,
    val playerType: PlayerType,
    val handCards: List<Card> = emptyList(),
    val playedCards: List<Card> = emptyList(),
    val score: Int = 0,
    val isDealer: Boolean = false,
    val teamIndex: Int = 0,
    val isAiControlled: Boolean = playerType == PlayerType.AI
) {
    /**
     * 手牌数量
     */
    val handCardCount: Int get() = handCards.size

    /**
     * 是否已出完手牌
     */
    val hasNoCards: Boolean get() = handCards.isEmpty()

    /**
     * 按花色和点数排序手牌（不考虑级牌特殊位置）
     *
     * 排序优先级：主牌 > 王牌 > 按花色分组 > 按点数降序。
     *
     * @return 排序后的新列表，不修改原手牌
     */
    fun sortedHand(): List<Card> = handCards.sortedWith(
        compareByDescending<Card> { it.isMainTrump }
            .thenByDescending { it.isJoker }
            .thenBy { it.suit.ordinal }
            .thenByDescending { it.rank.order }
    )

    /**
     * 按花色和点数排序手牌（级牌排在同花色 A 前面）
     *
     * 级牌权重设为 14（高于 A 的 13），使其在同花色内排在最前。
     *
     * @param currentLevel 当前级牌，用于判断级牌位置
     * @return 排序后的新列表，不修改原手牌
     */
    fun sortedHand(currentLevel: Rank): List<Card> = handCards.sortedWith(
        compareByDescending<Card> { it.isMainTrump }
            .thenByDescending { it.isJoker }
            .thenBy { it.suit.ordinal }
            .thenByDescending { if (it.rank == currentLevel && !it.isJoker) 14 else it.rank.order }
    )
}
