package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 牌面评估工具
 *
 * 用于判断牌的主牌属性、比较大小、计算牌型等。
 * 所有方法均为纯函数，无副作用。
 */
object CardEvaluator {

    /**
     * 判断一张牌是否为主牌
     *
     * 主牌包括：大小王 + 主花色的所有牌 + 非主花色中的级牌
     */
    fun isMainTrump(card: Card, mainSuit: Suit?, currentLevel: Rank): Boolean {
        if (card.isJoker) return true
        if (mainSuit == null) return false
        if (card.suit == mainSuit) return true
        return card.rank == currentLevel
    }

    /**
     * 获取牌在比较中的"有效花色"
     *
     * 主牌统一视为 mainSuit（用于跟牌判断）
     */
    fun effectiveSuit(card: Card, mainSuit: Suit?, currentLevel: Rank): Suit {
        if (card.isJoker) return mainSuit ?: Suit.JOKER
        if (mainSuit != null && isMainTrump(card, mainSuit, currentLevel)) return mainSuit
        return card.suit
    }

    /**
     * 计算牌的比较权重（越大越强）
     *
     * 主牌权重高于副牌。同花色内按点数比较。
     */
    fun comparisonWeight(card: Card, mainSuit: Suit?, currentLevel: Rank): Int {
        val isTrump = isMainTrump(card, mainSuit, currentLevel)

        return when {
            // 大小王
            card.rank == Rank.BIG_JOKER -> if (isTrump) 200 else 150
            card.rank == Rank.SMALL_JOKER -> if (isTrump) 190 else 140
            // 主牌中的级牌
            isTrump && card.rank == currentLevel -> {
                // 主花色级牌 > 其他花色级牌
                if (card.suit == mainSuit) 180 else 170
            }
            // 其他主牌
            isTrump -> 100 + rankOrder(card.rank, currentLevel)
            // 副牌
            else -> rankOrder(card.rank, currentLevel)
        }
    }

    /**
     * 副牌内的点数排序权重（A 最大，3 最小）
     */
    private fun rankOrder(rank: Rank, currentLevel: Rank): Int {
        return when (rank) {
            Rank.ACE -> 13
            Rank.KING -> 12
            Rank.QUEEN -> 11
            Rank.JACK -> 10
            Rank.TEN -> 9
            Rank.NINE -> 8
            Rank.EIGHT -> 7
            Rank.SEVEN -> 6
            Rank.SIX -> 5
            Rank.FIVE -> 4
            Rank.FOUR -> 3
            Rank.THREE -> 2
            Rank.TWO -> 1
            else -> 0
        }
    }

    /**
     * 标记一批牌的主牌属性
     */
    fun markMainTrump(cards: List<Card>, mainSuit: Suit?, currentLevel: Rank): List<Card> {
        return cards.map { card ->
            if (isMainTrump(card, mainSuit, currentLevel)) card.asMainTrump() else card
        }
    }

    /**
     * 获取牌的分值
     */
    fun pointValue(card: Card): Int = card.pointValue

    /**
     * 计算一组牌的总分值
     */
    fun totalPoints(cards: List<Card>): Int = cards.sumOf { it.pointValue }
}
