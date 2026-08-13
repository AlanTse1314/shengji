package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 记牌器 - 跟踪已出的牌，推算剩余的牌
 */
class CardTracker {
    // 所有已出的牌（按 deckIndex 记录）
    private val allPlayedIndices = mutableSetOf<Int>()

    // 已出的牌对象（用于花色/点数统计）
    private val allPlayedCards = mutableListOf<Card>()

    // 按玩家分组的出牌记录
    private val playedByPlayer = mutableMapOf<Int, MutableList<Card>>()

    /**
     * 记录一次出牌
     */
    fun recordPlay(playerIndex: Int, cards: List<Card>) {
        playedByPlayer.getOrPut(playerIndex) { mutableListOf() }.addAll(cards)
        allPlayedCards.addAll(cards)
        cards.forEach { allPlayedIndices.add(it.deckIndex) }
    }

    /**
     * 重置记牌器
     */
    fun reset() {
        playedByPlayer.clear()
        allPlayedCards.clear()
        allPlayedIndices.clear()
    }

    /**
     * 获取某玩家已出的牌
     */
    fun getPlayedCards(playerIndex: Int): List<Card> {
        return playedByPlayer[playerIndex] ?: emptyList()
    }

    /**
     * 判断某张牌是否已出
     */
    fun isCardPlayed(deckIndex: Int): Boolean {
        return deckIndex in allPlayedIndices
    }

    /**
     * 统计已出的某个 rank 的牌数
     */
    fun getPlayedCountByRank(rank: Rank): Int {
        return allPlayedCards.count { it.rank == rank }
    }

    /**
     * 统计已出的某个花色（有效花色）的牌数
     */
    fun getPlayedCountBySuit(suit: Suit, mainSuit: Suit?, currentLevel: Rank): Int {
        return allPlayedCards.count {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
        }
    }

    /**
     * 计算某个花色剩余的牌数（不含自己手中的）
     *
     * 两副牌每花色 26 张（不含级牌时24张，级牌在主牌中）
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
     * 判断某个玩家是否可能已经绝了某个花色
     *
     * 通过观察该玩家在该花色被领出时出了其他花色来判断
     */
    fun isSuitVoid(
        playerIndex: Int,
        suit: Suit,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Boolean {
        val played = playedByPlayer[playerIndex] ?: return false
        val totalInSuit = when (suit) {
            Suit.JOKER -> 4
            else -> 26
        }
        // 如果该花色已经全部出现（手中+已出），则所有人该花色都绝了
        val playedInSuit = allPlayedCards.count {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
        }
        return playedInSuit >= totalInSuit
    }

    /**
     * 获取某个 rank 的剩余张数（不含自己手中的）
     *
     * 两副牌每 rank 8 张（大小王各4张）
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
     * 统计剩余的高分牌（10、K、5）数量（不含自己手中的）
     */
    fun getRemainingPointCards(handCards: List<Card>): Int {
        val totalPointCards = 20 // 4×5 + 8×10 + 8×K
        val playedPointCards = allPlayedCards.count { it.pointValue > 0 }
        val handPointCards = handCards.count { it.pointValue > 0 }
        return (totalPointCards - playedPointCards - handPointCards).coerceAtLeast(0)
    }

    /**
     * 统计某个花色的大牌（A、K）剩余数量（不含自己手中的）
     */
    fun getRemainingHighCards(
        suit: Suit,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        val highRanks = setOf(Rank.ACE, Rank.KING)
        val totalHigh = 4 // 每花色 2 张 A + 2 张 K = 4 张
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
     */
    fun getRemainingTrumpCount(
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        // 两副牌主牌总数：4王 + 主花色26张 + 其他花色级牌6张 = 36张（有主时）
        val totalTrump = if (mainSuit != null) 36 else 8 // 无主只有8张（4王+4级牌）
        val playedTrump = allPlayedCards.count {
            CardEvaluator.isMainTrump(it, mainSuit, currentLevel)
        }
        val handTrump = handCards.count {
            CardEvaluator.isMainTrump(it, mainSuit, currentLevel)
        }
        return (totalTrump - playedTrump - handTrump).coerceAtLeast(0)
    }
}
