package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 扑克牌数据类
 *
 * @param suit 花色
 * @param rank 点数
 * @param isMainTrump 是否为主牌（运行时由 GameState 确定后标记）
 * @param isJoker 是否为王牌
 * @param isBigJoker 是否为大王
 * @param isPlayed 是否已打出
 * @param deckIndex 牌在原始牌堆中的索引（0..107），用于唯一标识
 */
data class Card(
    val suit: Suit,
    val rank: Rank,
    val isMainTrump: Boolean = false,
    val isJoker: Boolean = rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER,
    val isBigJoker: Boolean = rank == Rank.BIG_JOKER,
    val isPlayed: Boolean = false,
    val deckIndex: Int = 0
) {
    /**
     * 显示名称，如 "♠A"、"大王"
     */
    val displayName: String
        get() = if (isJoker) rank.display else "${suit.symbol}${rank.display}"

    /**
     * 牌面分值：5=5分, 10=10分, K=10分, 其余=0
     */
    val pointValue: Int
        get() = when (rank) {
            Rank.FIVE -> 5
            Rank.TEN -> 10
            Rank.KING -> 10
            else -> 0
        }

    /**
     * 有效花色（王牌视为特殊花色 JOKER）
     */
    val effectiveSuit: Suit
        get() = if (isJoker) Suit.JOKER else suit

    /**
     * 是否为当前级牌
     */
    fun isLevelCard(currentLevel: Rank): Boolean = rank == currentLevel && !isJoker

    fun isJokerCard(): Boolean = isJoker

    /**
     * 生成标记为主牌的副本
     */
    fun asMainTrump(): Card = copy(isMainTrump = true)
}
