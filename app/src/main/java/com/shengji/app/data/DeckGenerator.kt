package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 牌组生成工具类
 *
 * 负责生成、洗牌等牌堆操作。
 * 两副牌共 108 张（54 × 2），每张牌通过 [Card.deckIndex] 唯一标识（0~107）。
 */
object DeckGenerator {

    /**
     * 生成一副完整的 54 张牌
     *
     * 包含 4 种花色各 13 张（2~A）+ 大小王各 1 张。
     * 此时尚未分配 [Card.deckIndex]，需由 [generateDoubleDeck] 统一编号。
     *
     * @return 54 张牌的列表
     */
    fun generateSingleDeck(): List<Card> {
        val cards = mutableListOf<Card>()
        for (suit in listOf(Suit.SPADE, Suit.HEART, Suit.CLUB, Suit.DIAMOND)) {
            for (rank in Rank.entries.filter { it.order in 1..13 }) {
                cards.add(Card(suit = suit, rank = rank))
            }
        }
        cards.add(Card(suit = Suit.JOKER, rank = Rank.SMALL_JOKER))
        cards.add(Card(suit = Suit.JOKER, rank = Rank.BIG_JOKER))
        return cards
    }

    /**
     * 生成两副牌（108 张）
     *
     * 两副牌各自独立编号：第一副 0~53，第二副 54~107。
     * 相同点数花色的两张牌 deckIndex 不同，可区分"哪一张"。
     *
     * @return 108 张牌的列表
     */
    fun generateDoubleDeck(): List<Card> {
        val deck1 = generateSingleDeck()
        val deck2 = generateSingleDeck()
        return deck1.mapIndexed { i, card -> card.copy(deckIndex = i) } +
                deck2.mapIndexed { i, card -> card.copy(deckIndex = i + 54) }
    }

    /**
     * Fisher-Yates 洗牌算法
     *
     * 原地打乱后返回新列表，不修改原始牌堆。
     *
     * @param cards 待洗牌的牌列表
     * @param seed 随机种子，null 表示随机；传入固定种子可用于测试复现
     * @return 洗牌后的新列表
     */
    fun shuffle(cards: List<Card>, seed: Long? = null): List<Card> {
        val mutable = cards.toMutableList()
        val random = seed?.let { java.util.Random(it) } ?: java.util.Random()
        for (i in mutable.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = mutable[i]
            mutable[i] = mutable[j]
            mutable[j] = temp
        }
        return mutable
    }
}
