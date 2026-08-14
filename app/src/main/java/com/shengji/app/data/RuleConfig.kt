package com.shengji.app.data

import com.shengji.app.enums.Rank

/**
 * 升级游戏规则配置
 *
 * 定义游戏的核心规则参数，可在开局前自定义调整。
 *
 * @property bottomCardCount 底牌数量，通常 8 张
 * @property maxLevel 最大级别，打到此级即获胜（通常为 [Rank.ACE]）
 * @property enableSlingshot 是否启用甩牌规则
 * @property deckCount 使用几副牌，通常 2 副（共 108 张）
 * @property playerCount 玩家总人数，通常 4 人
 * @property teamSize 每队人数，通常 2 人（两人对家）
 * @property pointsPerLevel 闲家得分对应的升级数，格式为 [0分→升3级, 40分→升2级, 80分→升1级]
 * @property startLevel 起始级牌，新局从 [Rank.TWO] 开始
 * @property allowRevealOnDouble 是否允许对子反主
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
     *
     * 两副牌 = 2 × 54 = 108 张。
     */
    val totalCards: Int get() = deckCount * 54

    /**
     * 每人手牌数（发牌后）
     *
     * = (总牌数 - 底牌数) / 玩家数。
     * 两副牌 4 人 = (108 - 8) / 4 = 25 张。
     */
    val cardsPerPlayer: Int
        get() = (totalCards - bottomCardCount) / playerCount
}
