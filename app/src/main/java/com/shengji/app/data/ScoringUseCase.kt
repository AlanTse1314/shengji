package com.shengji.app.data

import com.shengji.app.enums.Rank

/**
 * 计分升级结算逻辑
 *
 * 两副牌总分 200 分（5×8 + 10×8 + K×8）。
 * 庄家方为守方，闲家为攻方，闲家得分决定升级幅度：
 *
 * | 闲家得分 | 结果 |
 * |---------|------|
 * | 0 分 | 庄家连升 3 级（大光） |
 * | 1~39 分 | 庄家连升 2 级（小光） |
 * | 40~79 分 | 庄家升 1 级 |
 * | 80~119 分 | 闲家上台，不升级 |
 * | 120~159 分 | 闲家升 1 级 |
 * | 160~199 分 | 闲家升 2 级 |
 * | 200 分 | 闲家连升 3 级（大光） |
 */
object ScoringUseCase {

    /**
     * 本局结算结果
     *
     * @property defenderScore 庄家队伍最终得分（200 - 闲家得分）
     * @property attackerScore 闲家队伍得分（已 clamp 到 0~200）
     * @property defenderLevelUp 庄家队伍升级数（0 表示不升级，负数无意义）
     * @property attackerLevelUp 闲家队伍升级数（0 表示不升级，负数无意义）
     * @property dealerChanged 是否换庄（闲家得分 ≥ 80 则换庄）
     * @property newLevel 新的级牌
     * @property newDealerTeam 新的庄家队伍编号
     * @property summary 结算说明文本，用于 UI 展示
     */
    data class SettlementResult(
        val defenderScore: Int,
        val attackerScore: Int,
        val defenderLevelUp: Int,
        val attackerLevelUp: Int,
        val dealerChanged: Boolean,
        val newLevel: Rank,
        val newDealerTeam: Int,
        val summary: String
    )

    /**
     * 计算本局结算
     *
     * 根据闲家总得分（出牌得分 + 抠底得分）决定升级和换庄。
     *
     * @param attackerTotalScore 闲家总得分
     * @param dealerTeam 庄家队伍编号（0 或 1）
     * @param currentLevel 当前级牌
     * @param dealerLevel 庄家当前级别（默认等于 currentLevel）
     * @return 结算结果
     */
    fun settle(
        attackerTotalScore: Int,
        dealerTeam: Int,
        currentLevel: Rank,
        dealerLevel: Rank = currentLevel
    ): SettlementResult {
        val clampedScore = attackerTotalScore.coerceIn(0, 200)
        val defenderScore = 200 - clampedScore

        val (defenderLevelUp, attackerLevelUp) = calculateLevelUp(clampedScore)

        val dealerChanged = clampedScore >= 80

        val newDealerTeam = if (dealerChanged) 1 - dealerTeam else dealerTeam

        val newLevel = if (dealerChanged) {
            advanceLevel(currentLevel, attackerLevelUp)
        } else {
            advanceLevel(currentLevel, defenderLevelUp)
        }

        val summary = buildString {
            append("本局结算：")
            append("庄家方 $defenderScore 分，闲家方 $clampedScore 分。")
            if (dealerChanged) {
                append("闲家上台！")
                if (attackerLevelUp > 1) append("升 $attackerLevelUp 级。")
            } else {
                append("庄家守台成功。")
                if (defenderLevelUp > 1) append("升 $defenderLevelUp 级。")
            }
            append("新级牌：${newLevel.display}")
        }

        return SettlementResult(
            defenderScore = defenderScore,
            attackerScore = clampedScore,
            defenderLevelUp = defenderLevelUp,
            attackerLevelUp = attackerLevelUp,
            dealerChanged = dealerChanged,
            newLevel = newLevel,
            newDealerTeam = newDealerTeam,
            summary = summary
        )
    }

    /**
     * 根据闲家得分计算双方升级数
     *
     * @param attackerScore 闲家得分（0~200）
     * @return Pair(庄家升级数, 闲家升级数)，未上台一方的值为 0
     */
    fun calculateLevelUp(attackerScore: Int): Pair<Int, Int> {
        return when {
            attackerScore == 0 -> Pair(3, 0)           // 大光：庄家升 3 级
            attackerScore in 1..39 -> Pair(2, 0)       // 小光：庄家升 2 级
            attackerScore in 40..79 -> Pair(1, 0)      // 庄家升 1 级
            attackerScore in 80..119 -> Pair(0, 0)     // 换庄但不升级
            attackerScore in 120..159 -> Pair(0, 1)    // 闲家升 1 级
            attackerScore in 160..199 -> Pair(0, 2)    // 闲家升 2 级
            attackerScore >= 200 -> Pair(0, 3)         // 大光：闲家升 3 级
            else -> Pair(0, 0)
        }
    }

    /**
     * 推进级牌
     *
     * 级牌顺序：2→3→4→5→6→7→8→9→10→J→Q→K→A，到 A 后回到 2 循环。
     *
     * @param currentLevel 当前级牌
     * @param steps 推进步数
     * @return 新的级牌
     */
    fun advanceLevel(currentLevel: Rank, steps: Int): Rank {
        val levelOrder = listOf(
            Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE,
            Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE,
            Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE
        )

        val currentIndex = levelOrder.indexOf(currentLevel)
        if (currentIndex == -1) return currentLevel

        val newIndex = (currentIndex + steps) % levelOrder.size
        return levelOrder[newIndex]
    }
}
