package com.shengji.app.data

import com.shengji.app.enums.ComboType
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 出牌校验逻辑
 *
 * 提供牌型识别、首出校验、跟牌校验三大核心功能。
 * 所有方法均为纯函数，无副作用，不修改任何入参状态。
 */
object PlayValidation {

    // ==================== 牌型识别 ====================

    /**
     * 识别一组牌的牌型
     *
     * 根据牌的数量和特征判断属于单张、对子、拖拉机、甩牌中的哪一种。
     * 两张牌需同点数且同有效花色才算对子；四张及以上先尝试拖拉机，再尝试甩牌。
     *
     * @param cards 待识别的牌列表
     * @param mainSuit 当前主花色，null 表示无主局
     * @param currentLevel 当前级牌（如打 5 则为 Rank.FIVE）
     * @return 识别出的牌型，无法识别则返回 [ComboType.INVALID]
     */
    fun identifyCombo(
        cards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ComboType {
        if (cards.isEmpty()) return ComboType.INVALID

        return when (cards.size) {
            1 -> ComboType.SINGLE
            2 -> identifyPair(cards, mainSuit, currentLevel)
            else -> identifyMulti(cards, mainSuit, currentLevel)
        }
    }

    /**
     * 识别两张牌是否为对子
     *
     * 对子条件：同点数 + 同有效花色。有效花色由 [CardEvaluator.effectiveSuit] 计算，
     * 主牌（含级牌）统一视为主花色，因此不同花色的级牌对也能组成对子。
     *
     * @return 合法则返回 [ComboType.PAIR]，否则 [ComboType.INVALID]
     */
    private fun identifyPair(
        cards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ComboType {
        val sameRank = cards[0].rank == cards[1].rank
        val sameEffSuit = CardEvaluator.effectiveSuit(cards[0], mainSuit, currentLevel) ==
                CardEvaluator.effectiveSuit(cards[1], mainSuit, currentLevel)
        return if (sameRank && sameEffSuit) ComboType.PAIR else ComboType.INVALID
    }

    /**
     * 识别三张及以上牌的牌型（拖拉机或甩牌）
     *
     * 优先判断拖拉机（连续对子），其次判断甩牌（同花色多张）。
     *
     * @return [ComboType.TRACTOR]、[ComboType.SLINGSHOT] 或 [ComboType.INVALID]
     */
    private fun identifyMulti(
        cards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ComboType {
        if (isTractor(cards, mainSuit, currentLevel)) return ComboType.TRACTOR
        if (isSlingshot(cards, mainSuit, currentLevel)) return ComboType.SLINGSHOT
        return ComboType.INVALID
    }

    // ==================== 拖拉机判断 ====================

    /**
     * 判断是否为拖拉机（连对）
     *
     * 拖拉机条件：
     * 1. 至少 4 张牌，且为偶数
     * 2. 所有牌有效花色相同
     * 3. 可分成若干对子（同点数 + 同有效花色）
     * 4. 各对子点数连续（如 3344、JJQQKK）
     * 5. 不含大小王
     *
     * @param cards 待判断的牌列表
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 是拖拉机返回 true
     */
    private fun isTractor(cards: List<Card>, mainSuit: Suit?, currentLevel: Rank): Boolean {
        if (cards.size < 4 || cards.size % 2 != 0) return false

        // 所有牌必须同有效花色
        val suits = cards.map { CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) }.distinct()
        if (suits.size != 1) return false

        // 按顺序两两配对，检查每对是否为合法对子
        val pairs = cards.chunked(2)
        val allValidPairs = pairs.all { pair ->
            pair.size == 2 &&
                    pair[0].rank == pair[1].rank &&
                    CardEvaluator.effectiveSuit(pair[0], mainSuit, currentLevel) ==
                    CardEvaluator.effectiveSuit(pair[1], mainSuit, currentLevel)
        }
        if (!allValidPairs) return false

        // 各对子点数必须连续
        val ranks = pairs.map { it[0].rank }.sortedBy { it.order }
        val isConsecutive = ranks.zipWithNext().all { (a, b) -> b.order - a.order == 1 }
        if (!isConsecutive) return false

        // 拖拉机不含大小王
        if (cards.any { it.isJoker }) return false

        return true
    }

    // ==================== 甩牌判断 ====================

    /**
     * 判断是否为甩牌
     *
     * 甩牌条件：3 张及以上，且所有牌的有效花色相同。
     * 甩牌是同花色多张牌的混合出牌（可包含单张、对子的组合）。
     *
     * @param cards 待判断的牌列表
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 是甩牌返回 true
     */
    private fun isSlingshot(cards: List<Card>, mainSuit: Suit?, currentLevel: Rank): Boolean {
        if (cards.size < 3) return false
        val suits = cards.map { CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) }.distinct()
        return suits.size == 1
    }

    // ==================== 首出校验 ====================

    /**
     * 首出合法性校验
     *
     * 首出玩家可以出任意合法牌型。校验流程：
     * 1. 非空检查
     * 2. 重复牌检查（同一张牌不能出两次）
     * 3. 手牌归属检查（出的牌必须在自己手中）
     * 4. 牌型合法性检查
     *
     * @param cards 玩家选择要出的牌
     * @param handCards 玩家当前手牌
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 校验结果，包含是否合法及具体提示信息
     */
    fun validateLead(
        cards: List<Card>,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ValidationResult {
        val basicCheck = validateBasic(cards, handCards)
        if (basicCheck != null) return basicCheck

        val comboType = identifyCombo(cards, mainSuit, currentLevel)
        if (comboType == ComboType.INVALID) {
            return ValidationResult(false, diagnoseInvalidCombo(cards, mainSuit, currentLevel))
        }

        return ValidationResult(true, "合法")
    }

    // ==================== 跟牌校验 ====================

    /**
     * 跟牌合法性校验
     *
     * 跟牌规则：
     * 1. 非空、重复牌、手牌归属等基础检查
     * 2. 有首出花色时：必须跟出该花色，数量与首出一致，牌型匹配（对子跟对子、拖拉机跟拖拉机）
     * 3. 无首出花色时：可出任意花色，但数量须与首出一致
     *
     * @param cards 玩家选择要跟的牌
     * @param handCards 玩家当前手牌
     * @param leadCombo 本轮首出的牌组合
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 校验结果，包含是否合法及具体提示信息
     */
    fun validateFollow(
        cards: List<Card>,
        handCards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ValidationResult {
        val basicCheck = validateBasic(cards, handCards)
        if (basicCheck != null) return basicCheck

        val leadSuit = leadCombo.suit
        val hasLeadSuit = handCards.any {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadSuit
        }

        return if (hasLeadSuit) {
            validateFollowWithLeadSuit(cards, handCards, leadCombo, leadSuit, mainSuit, currentLevel)
        } else {
            validateFollowWithoutLeadSuit(cards, handCards, leadCombo, mainSuit, currentLevel)
        }
    }

    /**
     * 有首出花色时的跟牌校验
     *
     * 强制规则：
     * - 所有出牌必须为首出花色
     * - 数量与首出一致（手牌足够时）
     * - 对子必须跟对子，拖拉机必须跟拖拉机（手牌足够时）
     */
    private fun validateFollowWithLeadSuit(
        cards: List<Card>,
        handCards: List<Card>,
        leadCombo: PlayCombo,
        leadSuit: Suit?,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ValidationResult {
        val leadSuitName = suitName(leadSuit)

        // 必须跟首出花色
        val allMatchLeadSuit = cards.all {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadSuit
        }
        if (!allMatchLeadSuit) {
            return ValidationResult(false, "你有${leadSuitName}，必须跟出${leadSuitName}")
        }

        val requiredCount = leadCombo.cards.size
        val availableCount = handCards.count {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadSuit
        }

        // 数量必须一致
        if (availableCount >= requiredCount) {
            // 手牌足够，必须出相同数量
            if (cards.size != requiredCount) {
                return ValidationResult(false, "首出${requiredCount}张，你也必须跟${requiredCount}张")
            }
        } else {
            // 手牌不足，必须出完所有该花色的牌
            if (cards.size != availableCount) {
                return ValidationResult(false, "你只有${availableCount}张${leadSuitName}，必须全部出完")
            }
        }

        // 牌型必须合法
        val playedType = identifyCombo(cards, mainSuit, currentLevel)
        if (playedType == ComboType.INVALID) {
            return ValidationResult(false, diagnoseInvalidCombo(cards, mainSuit, currentLevel))
        }

        // 对子必须跟对子（手牌足够时）
        if (leadCombo.comboType == ComboType.PAIR && availableCount >= 2 && playedType != ComboType.PAIR) {
            return ValidationResult(false, "首出是对子，你也必须跟对子")
        }

        // 拖拉机必须跟拖拉机（手牌足够时）
        if (leadCombo.comboType == ComboType.TRACTOR) {
            val availablePairs = countPairsOfSuit(leadSuit, handCards, mainSuit, currentLevel)
            val requiredPairs = leadCombo.cards.size / 2
            if (availablePairs >= requiredPairs && playedType != ComboType.TRACTOR) {
                return ValidationResult(false, "首出是拖拉机（${requiredPairs}连对），你也必须跟拖拉机")
            }
        }

        return ValidationResult(true, "合法")
    }

    /**
     * 无首出花色时的跟牌校验（缺门垫牌或毙牌）
     *
     * 手中没有首出花色的牌时，可以出任意花色或主牌毙牌。
     * 唯一约束是数量须与首出一致（手牌足够时）。
     */
    private fun validateFollowWithoutLeadSuit(
        cards: List<Card>,
        handCards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank
    ): ValidationResult {
        val requiredCount = leadCombo.cards.size

        // 数量必须一致
        if (handCards.size >= requiredCount) {
            // 手牌足够，必须出相同数量
            if (cards.size != requiredCount) {
                return ValidationResult(false, "首出${requiredCount}张，你也必须跟${requiredCount}张")
            }
        } else {
            // 手牌不足，必须出完所有手牌
            if (cards.size != handCards.size) {
                return ValidationResult(false, "你只剩${handCards.size}张牌，必须全部出完")
            }
        }

        // 牌型必须合法
        val playedType = identifyCombo(cards, mainSuit, currentLevel)
        if (playedType == ComboType.INVALID) {
            return ValidationResult(false, diagnoseInvalidCombo(cards, mainSuit, currentLevel))
        }

        return ValidationResult(true, "合法")
    }

    // ==================== 公共校验 ====================

    /**
     * 基础校验（首出和跟牌共用）
     *
     * 检查：非空、无重复牌、牌在手中。
     *
     * @param cards 要出的牌
     * @param handCards 手牌
     * @return 不合法时返回对应的 [ValidationResult]，合法时返回 null
     */
    private fun validateBasic(cards: List<Card>, handCards: List<Card>): ValidationResult? {
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

        return null
    }

    // ==================== 诊断提示 ====================

    /**
     * 诊断牌型不合法的具体原因
     *
     * 按优先级逐项排查，返回最贴切的中文提示：
     * - 王牌搭配错误（大小王混搭）
     * - 混合花色
     * - 同花色但点数不连续（拖拉机要求）
     * - 点数连续但有不成对的牌
     * - 同点数但张数不对（3 张、奇数张等）
     *
     * @param cards 不合法的牌组合
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 具体的错误提示文本
     */
    private fun diagnoseInvalidCombo(
        cards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): String {
        // 两张王牌：检查是否同类型（可组对）或混搭（不可组对）
        if (cards.size == 2 && cards.all { it.isJoker }) {
            return if (cards[0].rank == cards[1].rank) {
                "两张${if (cards[0].isBigJoker) "大王" else "小王"}可以组成对子"
            } else {
                "大王和小王不能组成对子"
            }
        }

        // 不同有效花色：列出具体花色
        val effSuits = cards.map { CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) }.distinct()
        if (effSuits.size > 1) {
            val names = effSuits.joinToString("、") { suitName(it) }
            return "选了不同花色的牌（${names}），不能一起出"
        }

        // 同花色但多点数：检查是否尝试组拖拉机
        val distinctRanks = cards.map { it.rank }.distinct()
        if (distinctRanks.size > 1) {
            val sortedRanks = distinctRanks.sortedBy { it.order }
            val isConsecutive = sortedRanks.zipWithNext().all { (a, b) -> b.order - a.order == 1 }
            if (!isConsecutive) {
                return "同花色但点数不连续，不能组成拖拉机"
            }
            // 点数连续但有不成对的
            val rankCounts = cards.groupBy { it.rank }.mapValues { it.value.size }
            val missingRanks = rankCounts.filter { it.value < 2 }.keys
            if (missingRanks.isNotEmpty()) {
                val names = missingRanks.joinToString("、") { it.display }
                return "点数连续但${names}不成对，拖拉机要求每个点数都是对子"
            }
            return "不是合法的牌型"
        }

        // 同花色同点数但张数不对
        if (cards.size == 3) {
            return "3张同点数的牌不是合法牌型，可以出对子（2张）或拆开出单张"
        }
        if (cards.size % 2 != 0) {
            return "奇数张牌不是合法牌型，拖拉机要求偶数张"
        }

        return "不是合法的牌型"
    }

    // ==================== 工具方法 ====================

    /**
     * 获取花色的中文名称
     *
     * @param suit 花色枚举，null 返回"未知"
     * @return 如"黑桃"、"红心"、"王牌"等
     */
    private fun suitName(suit: Suit?): String = when (suit) {
        Suit.SPADE -> "黑桃"
        Suit.HEART -> "红心"
        Suit.CLUB -> "梅花"
        Suit.DIAMOND -> "方块"
        Suit.JOKER -> "王牌"
        null -> "未知"
    }

    /**
     * 统计手牌中某花色能组成多少个对子
     *
     * 按点数分组后，每个点数取 floor(张数 / 2) 求和。
     *
     * @param suit 目标有效花色
     * @param handCards 手牌
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 可组成的对子数量
     */
    private fun countPairsOfSuit(
        suit: Suit?,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        val suitedCards = handCards.filter {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
        }
        return suitedCards.groupBy { it.rank }.values.sumOf { it.size / 2 }
    }

    // ==================== 数据类 ====================

    /**
     * 校验结果
     *
     * @property isValid 是否合法
     * @property message 提示信息（合法时为"合法"，不合法时为具体原因）
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
