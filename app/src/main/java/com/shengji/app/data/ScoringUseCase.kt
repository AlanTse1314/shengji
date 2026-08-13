package com.shengji.app.data

import com.shengji.app.enums.Rank

/**
 * 计分升级结算逻辑
 *
 * 升级计分规则（README 标准）：
 * - 共 200 分（两副牌：5×8 + 10×8 + K×8 = 200）
 * - 庄家方（叫主方）为守方，闲家方为攻方
 * - 闲家得分决定升级幅度：
 *   - 0 分：庄家升 3 级
 *   - 1-39 分：庄家升 2 级
 *   - 40-79 分：庄家升 1 级
 *   - 80-119 分：庄家下台，闲家上台不升级
 *   - 120-159 分：闲家升 1 级
 *   - 160-199 分：闲家升 2 级
 *   - 200 分：闲家升 3 级
 */
object ScoringUseCase {

    data class SettlementResult(
        /** 庄家队伍最终得分（含抠底） */
        val defenderScore: Int,
        /** 闲家队伍得分 */
        val attackerScore: Int,
        /** 庄家队伍升级数（负数表示下台） */
        val defenderLevelUp: Int,
        /** 闲家队伍升级数（负数表示下台） */
        val attackerLevelUp: Int,
        /** 是否换庄 */
        val dealerChanged: Boolean,
        /** 新的级牌 */
        val newLevel: Rank,
        /** 新的庄家队伍 */
        val newDealerTeam: Int,
        /** 结算说明 */
        val summary: String
    )

    /**
     * 计算本局结算
     *
     * @param attackerTotalScore 闲家总得分（出牌得分 + 抠底得分）
     * @param dealerTeam 庄家队伍编号
     * @param currentLevel 当前级牌
     * @param dealerLevel 庄家当前级别（用于计算升级）
     * @return 结算结果
     */
    fun settle(
        attackerTotalScore: Int,
        dealerTeam: Int,
        currentLevel: Rank,
        dealerLevel: Rank = currentLevel
    ): SettlementResult {
        // 分数 clamp 到 0-200
        val clampedScore = attackerTotalScore.coerceIn(0, 200)
        val defenderScore = 200 - clampedScore

        val (defenderLevelUp, attackerLevelUp) = calculateLevelUp(clampedScore)

        val dealerChanged = clampedScore >= 80

        val newDealerTeam = if (dealerChanged) 1 - dealerTeam else dealerTeam

        // 计算新级别
        val newLevel = if (dealerChanged) {
            // 闲家上台，按闲家升级数计算
            advanceLevel(currentLevel, attackerLevelUp)
        } else {
            // 庄家继续，按庄家升级数计算
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
     * 根据闲家得分计算升级数
     *
     * @return Pair(庄家升级数, 闲家升级数)，负数表示下台（实际不升级）
     */
    fun calculateLevelUp(attackerScore: Int): Pair<Int, Int> {
        return when {
            attackerScore == 0 -> Pair(3, 0)           // 大光：庄家升3级
            attackerScore in 1..39 -> Pair(2, 0)       // 小光：庄家升2级
            attackerScore in 40..79 -> Pair(1, 0)      // 庄家升1级
            attackerScore in 80..119 -> Pair(0, 0)     // 换庄但不升级
            attackerScore in 120..159 -> Pair(0, 1)    // 闲家升1级
            attackerScore in 160..199 -> Pair(0, 2)    // 闲家升2级
            attackerScore >= 200 -> Pair(0, 3)         // 大光：闲家升3级
            else -> Pair(0, 0)
        }
    }

    /**
     * 推进级牌
     *
     * 级牌顺序：2→3→4→5→6→7→8→9→10→J→Q→K→A→2(完成一圈)
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
