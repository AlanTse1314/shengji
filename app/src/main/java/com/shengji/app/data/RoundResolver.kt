package com.shengji.app.data

import com.shengji.app.enums.ComboType
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 轮次结算逻辑
 *
 * 判断一轮出牌的赢家，收集本轮得分。
 */
object RoundResolver {

    data class RoundResult(
        /** 赢家玩家索引 */
        val winnerIndex: Int,
        /** 本轮得分 */
        val points: Int,
        /** 本轮所有出的牌（收集起来给赢家） */
        val collectedCards: List<Card>
    )

    /**
     * 判断一轮出牌的赢家
     *
     * @param plays 按出牌顺序排列的 (玩家索引, 出的牌列表)
     * @param leadCombo 首出牌型
     * @param mainSuit 主花色
     * @param currentLevel 当前级牌
     * @return 轮次结果
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
     * 主牌 > 首出花色 > 其他花色
     * 同花色内按最大牌比较
     */
    private fun evaluatePlay(
        cards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Int {
        val leadSuit = leadCombo.suit

        // 检查是否为主牌出牌
        val isTrumpPlay = cards.all {
            CardEvaluator.isMainTrump(it, mainSuit, currentLevel)
        }

        // 检查是否跟了首出花色
        val isLeadSuitPlay = cards.all {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadSuit
        }

        // 基础权重：主牌出牌 > 首出花色 > 杂牌
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
     * 底牌分值根据最后一轮出牌牌型翻倍（README 标准）：
     * - 单张抠底：底牌分 × 2
     * - 对子抠底：底牌分 × 4
     * - 拖拉机（两对）抠底：底牌分 × 16
     * - 更长拖拉机倍数继续翻倍（每多一对翻一倍）
     * - 甩牌按单张计算
     */
    fun digBottomMultiplier(lastCombo: ComboType, comboSize: Int = 1): Int {
        return when (lastCombo) {
            ComboType.SINGLE -> 2
            ComboType.PAIR -> 4
            ComboType.TRACTOR -> {
                // 拖拉机基础倍数是16（两对），每多一对翻一倍
                val pairCount = comboSize / 2  // 计算有多少对
                if (pairCount >= 2) {
                    16 * (1 shl (pairCount - 2))  // 16, 32, 64, 128...
                } else {
                    4  // 如果只有一对，按对子计算
                }
            }
            ComboType.SLINGSHOT -> 2  // 甩牌按单张计算
            else -> 2
        }
    }
}
