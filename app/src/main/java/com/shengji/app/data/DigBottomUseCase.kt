package com.shengji.app.data

import com.shengji.app.enums.ComboType

/**
 * 抠底逻辑
 *
 * 最后一轮出牌的赢家获得底牌，底牌中的分牌按牌型倍数计算。
 * 庄家赢最后一轮则不抠底（底牌分归庄家），闲家赢则抠底（底牌分 × 倍数归闲家）。
 */
object DigBottomUseCase {

    /**
     * 抠底结算结果
     *
     * @property diggerIndex 抠底的玩家索引（最后一轮赢家）
     * @property bottomPoints 底牌原始分值（所有 5、10、K 的分值之和）
     * @property multiplier 抠底倍数（由最后一轮牌型决定）
     * @property actualPoints 实际得分 = bottomPoints × multiplier
     * @property bottomCards 底牌列表（用于 UI 展示）
     * @property lastComboType 最后一轮的牌型（决定倍数）
     */
    data class DigBottomResult(
        val diggerIndex: Int,
        val bottomPoints: Int,
        val multiplier: Int,
        val actualPoints: Int,
        val bottomCards: List<Card>,
        val lastComboType: ComboType
    )

    /**
     * 执行抠底结算
     *
     * @param lastRoundWinner 最后一轮赢家索引
     * @param lastRoundCombo 最后一轮领出的牌型（决定倍数）
     * @param lastRoundCards 最后一轮出的牌（用于计算拖拉机长度）
     * @param bottomCards 底牌列表
     * @return 抠底结果
     */
    fun digBottom(
        lastRoundWinner: Int,
        lastRoundCombo: ComboType,
        lastRoundCards: List<Card> = emptyList(),
        bottomCards: List<Card>
    ): DigBottomResult {
        val bottomPoints = CardEvaluator.totalPoints(bottomCards)
        val comboSize = lastRoundCards.size
        val multiplier = RoundResolver.digBottomMultiplier(lastRoundCombo, comboSize)
        val actualPoints = bottomPoints * multiplier

        return DigBottomResult(
            diggerIndex = lastRoundWinner,
            bottomPoints = bottomPoints,
            multiplier = multiplier,
            actualPoints = actualPoints,
            bottomCards = bottomCards,
            lastComboType = lastRoundCombo
        )
    }
}
