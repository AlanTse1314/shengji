package com.shengji.app.enums

/**
 * 游戏阶段枚举
 *
 * 升级游戏的完整生命周期，从开局空闲到整局结束。
 * 各阶段按顺序流转：IDLE → DEAL → CALL_MAIN → PLAYING → DIG_BOTTOM → ROUND_SETTLE → GAME_OVER
 */
enum class GamePhase {
    /** 空闲/等待开始：尚未开始或已结束，等待玩家点击开始 */
    IDLE,

    /** 发牌阶段：系统自动给 4 位玩家发牌，每人 25 张，剩余 8 张底牌 */
    DEAL,

    /** 叫主（亮主）阶段：玩家可通过级牌对子亮主，他人可反主 */
    CALL_MAIN,

    /** 出牌阶段：庄家领出，四家轮流出牌、跟牌，直到手牌打完 */
    PLAYING,

    /** 抠底阶段：最后一轮结束后，根据赢家身份决定底牌分数归属 */
    DIG_BOTTOM,

    /** 本局结算：统计闲家得分，决定升级数和换庄 */
    ROUND_SETTLE,

    /** 整局游戏结束：某方打过 A，游戏结束 */
    GAME_OVER
}
