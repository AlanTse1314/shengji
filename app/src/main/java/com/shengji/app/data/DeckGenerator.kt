package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 牌组生成工具类
 *
 * 生成 2 副标准扑克牌（共 108 张），含大小王。
 */
object DeckGenerator {

    /**
     * 生成一副完整的 54 张牌
     */
    fun generateSingleDeck(): List<Card> {
        val cards = mutableListOf<Card>()
        for (suit in listOf(Suit.SPADE, Suit.HEART, Suit.CLUB, Suit.DIAMOND)) {
            for (rank in Rank.entries.filter { it.order in 1..13 }) {
                cards.add(Card(suit = suit, rank = rank))
            }
        }
        // 大小王
        cards.add(Card(suit = Suit.JOKER, rank = Rank.SMALL_JOKER))
        cards.add(Card(suit = Suit.JOKER, rank = Rank.BIG_JOKER))
        return cards
    }

    /**
     * 生成两副牌（108 张），每张牌有唯一 deckIndex
     */
    fun generateDoubleDeck(): List<Card> {
        val deck1 = generateSingleDeck()
        val deck2 = generateSingleDeck()
        return deck1.mapIndexed { i, card -> card.copy(deckIndex = i) } +
               deck2.mapIndexed { i, card -> card.copy(deckIndex = i + 54) }
    }

    /**
     * Fisher-Yates 洗牌算法
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
