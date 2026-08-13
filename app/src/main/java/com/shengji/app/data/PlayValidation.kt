package com.shengji.app.data

import com.shengji.app.enums.ComboType
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 出牌校验逻辑
 *
 * 纯函数，所有方法无副作用。
 */
object PlayValidation {

    /**
     * 判断一组牌的牌型
     */
    fun identifyCombo(
        cards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ComboType {
        if (cards.isEmpty()) return ComboType.INVALID

        when (cards.size) {
            1 -> return ComboType.SINGLE
            2 -> {
                // 对子：同点数同花色
                if (cards[0].rank == cards[1].rank &&
                    cards[0].effectiveSuit == cards[1].effectiveSuit) {
                    return ComboType.PAIR
                }
                return ComboType.INVALID
            }
            else -> {
                // 尝试识别拖拉机（连续对子）
                if (isTractor(cards, mainSuit, currentLevel)) return ComboType.TRACTOR
                // 尝试识别甩牌（同花色多张）
                if (isSlingshot(cards, mainSuit, currentLevel)) return ComboType.SLINGSHOT
                return ComboType.INVALID
            }
        }
    }

    /**
     * 判断是否为拖拉机（连续对子）
     *
     * 要求：同花色、连续点数的对子组合
     * 如 3344, 556677, JJQQKK
     */
    private fun isTractor(cards: List<Card>, mainSuit: Suit?, currentLevel: Rank): Boolean {
        if (cards.size < 4 || cards.size % 2 != 0) return false

        // 所有牌必须同花色（或同为主牌）
        val suits = cards.map { CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) }.distinct()
        if (suits.size != 1) return false

        // 分成对子
        val pairs = cards.chunked(2)
        if (!pairs.all { it.size == 2 && it[0].rank == it[1].rank && it[0].suit == it[1].suit }) {
            return false
        }

        // 检查连续性
        val ranks = pairs.map { it[0].rank }.sortedBy { it.order }
        for (i in 0 until ranks.size - 1) {
            if (ranks[i + 1].order - ranks[i].order != 1) return false
        }

        // 不能包含大小王
        if (cards.any { it.isJoker }) return false

        return true
    }

    /**
     * 判断是否为甩牌
     *
     * 甩牌：同花色的多张牌（非标准对子/拖拉机）
     * 简化实现：同一有效花色的多张牌
     */
    private fun isSlingshot(cards: List<Card>, mainSuit: Suit?, currentLevel: Rank): Boolean {
        if (cards.size < 3) return false
        val suits = cards.map { CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) }.distinct()
        return suits.size == 1
    }

    /**
     * 首出合法性校验
     *
     * 首出可以出任意合法牌型。
     */
    fun validateLead(
        cards: List<Card>,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ValidationResult {
        if (cards.isEmpty()) return ValidationResult(false, "请先选择要出的牌")

        // 检查重复牌
        val deckIndices = cards.map { it.deckIndex }
        if (deckIndices.size != deckIndices.toSet().size) {
            return ValidationResult(false, "选择了重复的牌，请重新选择")
        }

        // 检查牌是否在手中
        val handSet = handCards.map { it.deckIndex }.toSet()
        if (cards.any { it.deckIndex !in handSet }) {
            return ValidationResult(false, "选择的牌不在手牌中")
        }

        val comboType = identifyCombo(cards, mainSuit, currentLevel)
        if (comboType == ComboType.INVALID) {
            return ValidationResult(false, "不合法的牌型：请检查是否选了不同花色或数量不对")
        }

        return ValidationResult(true, "合法")
    }

    /**
     * 跟牌合法性校验
     *
     * 规则：
     * 1. 必须跟出与首出相同花色的牌（如果手中有该花色）
     * 2. 如果没有首出花色的牌，可以出主牌（杀）或其他花色
     * 3. 出的牌必须是合法牌型
     * 4. 跟牌数量必须与首出相同（对子跟对子，拖拉机跟拖拉机）
     */
    fun validateFollow(
        cards: List<Card>,
        handCards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ValidationResult {
        if (cards.isEmpty()) return ValidationResult(false, "请先选择要跟的牌")

        // 检查重复牌
        val deckIndices = cards.map { it.deckIndex }
        if (deckIndices.size != deckIndices.toSet().size) {
            return ValidationResult(false, "选择了重复的牌，请重新选择")
        }

        val handSet = handCards.map { it.deckIndex }.toSet()
        if (cards.any { it.deckIndex !in handSet }) {
            return ValidationResult(false, "选择的牌不在手牌中")
        }

        val leadSuit = leadCombo.suit
        val leadSuitName = when (leadSuit) {
            Suit.SPADE -> "黑桃"
            Suit.HEART -> "红心"
            Suit.CLUB -> "梅花"
            Suit.DIAMOND -> "方块"
            Suit.JOKER -> "王牌"
            null -> "未知花色"
        }

        // 检查手中是否有首出花色的牌
        val hasLeadSuit = handCards.any {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadSuit
        }

        if (hasLeadSuit) {
            // 必须跟首出花色
            val playedLeadSuit = cards.all {
                CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadSuit
            }
            if (!playedLeadSuit) {
                return ValidationResult(false, "你有${leadSuitName}，必须跟出${leadSuitName}")
            }

            // 跟牌数量必须与首出相同
            if (cards.size != leadCombo.cards.size) {
                return ValidationResult(false, "首出${leadCombo.cards.size}张，你也必须跟${leadCombo.cards.size}张")
            }

            // 牌型必须合法
            val playedType = identifyCombo(cards, mainSuit, currentLevel)
            if (playedType == ComboType.INVALID) {
                return ValidationResult(false, "不合法的牌型：请检查是否选了不同花色或数量不对")
            }
            
            // 对子必须跟对子
            if (leadCombo.comboType == ComboType.PAIR && playedType != ComboType.PAIR) {
                return ValidationResult(false, "首出是对子，你也必须跟对子")
            }
        } else {
            // 没有首出花色，允许出任意合法牌型
            val playedType = identifyCombo(cards, mainSuit, currentLevel)
            if (playedType == ComboType.INVALID) {
                return ValidationResult(false, "不合法的牌型：请检查是否选了不同花色或数量不对")
            }
        }

        return ValidationResult(true, "合法")
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
