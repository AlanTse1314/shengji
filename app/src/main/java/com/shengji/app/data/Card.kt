package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 扑克牌数据类
 *
 * 两副牌共 108 张（含 4 张王牌），每张牌通过 [deckIndex] 唯一标识。
 * 主牌属性 [isMainTrump] 在发牌/亮主后由 GameState 标记，运行时动态变化。
 *
 * @property suit 花色（♠ ♥ ♣ ♦）
 * @property rank 点数（2~A、小王、大王）
 * @property isMainTrump 是否已被标记为主牌，用于 UI 高亮展示
 * @property isJoker 是否为王牌（小王或大王）
 * @property isBigJoker 是否为大王
 * @property isPlayed 是否已打出（不再在手中）
 * @property deckIndex 牌在原始牌堆中的索引（0..107），全局唯一标识
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
     * 显示名称
     *
     * 王牌显示为"大王"/"小王"，普通牌显示为"花色符号+点数"（如 "♠A"、"♥10"）。
     */
    val displayName: String
        get() = if (isJoker) rank.display else "${suit.symbol}${rank.display}"

    /**
     * 牌面分值
     *
     * 5 = 5 分，10 = 10 分，K = 10 分，其余为 0。
     * 两副牌总分 200 分，闲家抓分决定升级幅度。
     */
    val pointValue: Int
        get() = when (rank) {
            Rank.FIVE -> 5
            Rank.TEN -> 10
            Rank.KING -> 10
            else -> 0
        }

    /**
     * 有效花色（仅区分王牌 vs 非王牌）
     *
     * 王牌返回 [Suit.JOKER]，其余返回原始花色。
     * 注意：此属性不考虑主牌/级牌上下文，需要上下文时请使用 [CardEvaluator.effectiveSuit]。
     */
    val effectiveSuit: Suit
        get() = if (isJoker) Suit.JOKER else suit

    /**
     * 判断是否为当前级牌
     *
     * 级牌是本局打的数字牌（如打 5 则所有 5 都是级牌），属于主牌。
     * 王牌不是级牌。
     *
     * @param currentLevel 当前级牌点数
     * @return 是级牌返回 true
     */
    fun isLevelCard(currentLevel: Rank): Boolean = rank == currentLevel && !isJoker

    /**
     * 生成标记为主牌的副本
     *
     * 返回新 Card 实例，[isMainTrump] 设为 true，其余属性不变。
     */
    fun asMainTrump(): Card = copy(isMainTrump = true)
}
