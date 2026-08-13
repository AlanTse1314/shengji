package com.shengji.app.enums

/**
 * 点数枚举（A最大，依次递减，2最小；大小王最大）
 */
enum class Rank(val display: String, val order: Int) {
    TWO("2", 1),
    THREE("3", 2),
    FOUR("4", 3),
    FIVE("5", 4),
    SIX("6", 5),
    SEVEN("7", 6),
    EIGHT("8", 7),
    NINE("9", 8),
    TEN("10", 9),
    JACK("J", 10),
    QUEEN("Q", 11),
    KING("K", 12),
    ACE("A", 13),
    SMALL_JOKER("小王", 14),
    BIG_JOKER("大王", 15);
}
