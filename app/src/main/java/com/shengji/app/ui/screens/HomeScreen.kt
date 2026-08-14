package com.shengji.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shengji.app.ui.utils.rememberScreenSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasSavedProgress: Boolean = false,
    onContinueGame: () -> Unit = {},
    onNewGame: () -> Unit,
    onMultiplayer: () -> Unit,
    onHistory: () -> Unit
) {
    var showRules by remember { mutableStateOf(false) }
    val screenSize = rememberScreenSize()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = { showRules = !showRules }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "规则说明"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(horizontal = screenSize.spacingLarge * 2),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🃏",
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(screenSize.spacingLarge))

                Text(
                    text = "升级",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "纸牌游戏",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(screenSize.spacingLarge * 2))

                // 继续游戏 + 开始新游戏 - 同一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(screenSize.spacingMedium)
                ) {
                    // 继续游戏按钮（只有有存档时显示）
                    if (hasSavedProgress) {
                        Button(
                            onClick = onContinueGame,
                            modifier = Modifier
                                .weight(1f)
                                .height(screenSize.buttonHeightLarge),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("继续游戏", style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    // 开始新游戏按钮
                    Button(
                        onClick = onNewGame,
                        modifier = Modifier
                            .weight(1f)
                            .height(screenSize.buttonHeightLarge)
                    ) {
                        Text("开始新游戏", style = MaterialTheme.typography.titleSmall)
                    }
                }

                Spacer(modifier = Modifier.height(screenSize.spacingMedium))

                // 联机对战 和 历史记录 占一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(screenSize.spacingMedium)
                ) {
                    Button(
                        onClick = onMultiplayer,
                        modifier = Modifier
                            .weight(1f)
                            .height(screenSize.buttonHeightLarge),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("联机对战", style = MaterialTheme.typography.titleSmall)
                    }

                    OutlinedButton(
                        onClick = onHistory,
                        modifier = Modifier
                            .weight(1f)
                            .height(screenSize.buttonHeightLarge)
                    ) {
                        Text("历史记录", style = MaterialTheme.typography.titleSmall)
                    }
                }

                Spacer(modifier = Modifier.height(screenSize.spacingLarge * 2))

                Text(
                    text = "v1.0 · 单机版",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    // 规则说明弹窗
    if (showRules) {
        RulesDialog(onDismiss = { showRules = false })
    }
}

@Composable
private fun RulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📖 游戏规则",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                val rules = listOf(
                    "👥 4 人游戏，2 人一队，对家为队友",
                    "🃏 使用 2 副牌（108 张），每人 25 张，8 张底牌",
                    "🎯 级牌从 2 开始，赢牌升级，先打过 A 获胜",
                    "♠ 叫主：一对级牌或大小王对可确定主花色",
                    "🃏 主牌 = 大小王 + 主花色牌 + 级牌",
                    "🃏 出牌：首出任意牌型，跟牌必须跟同花色",
                    "🃏 无花色可出主牌「杀」，也可出废牌",
                    "🃏 牌型：单张、对子、拖拉机（连续对子）",
                    "🃏 计分：5=5分, 10=10分, K=10分，共 200 分",
                    "🃏 抠底：最后一轮赢家拿底牌，分值按牌型翻倍",
                    "🃏 升级：0分升3级, 5-35升2级, 40-75升1级",
                    "🃏 80分以上闲家上庄，120分以上升1级"
                )

                rules.forEach { rule ->
                    Text(
                        text = rule,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}
