package com.shengji.app.enums

/**
 * 花色枚举
 */
enum class Suit(val symbol: String, val displayName: String) {
    SPADE("♠", "黑桃"),
    HEART("♥", "红心"),
    CLUB("♣", "梅花"),
    DIAMOND("♦", "方块"),
    JOKER("🃏", "王牌");

    companion object {
        fun fromSymbol(symbol: String): Suit =
            entries.first { it.symbol == symbol }
    }
}
