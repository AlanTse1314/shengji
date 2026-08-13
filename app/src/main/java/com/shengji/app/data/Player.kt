package com.shengji.app.data

import com.shengji.app.enums.PlayerType
import com.shengji.app.enums.Rank

/**
 * 玩家数据类
 *
 * @param playerId 玩家唯一标识
 * @param displayName 显示名称
 * @param playerType 玩家类型（人类 / AI）
 * @param handCards 手牌列表
 * @param playedCards 已出的卡牌（按出牌顺序）
 * @param score 本局得分
 * @param isDealer 是否为庄家
 * @param teamIndex 所属队伍索引（0 或 1，两人一队）
 * @param isAiControlled 是否由 AI 托管（人类玩家可随时切换）
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
     * 是否出完手牌
     */
    val hasNoCards: Boolean get() = handCards.isEmpty()

    /**
     * 按花色和点数排序手牌（不考虑级牌）
     */
    fun sortedHand(): List<Card> = handCards.sortedWith(
        compareByDescending<Card> { it.isMainTrump }
            .thenByDescending { it.isJoker }
            .thenBy { it.suit.ordinal }
            .thenByDescending { it.rank.order }
    )

    /**
     * 按花色和点数排序手牌（级牌排在同花色 A 前面）
     */
    fun sortedHand(currentLevel: Rank): List<Card> = handCards.sortedWith(
        compareByDescending<Card> { it.isMainTrump }
            .thenByDescending { it.isJoker }
            .thenBy { it.suit.ordinal }
            .thenByDescending { if (it.rank == currentLevel && !it.isJoker) 14 else it.rank.order }
    )
}
