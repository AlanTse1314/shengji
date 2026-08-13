package com.shengji.app.data

import com.shengji.app.enums.PlayerType

/**
 * 发牌逻辑
 *
 * 将 108 张牌分发给 4 位玩家，剩余作为底牌。
 */
object DealUseCase {

    data class DealResult(
        val players: List<Player>,
        val bottomCards: List<Card>
    )

    /**
     * 发牌
     *
     * @param deck 已洗好的 108 张牌
     * @param playerIds 玩家 ID 列表（4 个）
     * @param playerNames 玩家显示名称列表
     * @param bottomCardCount 底牌数量（默认 8）
     * @return 发牌结果
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
                teamIndex = i % 2  // 0,2 一队；1,3 一队
            )
        }

        return DealResult(players = players, bottomCards = bottomCards)
    }

    /**
     * 带种子的发牌（用于测试和模拟）
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
