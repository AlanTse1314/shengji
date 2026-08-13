package com.shengji.app.data

import com.shengji.app.enums.ComboType
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * AI 机器人出牌逻辑（v4 - 增强策略 + 完整记牌器）
 *
 * 核心策略改进：
 * - 首出：清主、出长套、出对子/拖拉机、保留大牌
 * - 跟牌：根据分值决定是否压牌，正确跟对子/拖拉机
 * - 绝门：有分才杀，用最小主牌，考虑盖杀
 */
object AiPlayer {

    // 记牌器实例
    val tracker = CardTracker()

    fun resetTracker() {
        tracker.reset()
    }

    fun recordPlay(playerIndex: Int, cards: List<Card>) {
        tracker.recordPlay(playerIndex, cards)
    }

    // ═══════════════════════════════════════════════════
    // 首出
    // ═══════════════════════════════════════════════════

    fun chooseLead(
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank,
        teamScores: List<Int> = listOf(0, 0),
        myTeam: Int = 0
    ): List<Card> {
        if (handCards.isEmpty()) return emptyList()

        val possibleLeads = generatePossibleLeads(handCards, mainSuit, currentLevel)

        if (possibleLeads.isEmpty()) {
            return listOf(handCards.minBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) })
        }

        return possibleLeads.maxByOrNull { cards ->
            HeuristicEvaluator.scoreLead(
                cards = cards,
                handCards = handCards,
                mainSuit = mainSuit,
                currentLevel = currentLevel,
                tracker = tracker,
                teamScores = teamScores,
                myTeam = myTeam
            )
        } ?: possibleLeads.first()
    }

    // ═══════════════════════════════════════════════════
    // 跟牌
    // ═══════════════════════════════════════════════════

    fun chooseFollow(
        handCards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank,
        pointsInRound: Int,
        teamScores: List<Int> = listOf(0, 0),
        myTeam: Int = 0,
        isTeammateWinning: Boolean = false
    ): List<Card> {
        if (handCards.isEmpty()) return emptyList()

        val leadSuit = leadCombo.suit
        val cardsBySuit = handCards.groupBy {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel)
        }
        val leadSuitCards = cardsBySuit[leadSuit] ?: emptyList()

        return when (leadCombo.comboType) {
            ComboType.SINGLE -> followSingle(
                handCards, leadSuitCards, leadCombo, leadSuit, mainSuit, currentLevel,
                pointsInRound, isTeammateWinning, teamScores, myTeam
            )
            ComboType.PAIR -> followPair(
                handCards, leadSuitCards, leadCombo, mainSuit, currentLevel,
                pointsInRound, isTeammateWinning, teamScores, myTeam
            )
            ComboType.TRACTOR -> followTractor(
                handCards, leadSuitCards, leadCombo, mainSuit, currentLevel,
                pointsInRound, isTeammateWinning, teamScores, myTeam
            )
            else -> followGeneric(
                handCards, leadSuitCards, leadCombo, mainSuit, currentLevel,
                pointsInRound, isTeammateWinning, teamScores, myTeam
            )
        }
    }

    // ── 跟单张 ──

    private fun followSingle(
        handCards: List<Card>,
        leadSuitCards: List<Card>,
        leadCombo: PlayCombo,
        leadSuit: Suit?,
        mainSuit: Suit?,
        currentLevel: Rank,
        pointsInRound: Int,
        isTeammateWinning: Boolean,
        teamScores: List<Int>,
        myTeam: Int
    ): List<Card> {
        if (leadSuitCards.isNotEmpty()) {
            // 有首出花色，必须跟
            val possibleFollows = leadSuitCards.map { listOf(it) }

            return possibleFollows.maxByOrNull { cards ->
                HeuristicEvaluator.scoreFollow(
                    cards = cards,
                    leadCombo = leadCombo,
                    handCards = handCards,
                    mainSuit = mainSuit,
                    currentLevel = currentLevel,
                    tracker = tracker,
                    isTeammateWinning = isTeammateWinning,
                    pointsInRound = pointsInRound,
                    teamScores = teamScores,
                    myTeam = myTeam
                )
            } ?: possibleFollows.first()
        }

        // 没有首出花色
        return chooseWhenVoid(handCards, leadCombo, mainSuit, currentLevel, pointsInRound, isTeammateWinning)
    }

    // ── 跟对子 ──

    private fun followPair(
        handCards: List<Card>,
        leadSuitCards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank,
        pointsInRound: Int,
        isTeammateWinning: Boolean,
        teamScores: List<Int>,
        myTeam: Int
    ): List<Card> {
        if (leadSuitCards.isNotEmpty()) {
            // 找该花色的对子
            val pairs = findPairsInSuit(leadSuitCards, mainSuit, currentLevel)

            if (pairs.isNotEmpty()) {
                return pairs.maxByOrNull { cards ->
                    HeuristicEvaluator.scoreFollow(
                        cards = cards,
                        leadCombo = leadCombo,
                        handCards = handCards,
                        mainSuit = mainSuit,
                        currentLevel = currentLevel,
                        tracker = tracker,
                        isTeammateWinning = isTeammateWinning,
                        pointsInRound = pointsInRound,
                        teamScores = teamScores,
                        myTeam = myTeam
                    )
                } ?: pairs.first()
            }

            // 没有对子，按规则出两张最小的同花色牌
            return leadSuitCards
                .sortedBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                .take(2)
        }

        return chooseWhenVoid(handCards, leadCombo, mainSuit, currentLevel, pointsInRound, isTeammateWinning)
    }

    // ── 跟拖拉机 ──

    private fun followTractor(
        handCards: List<Card>,
        leadSuitCards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank,
        pointsInRound: Int,
        isTeammateWinning: Boolean,
        teamScores: List<Int>,
        myTeam: Int
    ): List<Card> {
        val requiredCount = leadCombo.cards.size

        if (leadSuitCards.isNotEmpty()) {
            // 找该花色的拖拉机
            val tractors = findTractorsInSuit(leadSuitCards, mainSuit, currentLevel)
            // 找长度匹配的拖拉机
            val matchingTractors = tractors.filter { it.size == requiredCount }

            if (matchingTractors.isNotEmpty()) {
                return matchingTractors.maxByOrNull { cards ->
                    HeuristicEvaluator.scoreFollow(
                        cards = cards,
                        leadCombo = leadCombo,
                        handCards = handCards,
                        mainSuit = mainSuit,
                        currentLevel = currentLevel,
                        tracker = tracker,
                        isTeammateWinning = isTeammateWinning,
                        pointsInRound = pointsInRound,
                        teamScores = teamScores,
                        myTeam = myTeam
                    )
                } ?: matchingTractors.first()
            }

            // 没有匹配的拖拉机，尝试凑对子
            val pairs = findPairsInSuit(leadSuitCards, mainSuit, currentLevel)
            if (pairs.size * 2 >= requiredCount) {
                // 用最小的对子组合
                val sortedPairs = pairs.sortedBy { p ->
                    p.sumOf { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                }
                val combined = mutableListOf<Card>()
                for (pair in sortedPairs) {
                    if (combined.size >= requiredCount) break
                    combined.addAll(pair)
                }
                if (combined.size >= requiredCount) return combined.take(requiredCount)
            }

            // 都没有，出最小的同花色牌凑数
            return leadSuitCards
                .sortedBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                .take(requiredCount)
        }

        return chooseWhenVoid(handCards, leadCombo, mainSuit, currentLevel, pointsInRound, isTeammateWinning)
    }

    // ── 跟甩牌/通用 ──

    private fun followGeneric(
        handCards: List<Card>,
        leadSuitCards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank,
        pointsInRound: Int,
        isTeammateWinning: Boolean,
        teamScores: List<Int>,
        myTeam: Int
    ): List<Card> {
        val requiredCount = leadCombo.cards.size

        if (leadSuitCards.isNotEmpty()) {
            // 有同花色，出最小的
            return leadSuitCards
                .sortedBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                .take(requiredCount)
        }

        return chooseWhenVoid(handCards, leadCombo, mainSuit, currentLevel, pointsInRound, isTeammateWinning)
    }

    // ═══════════════════════════════════════════════════
    // 绝门策略（没有首出花色时）
    // ═══════════════════════════════════════════════════

    private fun chooseWhenVoid(
        handCards: List<Card>,
        leadCombo: PlayCombo,
        mainSuit: Suit?,
        currentLevel: Rank,
        pointsInRound: Int,
        isTeammateWinning: Boolean
    ): List<Card> {
        val trumpCards = handCards.filter { CardEvaluator.isMainTrump(it, mainSuit, currentLevel) }
        val nonTrumpCards = handCards.filter { !CardEvaluator.isMainTrump(it, mainSuit, currentLevel) }
        val requiredCount = leadCombo.cards.size

        // ── 队友在赢 ──
        if (isTeammateWinning) {
            // 不杀，出废牌
            val wasteCards = nonTrumpCards.filter { it.pointValue == 0 }
            if (wasteCards.size >= requiredCount) {
                return wasteCards
                    .sortedBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                    .take(requiredCount)
            }
            return handCards
                .sortedBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                .take(requiredCount)
        }

        // ── 对手在赢 ──

        // 评估是否应该杀
        if (leadCombo.comboType == ComboType.PAIR) {
            // 对子必须用主牌对子杀
            val trumpPairs = findPairsInSuit(trumpCards, mainSuit, currentLevel)
            if (trumpPairs.isNotEmpty() && HeuristicEvaluator.shouldKill(handCards, mainSuit, currentLevel, pointsInRound, tracker)) {
                // 用最小的主牌对子杀
                return trumpPairs.minByOrNull { p ->
                    p.sumOf { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                } ?: trumpPairs.first()
            }
        } else if (leadCombo.comboType == ComboType.TRACTOR) {
            // 拖拉机必须用主牌拖拉机杀
            val trumpTractors = findTractorsInSuit(trumpCards, mainSuit, currentLevel)
            val matchingTractors = trumpTractors.filter { it.size == requiredCount }
            if (matchingTractors.isNotEmpty() && pointsInRound >= 20) {
                return matchingTractors.minByOrNull { t ->
                    t.sumOf { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                } ?: matchingTractors.first()
            }
        } else {
            // 单张，用主牌杀
            if (HeuristicEvaluator.shouldKill(handCards, mainSuit, currentLevel, pointsInRound, tracker)) {
                if (trumpCards.isNotEmpty()) {
                    // 用最小的主牌杀
                    val killCard = trumpCards.minBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                    return listOf(killCard)
                }
            }
        }

        // 不杀或杀不了，出最小废牌
        val wasteCards = nonTrumpCards.filter { it.pointValue == 0 }
        if (wasteCards.size >= requiredCount) {
            return wasteCards
                .sortedBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
                .take(requiredCount)
        }

        return handCards
            .sortedBy { HeuristicEvaluator.cardStrength(it, mainSuit, currentLevel) }
            .take(requiredCount)
    }

    // ═══════════════════════════════════════════════════
    // 生成首出牌型
    // ═══════════════════════════════════════════════════

    private fun generatePossibleLeads(
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): List<List<Card>> {
        val result = mutableListOf<List<Card>>()

        val cardsBySuit = handCards.groupBy {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel)
        }

        for ((suit, cards) in cardsBySuit) {
            // 单张
            cards.forEach { result.add(listOf(it)) }

            // 对子
            val pairs = findPairsInSuit(cards, mainSuit, currentLevel)
            result.addAll(pairs)

            // 拖拉机
            val tractors = findTractorsInSuit(cards, mainSuit, currentLevel)
            result.addAll(tractors)
        }

        return result
    }

    // ═══════════════════════════════════════════════════
    // 找对子和拖拉机
    // ═══════════════════════════════════════════════════

    /**
     * 在一组同花色牌中找所有对子
     */
    private fun findPairsInSuit(
        cards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): List<List<Card>> {
        val groups = cards.groupBy { "${it.rank}_${it.suit}" }
        return groups.values.filter { it.size >= 2 }.map { it.take(2) }
    }

    /**
     * 在一组同花色牌中找所有拖拉机（连续对子）
     *
     * 注意：级牌位置要跳过（如打7，6688是拖拉机）
     */
    private fun findTractorsInSuit(
        cards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank
    ): List<List<Card>> {
        val pairs = findPairsInSuit(cards, mainSuit, currentLevel)
        if (pairs.size < 2) return emptyList()

        val result = mutableListOf<List<Card>>()

        // 按 rank 排序
        val sortedPairs = pairs.sortedBy { it.first().rank.order }

        // 找连续的对子（跳过级牌位置）
        for (i in 0 until sortedPairs.size - 1) {
            val currentRank = sortedPairs[i].first().rank
            val nextRank = sortedPairs[i + 1].first().rank

            // 检查是否连续（跳过级牌）
            if (isConsecutiveSkippingLevel(currentRank, nextRank, currentLevel)) {
                result.add(sortedPairs[i] + sortedPairs[i + 1])
            }
        }

        // 找更长的拖拉机（3对及以上）
        for (i in 0 until sortedPairs.size - 2) {
            var endIdx = i + 1
            while (endIdx < sortedPairs.size - 1) {
                val prevRank = sortedPairs[endIdx].first().rank
                val nextRank = sortedPairs[endIdx + 1].first().rank
                if (isConsecutiveSkippingLevel(prevRank, nextRank, currentLevel)) {
                    endIdx++
                } else {
                    break
                }
            }
            if (endIdx > i) {
                val tractor = mutableListOf<Card>()
                for (j in i..endIdx) {
                    tractor.addAll(sortedPairs[j])
                }
                if (tractor.size >= 4 && tractor.size !in result.map { it.size }) {
                    result.add(tractor)
                }
            }
        }

        return result
    }

    /**
     * 判断两个 rank 是否连续（跳过级牌位置）
     *
     * 如打7：6和8是连续的（跳过了7）
     */
    private fun isConsecutiveSkippingLevel(r1: Rank, r2: Rank, currentLevel: Rank): Boolean {
        val order1 = r1.order
        val order2 = r2.order
        val levelOrder = currentLevel.order

        // 正常连续
        if (order2 - order1 == 1) return true

        // 跳过级牌的情况：如打7，6(order5)和8(order7)之间跳过了7(order6)
        if (order2 - order1 == 2) {
            val betweenOrder = order1 + 1
            if (betweenOrder == levelOrder) return true
        }

        return false
    }

    // ═══════════════════════════════════════════════════
    // 叫主
    // ═══════════════════════════════════════════════════

    fun chooseCallMain(
        handCards: List<Card>,
        currentLevel: Rank,
        currentMainSuit: Suit?,
        currentCallerIndex: Int
    ): List<Card>? {
        return CallMainUseCase.aiChooseCallMain(handCards, currentLevel, currentMainSuit, currentCallerIndex)
    }
}
