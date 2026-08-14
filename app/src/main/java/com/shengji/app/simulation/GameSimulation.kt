package com.shengji.app.simulation

import com.shengji.app.data.*
import com.shengji.app.enums.*
import com.shengji.app.viewmodel.GameViewModel

/**
 * 模拟10把完整升级游戏，检测牌数量不一致问题
 */
fun main() {
    println("╔══════════════════════════════════════╗")
    println("║   升级 - 10把模拟测试（检测数量问题） ║")
    println("╚══════════════════════════════════════╝\n")

    var totalErrors = 0
    var totalRounds = 0

    for (gameNum in 1..10) {
        println("\n${"=".repeat(50)}")
        println("第 ${gameNum} 把游戏")
        println("=".repeat(50))

        val result = simulateOneGame(gameNum)
        totalErrors += result.errors
        totalRounds += result.rounds
    }

    println("\n${"=".repeat(50)}")
    println("测试总结")
    println("=".repeat(50))
    println("完成 10 把游戏")
    println("总轮次: $totalRounds")
    println("数量不一致错误: $totalErrors")
    if (totalErrors == 0) {
        println("✅ 所有轮次出牌数量正确！")
    } else {
        println("❌ 发现 $totalErrors 处数量不一致！")
    }
}

data class TestResult(val rounds: Int, val errors: Int)

private fun simulateOneGame(gameNum: Int): TestResult {
    val vm = GameViewModel()
    var roundErrors = 0
    var roundCount = 0
    val roundDetails = mutableListOf<String>()

    // 静默模式，不打印日志
    vm.logCallback = { }

    // ── 发牌 ──
    vm.handleEvent(GameEvent.Deal)

    // ── 叫主 ──
    val stateAfterDeal = vm.state.value
    val humanHand = stateAfterDeal.players[0].handCards
    val levelPairs = humanHand
        .filter { it.rank == stateAfterDeal.currentLevel && !it.isJoker }
        .groupBy { it.suit }
        .filter { it.value.size >= 2 }

    if (levelPairs.isNotEmpty()) {
        val suit = levelPairs.keys.first()
        val cards = levelPairs[suit]!!.take(2)
        vm.handleEvent(GameEvent.CallMain(cards))
    } else {
        vm.handleEvent(GameEvent.PassCallMain)
    }

    // ── 出牌循环，同时记录每轮出牌数量 ──
    var safetyCounter = 0
    val maxIterations = 300
    var lastRoundPlays: List<Pair<Int, List<Card>>> = emptyList()
    var currentRoundLeadCount = 0

    while (safetyCounter++ < maxIterations) {
        val state = vm.state.value

        if (state.phase != GamePhase.PLAYING) break
        if (state.players.all { it.handCards.isEmpty() }) break

        val currentPlayer = state.currentPlayer ?: break

        // 检测新一轮开始
        if (state.isLeadPhase && state.currentRoundPlays.isEmpty() && lastRoundPlays.isNotEmpty()) {
            // 上一轮结束，检查数量一致性
            val counts = lastRoundPlays.map { it.second.size }.distinct()
            if (counts.size > 1) {
                roundErrors++
                val detail = "轮次${roundCount}: 数量不一致! ${lastRoundPlays.map { "玩家${it.first}:${it.second.size}张" }.joinToString(", ")}"
                roundDetails.add(detail)
                println("❌ $detail")
            }
            lastRoundPlays = emptyList()
        }

        // 人类玩家自动出牌
        if (currentPlayer.playerType == PlayerType.HUMAN && currentPlayer.handCards.isNotEmpty()) {
            val hand = currentPlayer.handCards.sortedBy {
                CardEvaluator.comparisonWeight(it, state.mainSuit, state.currentLevel)
            }

            if (state.isLeadPhase) {
                val card = hand.first()
                vm.handleEvent(GameEvent.LeadPlay(listOf(card)))
                currentRoundLeadCount = 1
                roundCount++
            } else {
                // 跟牌时，尝试出正确数量的牌
                val leadCombo = state.currentLeadCombo
                if (leadCombo != null) {
                    val requiredCount = leadCombo.cards.size
                    val leadSuit = leadCombo.suit
                    val suitedCards = hand.filter {
                        CardEvaluator.effectiveSuit(it, state.mainSuit, state.currentLevel) == leadSuit
                    }
                    
                    val cardsToPlay = if (suitedCards.isNotEmpty()) {
                        // 有首出花色
                        val takeCount = if (suitedCards.size < requiredCount) suitedCards.size else requiredCount
                        suitedCards.take(takeCount)
                    } else {
                        // 没有首出花色
                        val takeCount = if (hand.size < requiredCount) hand.size else requiredCount
                        hand.take(takeCount)
                    }
                    vm.handleEvent(GameEvent.FollowPlay(cardsToPlay))
                } else {
                    vm.handleEvent(GameEvent.FollowPlay(listOf(hand.first())))
                }
            }
        }

        // 记录当前轮次的出牌
        val currentPlays = vm.state.value.currentRoundPlays
        if (currentPlays.isNotEmpty()) {
            lastRoundPlays = currentPlays.toList()
        }
    }

    // 检查最后一轮
    if (lastRoundPlays.isNotEmpty()) {
        val counts = lastRoundPlays.map { it.second.size }.distinct()
        if (counts.size > 1) {
            roundErrors++
            val detail = "轮次${roundCount}: 数量不一致! ${lastRoundPlays.map { "玩家${it.first}:${it.second.size}张" }.joinToString(", ")}"
            roundDetails.add(detail)
            println("❌ $detail")
        }
    }

    // 打印本局结果
    val finalState = vm.state.value
    println("级牌: ${finalState.currentLevel.display}, 主花色: ${finalState.mainSuit?.displayName ?: "无主"}")
    println("轮次: $roundCount, 错误: $roundErrors")
    if (roundErrors == 0) {
        println("✅ 本局无数量问题")
    }

    return TestResult(roundCount, roundErrors)
}
