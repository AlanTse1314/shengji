package com.shengji.app.data

import com.shengji.app.enums.ComboType
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 轮次结算逻辑
 *
 * 判断一轮出牌的赢家，收集本轮得分，计算抠底倍数。
 * 所有方法均为纯函数，无副作用。
 */
object RoundResolver {

    /**
     * 轮次结算结果
     *
     * @property winnerIndex 赢家玩家索引
     * @property points 本轮总得分（所有 5、10、K 的分值之和）
     * @property collectedCards 本轮所有出的牌（收归赢家）
     */
    data class RoundResult(
        val winnerIndex: Int,
        val points: Int,
        val collectedCards: List<Card>
    )

    /**
     * 判断一轮出牌的赢家
     *
     * 比较规则：
     * 1. 主牌出牌 > 跟首出花色 > 其他花色（垫牌）
     * 2. 同级别内按最大牌的比较权重决定胜负
     *
     * @param plays 按出牌顺序排列的 (玩家索引, 出的牌列表)
     * @param leadCombo 首出牌型，决定比较基准
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 轮次结果，包含赢家、得分和收集的牌
     */
    fun resolveRound(
        plays: List<Pair<Int, List<Card>>>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank
    ): RoundResult {
        require(plays.isNotEmpty()) { "本轮出牌不能为空" }

        var winnerIndex = plays[0].first
        var maxWeight = Int.MIN_VALUE

        for ((playerIndex, cards) in plays) {
            val weight = evaluatePlay(cards, leadCombo, mainSuit, currentLevel)
            if (weight > maxWeight) {
                maxWeight = weight
                winnerIndex = playerIndex
            }
        }

        val allCards = plays.flatMap { it.second }
        val points = CardEvaluator.totalPoints(allCards)

        return RoundResult(
            winnerIndex = winnerIndex,
            points = points,
            collectedCards = allCards
        )
    }

    /**
     * 评估一次出牌的比较权重
     *
     * 权重 = 基础权重 + 最大牌比较权重：
     * - 主牌出牌：基础 10000
     * - 跟首出花色：基础 5000
     * - 垫其他花色：基础 0
     *
     * @param cards 出的牌
     * @param leadCombo 首出牌型
     * @param mainSuit 当前主花色
     * @param currentLevel 当前级牌
     * @return 比较权重值，越大越强
     */
    private fun evaluatePlay(
        cards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        val leadSuit = leadCombo.suit

        // 检查是否全部为主牌（毙牌）
        val isTrumpPlay = cards.all {
            CardEvaluator.isMainTrump(it, mainSuit, currentLevel)
        }

        // 检查是否跟了首出花色
        val isLeadSuitPlay = cards.all {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadSuit
        }

        // 基础权重：主牌 > 首出花色 > 杂牌
        val baseWeight = when {
            isTrumpPlay -> 10000
            isLeadSuitPlay -> 5000
            else -> 0
        }

        // 取最大牌的比较权重
        val maxCardWeight = cards.maxOf {
            CardEvaluator.comparisonWeight(it, mainSuit, currentLevel)
        }

        return baseWeight + maxCardWeight
    }

    /**
     * 计算抠底倍数
     *
     * 底牌分值根据最后一轮领出的最大牌型翻倍：
     * - 单张抠底：底牌分 × 2
     * - 对子抠底：底牌分 × 4
     * - 拖拉机（两对）抠底：底牌分 × 16
     * - 更长拖拉机每多一对翻一倍（三对 × 32，四对 × 64 ...）
     * - 甩牌按单张计算
     *
     * @param lastCombo 最后一轮领出的牌型
     * @param comboSize 牌张数（拖拉机用于计算对数）
     * @return 倍数
     */
    fun digBottomMultiplier(lastCombo: ComboType, comboSize: Int = 1): Int {
        return when (lastCombo) {
            ComboType.SINGLE -> 2
            ComboType.PAIR -> 4
            ComboType.TRACTOR -> {
                val pairCount = comboSize / 2
                if (pairCount >= 2) {
                    16 * (1 shl (pairCount - 2)) // 16, 32, 64, 128...
                } else {
                    4 // 单对按对子计算
                }
            }
            ComboType.SLINGSHOT -> 2 // 甩牌按单张计算
            else -> 2
        }
    }
}
