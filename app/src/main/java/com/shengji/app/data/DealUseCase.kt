package com.shengji.app.data

import com.shengji.app.enums.PlayerType

/**
 * 发牌逻辑
 *
 * 将洗好的 108 张牌分发给 4 位玩家（每人 25 张），剩余 8 张作为底牌。
 * 玩家 0 固定为人类，其余为 AI。
 */
object DealUseCase {

    /**
     * 发牌结果
     *
     * @property players 四位玩家（索引 0 为人类，1/2/3 为 AI）
     * @property bottomCards 底牌（8 张），归属庄家
     */
    data class DealResult(
        val players: List<Player>,
        val bottomCards: List<Card>
    )

    /**
     * 执行发牌
     *
     * 从牌堆中取前 [bottomCardCount] 张作为底牌，其余按顺序均分给 4 位玩家。
     * 队伍分配：索引 0/2 为一队，1/3 为一队（对家为队友）。
     *
     * @param deck 已洗好的 108 张牌
     * @param playerIds 玩家 ID 列表（4 个）
     * @param playerNames 玩家显示名称列表（4 个）
     * @param bottomCardCount 底牌数量，默认 8
     * @return 发牌结果，包含玩家列表和底牌
     * @throws IllegalArgumentException 牌数不为 108 或玩家数不为 4
     */
    fun deal(
        deck: List<Card>,
        playerIds: List<String>,
        playerNames: List<String>,
        bottomCardCount: Int = 8
    ): DealResult {
        require(deck.size == 108) { "牌组必须为 108 张" }
        require(playerIds.size == 4) { "必须为 4 位玩家" }

        val shuffled = DeckGenerator.shuffle(deck)
        val bottomCards = shuffled.take(bottomCardCount)
        val handCards = shuffled.drop(bottomCardCount)

        val players = (0 until 4).map { i ->
            val start = i * 25
            val end = start + 25
            Player(
                playerId = playerIds[i],
                displayName = playerNames[i],
                playerType = if (i == 0) PlayerType.HUMAN else PlayerType.AI,
                handCards = handCards.subList(start, end),
                teamIndex = i % 2 // 0,2 一队；1,3 一队
            )
        }

        return DealResult(players = players, bottomCards = bottomCards)
    }

    /**
     * 带种子的发牌（用于测试和模拟）
     *
     * 使用固定随机种子，确保每次调用发出相同的牌局。
     *
     * @param seed 随机种子
     * @param playerIds 玩家 ID 列表，默认 P1~P4
     * @param playerNames 玩家名称列表，默认 "你/右家/对家/左家"
     * @param bottomCardCount 底牌数量，默认 8
     * @return 发牌结果
     */
    fun dealWithSeed(
        seed: Long,
        playerIds: List<String> = listOf("P1", "P2", "P3", "P4"),
        playerNames: List<String> = listOf("你", "右家", "对家", "左家"),
        bottomCardCount: Int = 8
    ): DealResult {
        val deck = DeckGenerator.generateDoubleDeck()
        val shuffled = DeckGenerator.shuffle(deck, seed)
        return deal(shuffled, playerIds, playerNames, bottomCardCount)
    }
}
