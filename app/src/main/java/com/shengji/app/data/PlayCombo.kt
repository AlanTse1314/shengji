package com.shengji.app.data

import com.shengji.app.enums.ComboType
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 一次出牌组合
 *
 * 封装玩家打出的一组牌及其牌型、有效花色。
 * 有效花色用于轮次比较，主牌统一视为 [mainSuit]。
 *
 * @property cards 出的牌列表
 * @property comboType 牌型（单张 / 对子 / 拖拉机 / 甩牌）
 * @property suit 出牌的有效花色，null 表示尚未确定
 */
data class PlayCombo(
    val cards: List<Card>,
    val comboType: ComboType,
    val suit: Suit? = null
) {
    /**
     * 出牌的分值总和
     *
     * 5 = 5 分，10 = 10 分，K = 10 分，其余为 0。
     */
    val pointValue: Int
        get() = cards.sumOf { it.pointValue }

    /**
     * 是否包含分牌（5、10、K）
     */
    val hasPoints: Boolean get() = pointValue > 0

    companion object {

        /**
         * 计算牌的有效花色
         *
         * 优先使用 [CardEvaluator.effectiveSuit]（考虑主牌/级牌上下文），
         * 若无上下文则回退到 [Card.effectiveSuit]（仅区分王牌）。
         *
         * @param card 目标牌
         * @param mainSuit 当前主花色，null 表示无主局或无上下文
         * @param currentLevel 当前级牌，null 表示无上下文
         * @return 有效花色
         */
        private fun resolveEffectiveSuit(card: Card, mainSuit: Suit?, currentLevel: Rank?): Suit {
            return if (mainSuit != null && currentLevel != null) {
                CardEvaluator.effectiveSuit(card, mainSuit, currentLevel)
            } else {
                card.effectiveSuit
            }
        }

        /**
         * 创建单张出牌组合
         *
         * @param card 打出的单张牌
         * @param mainSuit 当前主花色（可选，用于计算有效花色）
         * @param currentLevel 当前级牌（可选，用于计算有效花色）
         * @return 单张 [PlayCombo]
         */
        fun single(card: Card, mainSuit: Suit? = null, currentLevel: Rank? = null): PlayCombo {
            val suit = resolveEffectiveSuit(card, mainSuit, currentLevel)
            return PlayCombo(listOf(card), ComboType.SINGLE, suit)
        }

        /**
         * 创建对子出牌组合
         *
         * @param cards 两张同点数同有效花色的牌
         * @param mainSuit 当前主花色（可选，用于计算有效花色）
         * @param currentLevel 当前级牌（可选，用于计算有效花色）
         * @return 对子 [PlayCombo]
         */
        fun pair(cards: List<Card>, mainSuit: Suit? = null, currentLevel: Rank? = null): PlayCombo {
            val suit = resolveEffectiveSuit(cards.first(), mainSuit, currentLevel)
            return PlayCombo(cards, ComboType.PAIR, suit)
        }

        /**
         * 创建拖拉机出牌组合
         *
         * @param cards 连续对子（如 3344、JJQQKK）
         * @param mainSuit 当前主花色（可选，用于计算有效花色）
         * @param currentLevel 当前级牌（可选，用于计算有效花色）
         * @return 拖拉机 [PlayCombo]
         */
        fun tractor(cards: List<Card>, mainSuit: Suit? = null, currentLevel: Rank? = null): PlayCombo {
            val suit = resolveEffectiveSuit(cards.first(), mainSuit, currentLevel)
            return PlayCombo(cards, ComboType.TRACTOR, suit)
        }

        /**
         * 创建甩牌出牌组合
         *
         * @param cards 同花色多张牌（可混合单张、对子）
         * @param mainSuit 当前主花色（可选，用于计算有效花色）
         * @param currentLevel 当前级牌（可选，用于计算有效花色）
         * @return 甩牌 [PlayCombo]
         */
        fun slingshot(cards: List<Card>, mainSuit: Suit? = null, currentLevel: Rank? = null): PlayCombo {
            val suit = resolveEffectiveSuit(cards.first(), mainSuit, currentLevel)
            return PlayCombo(cards, ComboType.SLINGSHOT, suit)
        }
    }
}
