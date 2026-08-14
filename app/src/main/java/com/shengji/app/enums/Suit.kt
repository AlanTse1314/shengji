package com.shengji.app.enums

/**
 * 花色枚举
 *
 * 定义扑克牌的四种花色及王牌特殊花色。
 *
 * @property symbol 花色符号（如 "♠"、"♥"），用于 UI 显示
 * @property displayName 中文名称（如 "黑桃"、"红心"），用于提示文本
 */
enum class Suit(val symbol: String, val displayName: String) {
    SPADE("♠", "黑桃"),
    HEART("♥", "红心"),
    CLUB("♣", "梅花"),
    DIAMOND("♦", "方块"),
    JOKER("🃏", "王牌");

    companion object {
        /**
         * 根据符号字符串查找花色
         *
         * @param symbol 花色符号（如 "♠"）
         * @return 匹配的 [Suit] 枚举值
         * @throws NoSuchElementException 无匹配时抛出
         */
        fun fromSymbol(symbol: String): Suit =
            entries.first { it.symbol == symbol }
    }
}
