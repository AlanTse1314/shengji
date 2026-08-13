package com.shengji.app.data

import com.shengji.app.enums.ComboType
import com.shengji.app.enums.Suit

/**
 * 一次出牌组合
 *
 * @param cards 出的牌列表
 * @param comboType 牌型
 * @param suit 出牌的有效花色（用于轮次比较，主牌统一视为 mainSuit）
 */
data class PlayCombo(
    val cards: List<Card>,
    val comboType: ComboType,
    val suit: Suit? = null
) {
    /**
     * 出牌的分值总和
     */
    val pointValue: Int
        get() = cards.sumOf { it.pointValue }

    /**
     * 是否包含分牌
     */
    val hasPoints: Boolean get() = pointValue > 0

    companion object {
        /**
         * 创建单张（需传入 mainSuit 以正确计算有效花色）
         */
        fun single(card: Card, mainSuit: Suit? = null, currentLevel: com.shengji.app.enums.Rank? = null): PlayCombo {
            val suit = if (mainSuit != null && currentLevel != null)
                CardEvaluator.effectiveSuit(card, mainSuit, currentLevel)
            else card.effectiveSuit
            return PlayCombo(listOf(card), ComboType.SINGLE, suit)
        }

        /**
         * 创建对子
         */
        fun pair(cards: List<Card>, mainSuit: Suit? = null, currentLevel: com.shengji.app.enums.Rank? = null): PlayCombo {
            val suit = if (mainSuit != null && currentLevel != null)
                CardEvaluator.effectiveSuit(cards.first(), mainSuit, currentLevel)
            else cards.first().effectiveSuit
            return PlayCombo(cards, ComboType.PAIR, suit)
        }

        /**
         * 创建拖拉机
         */
        fun tractor(cards: List<Card>, mainSuit: Suit? = null, currentLevel: com.shengji.app.enums.Rank? = null): PlayCombo {
            val suit = if (mainSuit != null && currentLevel != null)
                CardEvaluator.effectiveSuit(cards.first(), mainSuit, currentLevel)
            else cards.first().effectiveSuit
            return PlayCombo(cards, ComboType.TRACTOR, suit)
        }

        /**
         * 创建甩牌
         */
        fun slingshot(cards: List<Card>, mainSuit: Suit? = null, currentLevel: com.shengji.app.enums.Rank? = null): PlayCombo {
            val suit = if (mainSuit != null && currentLevel != null)
                CardEvaluator.effectiveSuit(cards.first(), mainSuit, currentLevel)
            else cards.first().effectiveSuit
            return PlayCombo(cards, ComboType.SLINGSHOT, suit)
        }
    }
}
