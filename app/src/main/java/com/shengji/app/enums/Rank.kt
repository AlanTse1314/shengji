package com.shengji.app.enums

/**
 * 点数枚举
 *
 * 定义扑克牌的所有点数，从小到大排列。
 * 普通牌 2~A 共 13 级，大小王为特殊点数，权重最高。
 *
 * @property display 显示文本（如 "2"、"A"、"大王"）
 * @property order 排序权重，值越大越大（2=1，A=13，小王=14，大王=15）
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
