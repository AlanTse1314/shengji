package com.shengji.app.enums

/**
 * 游戏阶段枚举
 */
enum class GamePhase {
    /** 空闲 / 等待开始 */
    IDLE,

    /** 发牌阶段 */
    DEAL,

    /** 叫主（亮主）阶段 */
    CALL_MAIN,

    /** 出牌阶段 */
    PLAYING,

    /** 抠底阶段 */
    DIG_BOTTOM,

    /** 本局结算 */
    ROUND_SETTLE,

    /** 整局游戏结束 */
    GAME_OVER
}
