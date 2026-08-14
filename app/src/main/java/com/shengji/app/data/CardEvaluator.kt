package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 牌面评估工具
 *
 * 提供主牌判断、有效花色计算、比较权重、分值计算等核心功能。
 * 所有方法均为纯函数，无副作用。
 */
object CardEvaluator {

    // ==================== 主牌判断 ====================

    /**
     * 判断一张牌是否为主牌
     *
     * 主牌范围：
     * 1. 大小王（永远是主牌）
     * 2. 主花色的所有牌
     * 3. 非主花色中的级牌（如打 5 时，♠5 也是主牌）
     *
     * @param card 待判断的牌
     * @param mainSuit 当前主花色，null 表示无主局（此时仅大小王和级牌为主牌）
     * @param currentLevel 当前级牌
     * @return 是主牌返回 true
     */
    fun isMainTrump(card: Card, mainSuit: Suit?, currentLevel: Rank): Boolean {
        if (card.isJoker) return true
        if (mainSuit == null) return false
        if (card.suit == mainSuit) return true
        return card.rank == currentLevel
    }

    // ==================== 有效花色 ====================

    /**
     * 获取牌的"有效花色"
     *
     * 主牌统一视为 [mainSuit]，用于跟牌时的花色匹配判断。
     * 例如：打 5 红桃主时，♠5 的有效花色为红桃（而非黑桃）。
     * 大小王的有效花色为主花色；无主局时为 [Suit.JOKER]。
     *
     * @param card 目标牌
     * @param mainSuit 当前主花色，null 表示无主局
     * @param currentLevel 当前级牌
     * @return 有效花色枚举值
     */
    fun effectiveSuit(card: Card, mainSuit: Suit?, currentLevel: Rank): Suit {
        if (card.isJoker) return mainSuit ?: Suit.JOKER
        if (mainSuit != null && isMainTrump(card, mainSuit, currentLevel)) return mainSuit
        return card.suit
    }

    // ==================== 比较权重 ====================

    /**
     * 计算牌的比较权重（越大越强）
     *
     * 权重分配（从高到低）：
     * - 200: 大王（主牌身份）
     * - 190: 小王（主牌身份）
     * - 180: 主花色级牌（如打 5 红桃主，♥5）
     * - 170: 其他花色级牌（如打 5 红桃主，♠5）
     * - 101~113: 其他主牌（按点数递增）
     * - 1~13: 副牌（按点数递增）
     *
     * @param card 目标牌
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 比较权重值
     */
    fun comparisonWeight(card: Card, mainSuit: Suit?, currentLevel: Rank): Int {
        val isTrump = isMainTrump(card, mainSuit, currentLevel)

        return when {
            // 大小王
            card.rank == Rank.BIG_JOKER -> if (isTrump) 200 else 150
            card.rank == Rank.SMALL_JOKER -> if (isTrump) 190 else 140
            // 主牌中的级牌：主花色级牌 > 其他花色级牌
            isTrump && card.rank == currentLevel -> {
                if (card.suit == mainSuit) 180 else 170
            }
            // 其他主牌：基础权重 100 + 点数序号
            isTrump -> 100 + rankOrder(card.rank, currentLevel)
            // 副牌：仅按点数排序
            else -> rankOrder(card.rank, currentLevel)
        }
    }

    /**
     * 副牌内的点数排序权重
     *
     * A 最大（13），2 最小（1），大小王返回 0（不参与副牌排序）。
     *
     * @param rank 点数枚举
     * @param currentLevel 当前级牌（此处未使用，保留接口一致性）
     * @return 排序权重值
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

    // ==================== 主牌标记 ====================

    /**
     * 批量标记牌的主牌属性
     *
     * 将 [Card.isMainTrump] 设为 true，用于 UI 展示主牌高亮等场景。
     * 返回新的牌列表，不修改原始列表。
     *
     * @param cards 待标记的牌列表
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 标记后的新牌列表
     */
    fun markMainTrump(cards: List<Card>, mainSuit: Suit?, currentLevel: Rank): List<Card> {
        return cards.map { card ->
            if (isMainTrump(card, mainSuit, currentLevel)) card.asMainTrump() else card
        }
    }

    // ==================== 分值计算 ====================

    /**
     * 获取单张牌的分值
     *
     * 5 = 5 分，10 = 10 分，K = 10 分，其余为 0。
     *
     * @param card 目标牌
     * @return 分值
     */
    fun pointValue(card: Card): Int = card.pointValue

    /**
     * 计算一组牌的总分值
     *
     * 用于统计一轮出牌中的得分，以及底牌分值计算。
     *
     * @param cards 牌列表
     * @return 总分值
     */
    fun totalPoints(cards: List<Card>): Int = cards.sumOf { it.pointValue }
}
