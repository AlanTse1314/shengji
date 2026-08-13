package com.shengji.app.data

import com.shengji.app.enums.ComboType
import com.shengji.app.enums.Rank
import com.shengji.app.enums.Suit

/**
 * 启发式评分器 - 为每张可能出的牌打分（v2）
 *
 * 核心策略：
 * - 首出：清主 > 出长套 > 出对子/拖拉机 > 出小牌
 * - 跟牌：队友在赢不出分，对手在赢看分值决定是否压
 * - 绝门：有分才杀，用最小主牌杀
 */
object HeuristicEvaluator {

    /**
     * 首出评分
     */
    fun scoreLead(
        cards: List<Card>,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank,
        tracker: CardTracker,
        teamScores: List<Int>,
        myTeam: Int
    ): Double {
        var score = 0.0
        val card = cards.first()
        val suit = CardEvaluator.effectiveSuit(card, mainSuit, currentLevel)
        val isTrump = CardEvaluator.isMainTrump(card, mainSuit, currentLevel)

        // ── 1. 主牌首出（清主策略）──
        if (isTrump) {
            val myTrumpCount = handCards.count { CardEvaluator.isMainTrump(it, mainSuit, currentLevel) }
            val remainingTrump = tracker.getRemainingTrumpCount(handCards, mainSuit, currentLevel)

            // 有很多主牌时积极清主
            if (myTrumpCount >= 6) {
                score += 30.0 + myTrumpCount * 2.0
            } else if (myTrumpCount >= 4) {
                score += 15.0
            }

            // 对子/拖拉机清主更高效
            if (cards.size >= 2) {
                score += cards.size * 5.0
            }

            // 主牌中的分牌要谨慎（5、10、K在主牌中很值钱）
            val trumpPoints = cards.sumOf { it.pointValue }
            score -= trumpPoints * 0.5
        }

        // ── 2. 副牌首出 ──
        if (!isTrump) {
            val suitCards = handCards.filter {
                CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
            }
            val suitCount = suitCards.size

            // 长套优先（可以甩牌）
            if (suitCount >= 5) {
                score += 20.0 + suitCount * 2.0
            } else if (suitCount >= 3) {
                score += 10.0
            }

            // 短套（只有1-2张）不适合首出
            if (suitCount <= 2) {
                score -= 10.0
            }

            // 对子首出价值高（可以逼对手出大牌或拆对子）
            if (cards.size == 2 && cards[0].rank == cards[1].rank) {
                score += 20.0
                // 高对更好
                if (cards[0].rank.order >= Rank.TEN.order) {
                    score += 10.0
                }
            }

            // 拖拉机首出价值很高
            if (cards.size >= 4) {
                score += 25.0 + cards.size * 3.0
            }

            // 有该花色的分牌时，首出可以收分
            val suitPoints = suitCards.sumOf { it.pointValue }
            if (suitPoints >= 15) {
                score += 10.0
            }
        }

        // ── 3. 分牌扣分（保留分牌等队友收） ──
        val pointValue = cards.sumOf { it.pointValue }
        if (pointValue > 0 && !isTrump) {
            // 副牌分牌首出要谨慎，除非是长套
            val suitCards = handCards.filter {
                CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
            }
            if (suitCards.size < 4) {
                score -= pointValue * 0.3
            }
        }

        // ── 4. 大牌保留 ──
        val highCardCount = cards.count { it.rank == Rank.ACE || it.rank == Rank.KING }
        if (highCardCount > 0 && cards.size <= 2) {
            // 单张大牌首出扣分（留着收分更好）
            score -= highCardCount * 8.0
        }

        // ── 5. 花色控制力 ──
        val suitControl = evaluateSuitControl(handCards, suit, mainSuit, currentLevel)
        score += suitControl * 0.3

        // ── 6. 游戏分数策略 ──
        val myScore = teamScores.getOrElse(myTeam) { 0 }
        val opponentScore = teamScores.getOrElse(1 - myTeam) { 0 }

        // 对手分数高时更激进
        if (opponentScore >= 60) {
            score += 10.0
        }
        // 自己队伍分数领先时保守
        if (myScore >= 60) {
            score -= 5.0
        }

        return score
    }

    /**
     * 跟牌评分
     */
    fun scoreFollow(
        cards: List<Card>,
        leadCombo: PlayCombo,
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank,
        tracker: CardTracker,
        isTeammateWinning: Boolean,
        pointsInRound: Int,
        teamScores: List<Int>,
        myTeam: Int
    ): Double {
        var score = 0.0

        val cardStrength = cards.sumOf { cardStrength(it, mainSuit, currentLevel) }
        val pointValue = cards.sumOf { it.pointValue }
        val isFollowingSuit = cards.all {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadCombo.suit
        }

        // ── 队友在赢 ──
        if (isTeammateWinning) {
            if (pointsInRound >= 20) {
                // 场上分多，考虑出分牌帮队友收分
                score += pointValue * 0.5
                // 但也别出太大的牌
                score -= cardStrength * 0.3
            } else {
                // 场上没分，出最小的牌保留实力
                score -= cardStrength * 0.8
                score -= pointValue * 0.5
            }
            return score
        }

        // ── 对手在赢 ──
        if (isFollowingSuit) {
            // 跟花色
            val leadSuitCards = handCards.filter {
                CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == leadCombo.suit
            }
            val leadCardStrength = leadCombo.cards.maxOf { comparisonWeight(it, mainSuit, currentLevel) }
            val myMaxStrength = cards.maxOf { comparisonWeight(it, mainSuit, currentLevel) }

            if (myMaxStrength > leadCardStrength) {
                // 能赢
                if (pointsInRound >= 15) {
                    // 有分要抢
                    score += 30.0 + pointsInRound * 0.5
                    // 但不要用太大的牌赢小分
                    val overkill = myMaxStrength - leadCardStrength
                    if (overkill > 50) {
                        score -= overkill * 0.2
                    }
                } else {
                    // 没分，能不赢就不赢
                    score -= 15.0
                    // 除非手牌很少，需要争出牌权
                    if (handCards.size <= 5) {
                        score += 10.0
                    }
                }
            } else {
                // 赢不了，出最小的
                score -= cardStrength * 0.5
                // 但如果有分牌，考虑是否要送分
                if (pointValue > 0) {
                    // 场上已经有分了，再送分不划算
                    if (pointsInRound >= 20) {
                        score -= pointValue * 0.3
                    }
                }
            }
        } else {
            // 不跟花色（绝门了）
            val trumpCards = handCards.filter { CardEvaluator.isMainTrump(it, mainSuit, currentLevel) }
            val isKilling = cards.any { CardEvaluator.isMainTrump(it, mainSuit, currentLevel) }

            if (isKilling) {
                // 杀牌
                if (pointsInRound >= 15) {
                    // 有分值得杀
                    score += 25.0 + pointsInRound * 0.3
                    // 用小主牌杀更好
                    score -= cardStrength * 0.3
                } else {
                    // 没分不值得杀
                    score -= 20.0
                }
            } else {
                // 垫牌（不出主牌）
                // 出废牌
                score -= cardStrength * 0.6
                // 有分的废牌先出掉
                score += pointValue * 0.2
            }
        }

        return score
    }

    /**
     * 牌力评估 - 单张牌的价值（用于排序和比较）
     */
    fun cardStrength(
        card: Card,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Double {
        var strength = 0.0

        // 基础点数
        strength += card.rank.order * 2.0

        // 主牌加成
        if (card.isMainTrump) {
            strength += 20.0
            if (card.isBigJoker) strength += 30.0
            else if (card.isJoker) strength += 25.0
        }

        // 级牌加成
        if (card.rank == currentLevel) {
            strength += 15.0
        }

        // 分牌加成
        strength += card.pointValue * 0.5

        return strength
    }

    /**
     * 比较权重（用于判断能否赢过对手）
     */
    private fun comparisonWeight(card: Card, mainSuit: Suit?, currentLevel: Rank): Int {
        return CardEvaluator.comparisonWeight(card, mainSuit, currentLevel)
    }

    /**
     * 花色控制评估 - 评估对某个花色的控制力
     */
    private fun evaluateSuitControl(
        handCards: List<Card>,
        suit: Suit?,
        mainSuit: Suit?,
        currentLevel: Rank
    ): Double {
        val suitCards = handCards.filter {
            CardEvaluator.effectiveSuit(it, mainSuit, currentLevel) == suit
        }

        if (suitCards.isEmpty()) return 0.0

        var control = 0.0

        // 数量优势
        control += suitCards.size * 2.0

        // 大牌优势
        val highCards = suitCards.filter { it.rank == Rank.ACE || it.rank == Rank.KING }
        control += highCards.size * 5.0

        // 对子优势
        val pairs = suitCards.groupBy { it.rank }.values.count { it.size >= 2 }
        control += pairs * 8.0

        return control
    }

    /**
     * 评估是否应该杀牌
     */
    fun shouldKill(
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank,
        pointsInRound: Int,
        tracker: CardTracker
    ): Boolean {
        // 没分不杀
        if (pointsInRound < 10) return false

        val trumpCards = handCards.filter { CardEvaluator.isMainTrump(it, mainSuit, currentLevel) }
        if (trumpCards.isEmpty()) return false

        // 场上分多，必须杀
        if (pointsInRound >= 30) return true

        // 评估杀牌的性价比
        val minTrumpStrength = trumpCards.minOf { cardStrength(it, mainSuit, currentLevel) }
        val remainingTrump = tracker.getRemainingTrumpCount(handCards, mainSuit, currentLevel)

        // 主牌充足时可以杀
        if (trumpCards.size >= 4 && pointsInRound >= 15) return true

        // 主牌不多时只杀大分
        if (trumpCards.size <= 2 && pointsInRound < 30) return false

        return pointsInRound > minTrumpStrength * 2
    }

    /**
     * 评估是否应该盖杀（用更大的主牌压过已有的杀牌）
     */
    fun shouldOverkill(
        handCards: List<Card>,
        mainSuit: Suit?,
        currentLevel: Rank,
        pointsInRound: Int,
        existingKillStrength: Int
    ): Boolean {
        // 没分不盖杀
        if (pointsInRound < 20) return false

        val trumpCards = handCards.filter { CardEvaluator.isMainTrump(it, mainSuit, currentLevel) }
        // 有更大的主牌才能盖杀
        val biggerTrumps = trumpCards.filter {
            comparisonWeight(it, mainSuit, currentLevel) > existingKillStrength
        }

        if (biggerTrumps.isEmpty()) return false

        // 分够多就盖杀
        return pointsInRound >= 30
    }
}
