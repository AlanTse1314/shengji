package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 记牌器 - 跟踪已出的牌，推算剩余牌分布
 *
 * 用于 AI 决策和玩家辅助。记录每轮各家出牌，提供剩余牌数统计。
 * 两副牌基准：每点数 8 张，每花色 26 张（不含王牌），王牌 4 张。
 */
class CardTracker {
    /** 所有已出牌的 deckIndex 集合，用于快速查重 */
    private val allPlayedIndices = mutableSetOf<Int>()

    /** 所有已出牌对象，用于花色/点数统计 */
    private val allPlayedCards = mutableListOf<Card>()

    /** 按玩家索引分组的出牌记录 */
    private val playedByPlayer = mutableMapOf<Int, MutableList<Card>>()

    /**
     * 记录一次出牌
     *
     * @param playerIndex 出牌玩家索引
     * @param cards 本次出的牌
     */
    fun recordPlay(playerIndex: Int, cards: List<Card>) {
        playedByPlayer.getOrPut(playerIndex) { mutableListOf() }.addAll(cards)
        allPlayedCards.addAll(cards)
        cards.forEach { allPlayedIndices.add(it.deckIndex) }
    }

    /**
     * 重置记牌器（新局开始时调用）
     */
    fun reset() {
        playedByPlayer.clear()
        allPlayedCards.clear()
        allPlayedIndices.clear()
    }

    /**
     * 获取某玩家已出的全部牌
     *
     * @param playerIndex 玩家索引
     * @return 该玩家已出的牌列表，未出过返回空列表
     */
    fun getPlayedCards(playerIndex: Int): List<Card> {
        return playedByPlayer[playerIndex] ?: emptyList()
    }

    /**
     * 判断某张牌是否已出
     *
     * @param deckIndex 牌的全局唯一索引
     * @return 已出返回 true
     */
    fun isCardPlayed(deckIndex: Int): Boolean {
        return deckIndex in allPlayedIndices
    }

    /**
     * 统计已出的某个点数的牌数
     *
     * 两副牌每点数共 8 张（4 花色 × 2 副），大小王各 4 张。
     *
     * @param rank 目标点数
     * @return 已出的张数
     */
    fun getPlayedCountByRank(rank: Rank): Int {
        return allPlayedCards.count { it.rank == rank }
    }

    /**
     * 统计已出的某个有效花色的牌数
     *
     * @param suit 目标有效花色
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 已出的张数
     */
    fun getPlayedCountBySuit(suit: Suit, mainSuit: Suit?, currentLevel: Rank): Int {
        return allPlayedCards.count {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
        }
    }

    /**
     * 计算某个花色在场上的剩余牌数（不含自己手中的）
     *
     * 两副牌每花色 26 张（4 花色 × 13 点数 × 2 副），王牌共 4 张。
     *
     * @param suit 目标花色
     * @param handCards 自己的手牌（排除不计）
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 剩余牌数（≥ 0）
     */
    fun getRemainingSuitCount(
        suit: Suit,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        val totalInSuit = when (suit) {
            Suit.JOKER -> 4
            else -> 26
        }
        val playedInSuit = getPlayedCountBySuit(suit, mainSuit, currentLevel)
        val handInSuit = handCards.count {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
        }
        return (totalInSuit - playedInSuit - handInSuit).coerceAtLeast(0)
    }

    /**
     * 计算某个花色在其他玩家手中的剩余牌数
     *
     * 等同于 [getRemainingSuitCount]，语义上强调"其他玩家"。
     */
    fun getRemainingSuitInOthers(
        suit: Suit,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        return getRemainingSuitCount(suit, handCards, mainSuit, currentLevel)
    }

    /**
     * 判断某个花色是否已经全部出现
     *
     * 当某花色的总张数（两副牌）全部被打出或在自己手中时，其他人不可能还有该花色。
     *
     * @param playerIndex 目标玩家索引（当前未使用，保留接口）
     * @param suit 目标花色
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 已全部出现返回 true
     */
    fun isSuitVoid(
        playerIndex: Int,
        suit: Suit,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Boolean {
        val totalInSuit = when (suit) {
            Suit.JOKER -> 4
            else -> 26
        }
        val playedInSuit = allPlayedCards.count {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
        }
        return playedInSuit >= totalInSuit
    }

    /**
     * 获取某个点数在场上的剩余张数（不含自己手中的）
     *
     * 两副牌每点数 8 张，大小王各 4 张。
     *
     * @param rank 目标点数
     * @param handCards 自己的手牌
     * @return 剩余张数（≥ 0）
     */
    fun getRemainingCountByRank(
        rank: Rank,
        handCards: List<Card>
    ): Int {
        val total = if (rank == Rank.BIG_JOKER || rank == Rank.SMALL_JOKER) 4 else 8
        val played = getPlayedCountByRank(rank)
        val inHand = handCards.count { it.rank == rank }
        return (total - played - inHand).coerceAtLeast(0)
    }

    /**
     * 统计剩余的高分牌数量（不含自己手中的）
     *
     * 高分牌：5（5 分 × 8 = 40 分）、10（10 分 × 8 = 80 分）、K（10 分 × 8 = 80 分），
     * 合计 20 张高分牌，总分 200。
     *
     * @param handCards 自己的手牌
     * @return 剩余高分牌数量
     */
    fun getRemainingPointCards(handCards: List<Card>): Int {
        val totalPointCards = 20 // 4×5 + 8×10 + 8×K
        val playedPointCards = allPlayedCards.count { it.pointValue > 0 }
        val handPointCards = handCards.count { it.pointValue > 0 }
        return (totalPointCards - playedPointCards - handPointCards).coerceAtLeast(0)
    }

    /**
     * 统计某个花色的大牌（A、K）剩余数量（不含自己手中的）
     *
     * 每花色 A 和 K 各 2 张（两副牌），共 4 张大牌。
     *
     * @param suit 目标花色
     * @param handCards 自己的手牌
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 剩余大牌数量（≥ 0）
     */
    fun getRemainingHighCards(
        suit: Suit,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        val highRanks = setOf(Rank.ACE, Rank.KING)
        val totalHigh = 4
        val playedHigh = allPlayedCards.count {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit &&
                    it.rank in highRanks
        }
        val handHigh = handCards.count {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit &&
                    it.rank in highRanks
        }
        return (totalHigh - playedHigh - handHigh).coerceAtLeast(0)
    }

    /**
     * 计算剩余主牌数量（不含自己手中的）
     *
     * 有主局：4 王 + 主花色 26 张 + 其他花色级牌 6 张 = 36 张主牌。
     * 无主局：4 王 + 4 级牌 = 8 张主牌。
     *
     * @param handCards 自己的手牌
     * @param mainSuit 当前主花色（null 表示无主局）
     * @param currentLevel 当前级牌
     * @return 剩余主牌数量（≥ 0）
     */
    fun getRemainingTrumpCount(
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        val totalTrump = if (mainSuit != null) 36 else 8
        val playedTrump = allPlayedCards.count {
            CardEvaluator.isMainTrump(it, mainSuit, currentLevel)
        }
        val handTrump = handCards.count {
            CardEvaluator.isMainTrump(it, mainSuit, currentLevel)
        }
        return (totalTrump - playedTrump - handTrump).coerceAtLeast(0)
    }
}
