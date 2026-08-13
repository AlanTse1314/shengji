package com.shengji.app.enums

/**
 * 牌型枚举
 */
enum class ComboType {
    /** 单张 */
    SINGLE,

    /** 对子 */
    PAIR,

    /** 拖拉机（连续对子），如 3344, 556677 */
    TRACTOR,

    /** 甩牌（多张非连续对子的组合出牌） */
    SLINGSHOT,

    /** 非法出牌 */
    INVALID
}
