package com.shengji.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shengji.app.data.ScoringUseCase.SettlementResult
import com.shengji.app.enums.Rank

/**
 * 结算弹窗
 */
@Composable
fun SettlementDialog(
    result: SettlementResult,
    currentLevel: Rank,
    dealerTeam: Int,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = if (result.dealerChanged) "🔄 换庄！" else "✅ 守台成功",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (result.dealerChanged)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 得分
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScoreColumn(
                        teamName = "庄家方",
                        score = result.defenderScore,
                        isDefender = true
                    )
                    ScoreColumn(
                        teamName = "闲家方",
                        score = result.attackerScore,
                        isDefender = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 升降级信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (result.dealerChanged) {
                            val levelUp = result.attackerLevelUp
                            if (levelUp > 0) {
                                Text(
                                    text = "闲家升 $levelUp 级！",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "闲家上台，不升级",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        } else {
                            val levelUp = result.defenderLevelUp
                            if (levelUp > 0) {
                                Text(
                                    text = "庄家升 $levelUp 级！",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "庄家守台成功",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "新级牌：${result.newLevel.display}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("继续", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun ScoreColumn(
    teamName: String,
    score: Int,
    isDefender: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = teamName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$score",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDefender)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
        Text(
            text = "分",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 叫主选择弹窗
 */
@Composable
fun CallMainDialog(
    handCards: List<com.shengji.app.data.Card>,
    currentLevel: Rank,
    onCallMain: (List<com.shengji.app.data.Card>) -> Unit,
    onPass: () -> Unit
) {
    // 找可叫主的对子
    val availablePairs = findCallablePairs(handCards, currentLevel)

    Dialog(onDismissRequest = onPass) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "叫主",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "选择一对级牌或大小王对来叫主",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availablePairs.isEmpty()) {
                    Text(
                        text = "没有可叫主的牌对",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    availablePairs.forEach { pair ->
                        val suitName = if (pair.first().isJoker) "无主"
                                       else pair.first().suit.displayName
                        OutlinedButton(
                            onClick = { onCallMain(pair) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                "${pair.joinToString { it.displayName }} → $suitName"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onPass,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("不叫主（跳过）")
                }
            }
        }
    }
}

/**
 * 找出手牌中可叫主的对子
 */
private fun findCallablePairs(
    handCards: List<com.shengji.app.data.Card>,
    currentLevel: Rank
): List<List<com.shengji.app.data.Card>> {
    val result = mutableListOf<List<com.shengji.app.data.Card>>()

    // 大王对
    val bigJokers = handCards.filter { it.isBigJoker }
    if (bigJokers.size >= 2) result.add(bigJokers.take(2))

    // 小王对
    val smallJokers = handCards.filter { it.rank == Rank.SMALL_JOKER }
    if (smallJokers.size >= 2) result.add(smallJokers.take(2))

    // 级牌对
    val levelCards = handCards.filter { it.rank == currentLevel && !it.isJoker }
    val levelBySuit = levelCards.groupBy { it.suit }
    for ((_, cards) in levelBySuit) {
        if (cards.size >= 2) result.add(cards.take(2))
    }

    return result
}
