package com.shengji.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shengji.app.data.Card
import com.shengji.app.enums.Suit

/**
 * 花色颜色
 */
private fun suitColor(suit: Suit): Color = when (suit) {
    Suit.HEART, Suit.DIAMOND -> Color(0xFFD32F2F)
    Suit.SPADE, Suit.CLUB -> Color(0xFF212121)
    Suit.JOKER -> Color(0xFFFF8F00)
}

private fun suitSymbol(suit: Suit): String = when (suit) {
    Suit.SPADE -> "♠"
    Suit.HEART -> "♥"
    Suit.CLUB -> "♣"
    Suit.DIAMOND -> "♦"
    Suit.JOKER -> "🃏"
}

/**
 * 卡牌组件
 */
@Composable
fun CardView(
    card: Card? = null,
    selected: Boolean = false,
    faceUp: Boolean = true,
    width: Dp = 56.dp,
    onClick: (() -> Unit)? = null
) {
    val height = width * 1.4f
    val shape = RoundedCornerShape(6.dp)

    val bgColor = when {
        !faceUp -> Color(0xFF1565C0)
        selected -> Color(0xFFFFF9C4)
        else -> Color.White
    }

    val borderStroke = when {
        selected -> BorderStroke(2.dp, Color(0xFFFF6F00))
        else -> BorderStroke(1.dp, Color(0xFFBDBDBD))
    }

    val yOffset = if (selected) (-8).dp else 0.dp

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .offset(y = yOffset)
            .shadow(2.dp, shape)
            .clip(shape)
            .background(bgColor)
            .border(borderStroke, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (!faceUp || card == null) {
            // 牌背
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1565C0), shape)
                    .border(1.dp, Color(0xFF0D47A1), shape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🂠",
                    fontSize = (width.value * 0.5f).sp,
                    color = Color.White
                )
            }
        } else if (card.isJoker) {
            // 大小王 - 左上角显示
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 3.dp, top = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (card.isBigJoker) "大" else "小",
                    fontSize = (width.value * 0.22f).sp,
                    fontWeight = FontWeight.Bold,
                    color = suitColor(card.suit)
                )
                Text(
                    text = "王",
                    fontSize = (width.value * 0.22f).sp,
                    fontWeight = FontWeight.Bold,
                    color = suitColor(card.suit)
                )
            }
            // 中心大字
            Text(
                text = if (card.isBigJoker) "🃏" else "🃏",
                fontSize = (width.value * 0.45f).sp,
                color = suitColor(card.suit).copy(alpha = 0.3f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // 普通牌 - 左上角显示数字和花色
            val textColor = suitColor(card.suit)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 3.dp, top = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = card.rank.display,
                    fontSize = (width.value * 0.22f).sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = suitSymbol(card.suit),
                    fontSize = (width.value * 0.2f).sp,
                    color = textColor
                )
            }
            // 中心大花色符号
            Text(
                text = suitSymbol(card.suit),
                fontSize = (width.value * 0.45f).sp,
                color = textColor.copy(alpha = 0.25f),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 主牌标记
        if (card != null && card.isMainTrump && faceUp) {
            Text(
                text = "★",
                fontSize = (width.value * 0.2f).sp,
                color = Color(0xFFFF6F00),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            )
        }
    }
}

/**
 * 牌背组件
 */
@Composable
fun CardBack(width: Dp = 40.dp) {
    CardView(card = null, faceUp = false, width = width)
}

/**
 * 一行手牌（横向排列，带重叠，自动适配宽度）
 */
@Composable
fun HandCardsRow(
    cards: List<Card>,
    selectedIndices: Set<Int> = emptySet(),
    faceUp: Boolean = true,
    cardWidth: Dp = 56.dp,
    overlap: Dp = 28.dp,
    maxWidth: Dp = Dp.Unspecified,
    onCardClick: ((Int) -> Unit)? = null
) {
    if (cards.isEmpty()) return

    // 计算实际重叠量
    val actualOverlap = if (maxWidth != Dp.Unspecified && cards.size > 1) {
        val totalSpace = maxWidth - cardWidth
        val stepNeeded = totalSpace / (cards.size - 1)
        val overlapNeeded = cardWidth - stepNeeded
        val minOverlap = cardWidth * 0.35f
        val maxOverlap = cardWidth - 4.dp
        overlapNeeded.coerceIn(minOverlap, maxOverlap)
    } else {
        overlap
    }

    val step = cardWidth - actualOverlap
    val totalWidth = cardWidth + step * (cards.size - 1).coerceAtLeast(0)

    Box(
        modifier = Modifier.width(totalWidth),
        contentAlignment = Alignment.CenterStart
    ) {
        cards.forEachIndexed { index, card ->
            Box(
                modifier = Modifier.offset(x = step * index)
            ) {
                CardView(
                    card = card,
                    selected = index in selectedIndices,
                    faceUp = faceUp,
                    width = cardWidth,
                    onClick = onCardClick?.let { cb -> { cb(index) } }
                )
            }
        }
    }
}

/**
 * AI 手牌展示（牌背堆叠）
 */
@Composable
fun AiHandView(
    cardCount: Int,
    label: String,
    cardWidth: Dp = 36.dp,
    isCurrentTurn: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        if (cardCount > 0) {
            Box(contentAlignment = Alignment.Center) {
                val showCount = cardCount.coerceAtMost(3)
                Row {
                    repeat(showCount) {
                        CardBack(width = cardWidth)
                        if (it < showCount - 1) {
                            Spacer(modifier = Modifier.width((-cardWidth * 0.3f)))
                        }
                    }
                }
                Text(
                    text = "${cardCount}张",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        } else {
            Text(
                text = "出完",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 信息芯片
 */
@Composable
fun InfoChip(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
