package com.shengji.app.data

/**
 * 游戏事件（用户操作 → ViewModel 消费）
 */
sealed class GameEvent {
    /** 发牌 */
    data object Deal : GameEvent()

    /** 发牌时亮牌（亮单张级牌确定主花色） */
    data class ShowCard(val card: Card) : GameEvent()

    /** 叫主/反主（选择一对牌） */
    data class CallMain(val cards: List<Card>) : GameEvent()

    /** 不叫主（跳过） */
    data object PassCallMain : GameEvent()

    /** 首出牌 */
    data class LeadPlay(val cards: List<Card>) : GameEvent()

    /** 跟牌 */
    data class FollowPlay(val cards: List<Card>) : GameEvent()

    /** 确认抠底 */
    data object ConfirmDigBottom : GameEvent()

    /** 确认结算，进入下一局 */
    data object ConfirmSettle : GameEvent()

    /** 重新开局 */
    data object Restart : GameEvent()

    /** 切换人类玩家 AI 托管 */
    data object ToggleAiControl : GameEvent()

    /** 选择底牌或手牌用于配对交换 */
    data class SelectForExchange(val cardIndex: Int, val isBottomCard: Boolean) : GameEvent()

    /** 确认换底牌 */
    data object ConfirmBottomExchange : GameEvent()

    /** 取消换底牌 */
    data object CancelBottomExchange : GameEvent()
}
