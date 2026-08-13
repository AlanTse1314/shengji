package com.shengji.app.simulation

import com.shengji.app.data.*
import com.shengji.app.enums.*
import com.shengji.app.viewmodel.GameViewModel

/**
 * 模拟一局完整升级游戏
 *
 * 所有逻辑由 ViewModel 驱动，此脚本只负责触发事件和打印日志。
 */
fun main() {
    println("╔══════════════════════════════════════╗")
    println("║        🃏 升级 - 模拟一局游戏        ║")
    println("╚══════════════════════════════════════╝\n")

    val vm = GameViewModel()
    vm.logCallback = { println(it) }

    // ── 第1步：发牌 ──────────────────────────
    println("【第1步】发牌")
    vm.handleEvent(GameEvent.Deal)

    // ── 第2步：叫主 ──────────────────────────
    println("\n【第2步】叫主")
    val stateAfterDeal = vm.state.value
    val humanHand = stateAfterDeal.players[0].handCards

    // 找人类玩家的级牌对
    val levelPairs = humanHand
        .filter { it.rank == stateAfterDeal.currentLevel && !it.isJoker }
        .groupBy { it.suit }
        .filter { it.value.size >= 2 }

    if (levelPairs.isNotEmpty()) {
        val suit = levelPairs.keys.first()
        val cards = levelPairs[suit]!!.take(2)
        println("你选择叫主: ${cards.joinToString { it.displayName }}")
        vm.handleEvent(GameEvent.CallMain(cards))
    } else {
        println("你没有级牌对，跳过叫主")
        vm.handleEvent(GameEvent.PassCallMain)
    }

    // ── 第3步：出牌 ──────────────────────────
    println("\n【第3步】出牌")
    playGameLoop(vm)

    // ── 第4步：抠底 ──────────────────────────
    if (vm.state.value.phase == GamePhase.DIG_BOTTOM) {
        println("\n【第4步】抠底")
        vm.handleEvent(GameEvent.ConfirmDigBottom)
    }

    // ── 第5步：结算 ──────────────────────────
    if (vm.state.value.phase == GamePhase.ROUND_SETTLE) {
        println("\n【第5步】结算")
        vm.handleEvent(GameEvent.ConfirmSettle)
    }

    // ── 最终报告 ──────────────────────────────
    printFinalReport(vm)
}

/**
 * 出牌主循环
 */
private fun playGameLoop(vm: GameViewModel) {
    var safetyCounter = 0
    val maxIterations = 200

    while (safetyCounter++ < maxIterations) {
        val state = vm.state.value

        if (state.phase != GamePhase.PLAYING) break
        if (state.players.all { it.handCards.isEmpty() }) break

        val currentPlayer = state.currentPlayer ?: break

        // 人类玩家自动出牌（模拟用最小牌策略）
        if (currentPlayer.playerType == PlayerType.HUMAN && currentPlayer.handCards.isNotEmpty()) {
            val hand = currentPlayer.handCards.sortedBy {
                CardEvaluator.comparisonWeight(it, state.mainSuit, state.currentLevel)
            }

            if (state.isLeadPhase) {
                // 首出：出最小的牌
                val card = hand.first()
                vm.handleEvent(GameEvent.LeadPlay(listOf(card)))
            } else {
                // 跟牌：出最小的牌
                val card = hand.first()
                vm.handleEvent(GameEvent.FollowPlay(listOf(card)))
            }
        }

        // AI 赢轮后的自动首出由 ViewModel 内部处理
        // 如果当前仍是 AI 的回合（AI 赢了轮次），ViewModel 已自动处理
    }

    if (safetyCounter >= maxIterations) {
        println("⚠️ 达到安全循环上限，强制结束")
    }
}

/**
 * 打印最终报告
 */
private fun printFinalReport(vm: GameViewModel) {
    val finalState = vm.state.value

    println("\n╔══════════════════════════════════════╗")
    println("║            游戏结束                   ║")
    println("╚══════════════════════════════════════╝")
    println("阶段: ${finalState.phase}")
    println("级牌: ${finalState.currentLevel.display}")
    println("主花色: ${finalState.mainSuit?.displayName ?: "无主"}")
    println("队伍0得分: ${finalState.teamScores.getOrElse(0) { 0 }}")
    println("队伍1得分: ${finalState.teamScores.getOrElse(1) { 0 }}")
    println("庄家队伍: 队伍${finalState.dealerTeam}")
    println("出牌轮次: ${finalState.roundNumber}")

    println("\n--- 玩家状态 ---")
    finalState.players.forEach { player ->
        val teamLabel = if (player.teamIndex == 0) "队伍A" else "队伍B"
        val typeLabel = if (player.playerType == PlayerType.HUMAN) "🧑" else "🤖"
        println("$typeLabel ${player.displayName} [$teamLabel]: " +
                "手牌${player.handCardCount}张, 已出${player.playedCards.size}张")
    }
}
