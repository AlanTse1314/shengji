package com.shengji.app.data

import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 叫主逻辑
 *
 * 升级中叫主规则（README 标准）：
 * - 单张级牌可以亮主（可被对子反掉）
 * - 一对级牌可以叫主（确定花色）
 * - 一对小王可以叫主（打无主）
 * - 一对大王可以叫主（打无主，优先级最高）
 * - 反主优先级：方块对子＜梅花对子＜红桃对子＜黑桃对子＜一对小王＜一对大王
 */
object CallMainUseCase {

    data class CallMainResult(
        val mainSuit: Suit?,
        val callerIndex: Int,
        val success: Boolean,
        val message: String
    )

    /**
     * 获取花色的反主优先级（越大越优先）
     */
    private fun getSuitPriority(suit: Suit?): Int {
        return when (suit) {
            Suit.DIAMOND -> 1
            Suit.CLUB -> 2
            Suit.HEART -> 3
            Suit.SPADE -> 4
            null -> 5 // 无主（小王对）
            else -> 0
        }
    }

    /**
     * 获取叫主牌的优先级（用于反主判断）
     */
    private fun getCallPriority(cards: List<Card>): Int {
        if (cards.size != 2) return 0
        val c1 = cards[0]
        return when {
            c1.isBigJoker -> 100        // 大王对最高
            c1.isJoker -> 90            // 小王对
            c1.rank == Rank.ACE -> 80   // A对
            c1.rank == Rank.KING -> 70  // K对
            c1.rank == Rank.QUEEN -> 60 // Q对
            c1.rank == Rank.JACK -> 50  // J对
            else -> getSuitPriority(c1.suit) + 10 // 级牌对，按花色优先级
        }
    }

    /**
     * 尝试叫主
     *
     * @param cards 玩家选择叫主的牌（必须是一对）
     * @param callerIndex 叫主玩家索引
     * @param currentLevel 当前级牌
     * @param currentMainSuit 当前已叫的主花色（null 表示还没人叫）
     * @param currentCallerIndex 当前叫主者索引（-1 表示没人叫）
     * @param currentCallCards 当前叫主的牌（用于反主优先级比较）
     * @return 叫主结果
     */
    fun tryCallMain(
        cards: List<Card>,
        callerIndex: Int,
        currentLevel: Rank,
        currentMainSuit: Suit?,
        currentCallerIndex: Int = -1,
        currentCallCards: List<Card>? = null
    ): CallMainResult {
        // 必须出两张
        if (cards.size != 2) {
            return CallMainResult(currentMainSuit, currentCallerIndex, false, "叫主必须出一对牌")
        }

        val (c1, c2) = cards

        // 必须是一对
        if (c1.rank != c2.rank || c1.suit != c2.suit) {
            return CallMainResult(currentMainSuit, currentCallerIndex, false, "叫主的两张牌必须是同点数同花色的一对")
        }

        val newMainSuit: Suit? = when {
            // 一对大王 → 无主（最高优先级）
            c1.isBigJoker && c2.isBigJoker -> null
            // 一对小王 → 无主
            c1.isJoker && c2.isJoker && !c1.isBigJoker -> null
            // 一对级牌 → 该花色为主
            c1.rank == currentLevel -> c1.suit
            else -> return CallMainResult(currentMainSuit, currentCallerIndex, false, "只有级牌对或大小王对才能叫主")
        }

        // 反主判断
        if (currentCallerIndex >= 0 && currentCallCards != null) {
            val currentPriority = getCallPriority(currentCallCards)
            val newPriority = getCallPriority(cards)
            
            if (newPriority <= currentPriority) {
                return CallMainResult(currentMainSuit, currentCallerIndex, false, "无法反主，需要更高级的对子")
            }
        }

        val suitName = if (newMainSuit == null) "无主" else newMainSuit.displayName
        return CallMainResult(
            mainSuit = newMainSuit,
            callerIndex = callerIndex,
            success = true,
            message = "叫主成功：$suitName"
        )
    }

    /**
     * AI 自动叫主逻辑（增强版）
     *
     * 扫描手牌，找是否有可以叫主的对子，选择最优的叫主方案。
     * 考虑因素：
     * 1. 有大王对/小王对时优先叫无主
     * 2. 评估该花色的整体实力（牌数、大牌、对子）
     * 3. 如果当前已有人叫主，评估是否值得反主
     *
     * @return 要用来叫主的一对牌，或 null（不叫）
     */
    fun aiChooseCallMain(
        handCards: List<Card>,
        currentLevel: Rank,
        currentMainSuit: Suit?,
        currentCallerIndex: Int
    ): List<Card>? {
        // 优先级：大王对 > 小王对 > 级牌对（按花色优先级）

        // 大王对
        val bigJokers = handCards.filter { it.isBigJoker }
        if (bigJokers.size >= 2) return bigJokers.take(2)

        // 小王对
        val smallJokers = handCards.filter { it.rank == Rank.SMALL_JOKER }
        if (smallJokers.size >= 2) return smallJokers.take(2)

        // 级牌对（选花色优先级最高的）
        val levelCards = handCards.filter { it.rank == currentLevel }
        val levelBySuit = levelCards.groupBy { it.suit }
        
        // 按花色优先级选择：黑桃 > 红桃 > 梅花 > 方块
        val suitPriorityOrder = listOf(Suit.SPADE, Suit.HEART, Suit.CLUB, Suit.DIAMOND)
        
        // 评估每个花色的实力
        val suitScores = suitPriorityOrder.map { suit ->
            val cards = levelBySuit[suit]
            if (cards != null && cards.size >= 2) {
                val suitStrength = evaluateSuitForCallMain(handCards, suit, currentLevel)
                suit to suitStrength
            } else {
                suit to 0.0
            }
        }.filter { it.second > 0 }
        
        // 选择实力最强的花色
        val bestSuit = suitScores.maxByOrNull { it.second }
        if (bestSuit != null) {
            val cards = levelBySuit[bestSuit.first]
            if (cards != null && cards.size >= 2) {
                // 如果当前已有人叫主，评估是否值得反主
                if (currentCallerIndex >= 0 && currentMainSuit != null) {
                    // 如果叫主的花色实力不够强，不反主
                    if (bestSuit.second < 15.0) {
                        return null
                    }
                }
                return cards.take(2)
            }
        }

        return null
    }
    
    /**
     * 评估某个花色的叫主价值
     */
    private fun evaluateSuitForCallMain(
        handCards: List<Card>,
        suit: Suit,
        currentLevel: Rank
    ): Double {
        var score = 0.0
        
        // 该花色的牌数
        val suitCards = handCards.filter { it.suit == suit }
        score += suitCards.size * 3.0
        
        // 该花色的大牌
        val bigCards = suitCards.filter { it.rank.order >= Rank.TEN.order }
        score += bigCards.size * 5.0
        
        // 该花色的对子
        val pairs = suitCards.groupBy { it.rank }.values.count { it.size >= 2 }
        score += pairs * 8.0
        
        // 该花色的级牌
        val levelCards = suitCards.filter { it.rank == currentLevel }
        score += levelCards.size * 10.0
        
        return score
    }
}
