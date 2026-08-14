package com.shengji.app.enums

/**
 * 玩家类型枚举
 *
 * 区分人类玩家和 AI 玩家，用于出牌逻辑分支。
 * AI 玩家由 [AiPlayer] 自动决策出牌，人类玩家需手动操作。
 */
enum class PlayerType {
    /** 人类玩家：通过 UI 交互选牌、出牌 */
    HUMAN,

    /** AI 玩家：由算法自动选择出牌 */
    AI
}
