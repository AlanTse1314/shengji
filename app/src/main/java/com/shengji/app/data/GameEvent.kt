package com.shengji.app.data

/**
 * 游戏事件（UI 操作 → ViewModel 消费）
 *
 * 密封类，所有事件类型在此定义。
 * UI 层通过发送事件与 ViewModel 交互，ViewModel 根据事件类型执行对应逻辑。
 */
sealed class GameEvent {
    /** 发牌：触发新一轮发牌流程 */
    data object Deal : GameEvent()

    /**
     * 发牌时亮牌
     *
     * 摸牌过程中亮出单张级牌确定主花色（可被后续对子反主）。
     *
     * @property card 亮出的级牌
     */
    data class ShowCard(val card: Card) : GameEvent()

    /**
     * 叫主/反主
     *
     * 选择一对牌（级牌对或大小王对）叫主或反主。
     *
     * @property cards 用于叫主的一对牌
     */
    data class CallMain(val cards: List<Card>) : GameEvent()

    /** 不叫主（跳过叫主阶段） */
    data object PassCallMain : GameEvent()

    /**
     * 首出牌
     *
     * 轮到自己首出时，选择要出的牌。
     *
     * @property cards 选择要出的牌
     */
    data class LeadPlay(val cards: List<Card>) : GameEvent()

    /**
     * 跟牌
     *
     * 其他玩家首出后，选择要跟的牌。
     *
     * @property cards 选择要跟的牌
     */
    data class FollowPlay(val cards: List<Card>) : GameEvent()

    /** 确认抠底：查看底牌后确认 */
    data object ConfirmDigBottom : GameEvent()

    /** 确认结算：查看本局结果后进入下一局 */
    data object ConfirmSettle : GameEvent()

    /** 重新开局：从头开始新一局 */
    data object Restart : GameEvent()

    /** 切换人类玩家的 AI 托管状态 */
    data object ToggleAiControl : GameEvent()

    /**
     * 选择底牌或手牌用于配对交换
     *
     * 庄家拿到底牌后，需要选 8 张扣回。通过此事件选择要交换的牌。
     *
     * @property cardIndex 牌的索引
     * @property isBottomCard true 表示选择的是底牌，false 表示选择的是手牌
     */
    data class SelectForExchange(val cardIndex: Int, val isBottomCard: Boolean) : GameEvent()

    /** 确认换底牌：完成底牌交换 */
    data object ConfirmBottomExchange : GameEvent()

    /** 取消换底牌：撤销所有交换操作 */
    data object CancelBottomExchange : GameEvent()
}
