package com.shengji.app.data

import com.shengji.app.enums.Rank

/**
 * 升级游戏规则配置
 *
 * @param bottomCardCount 底牌数量（通常 8 张）
 * @param maxLevel 最大级别（打到几算赢，通常为 A 或 2）
 * @param enableSlingshot 是否启用甩牌
 * @param deckCount 使用几副牌（通常 2 副）
 * @param playerCount 玩家人数（通常 4 人）
 * @param teamSize 每队人数（通常 2 人）
 * @param pointsPerLevel 升级所需分数（如 0/40/80 分别升 3/2/1 级）
 * @param startLevel 起始级牌
 * @param allowRevealOnDouble 对家亮主时是否允许反主
 */
data class RuleConfig(
    val bottomCardCount: Int = 8,
    val maxLevel: Rank = Rank.ACE,
    val enableSlingshot: Boolean = true,
    val deckCount: Int = 2,
    val playerCount: Int = 4,
    val teamSize: Int = 2,
    val pointsPerLevel: List<Int> = listOf(0, 40, 80),
    val startLevel: Rank = Rank.TWO,
    val allowRevealOnDouble: Boolean = true
) {
    /**
     * 总牌数（含大小王）
     */
    val totalCards: Int get() = deckCount * 54

    /**
     * 每人手牌数（发牌后）
     */
    val cardsPerPlayer: Int
        get() = (totalCards - bottomCardCount) / playerCount
}
