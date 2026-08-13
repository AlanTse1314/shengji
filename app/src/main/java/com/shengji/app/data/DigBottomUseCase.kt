package com.shengji.app.data

import com.shengji.app.enums.ComboType

/**
 * 抠底逻辑
 *
 * 最后一轮出牌的赢家获得底牌，底牌中的分牌按倍数计算。
 */
object DigBottomUseCase {

    data class DigBottomResult(
        /** 抠底的玩家索引 */
        val diggerIndex: Int,
        /** 底牌原始分值 */
        val bottomPoints: Int,
        /** 倍数 */
        val multiplier: Int,
        /** 实际得分 */
        val actualPoints: Int,
        /** 底牌列表 */
        val bottomCards: List<Card>,
        /** 最后一轮的牌型 */
        val lastComboType: ComboType
    )

    /**
     * 执行抠底结算
     *
     * @param lastRoundWinner 最后一轮赢家索引
     * @param lastRoundCombo 最后一轮牌型
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
