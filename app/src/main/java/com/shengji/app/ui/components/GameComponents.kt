package com.shengji.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shengji.app.data.Card
import com.shengji.app.enums.Suit
import com.shengji.app.ui.utils.ScreenSize

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
    val shape = remember { RoundedCornerShape(6.dp) }

    val bgColor = remember(faceUp, selected) {
        when {
            !faceUp -> Color(0xFF1565C0)
            selected -> Color(0xFFFFF9C4)
            else -> Color.White
        }
    }

    val borderStroke = remember(selected) {
        when {
            selected -> BorderStroke(2.dp, Color(0xFFFF6F00))
            else -> BorderStroke(1.dp, Color(0xFFBDBDBD))
        }
    }

    val yOffset by animateDpAsState(
        targetValue = if (selected) (-8).dp else 0.dp,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "cardYOffset"
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
            val textColor = remember { suitColor(card.suit) }
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
                    color = textColor
                )
                Text(
                    text = "王",
                    fontSize = (width.value * 0.22f).sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            // 中心大字
            Text(
                text = "🃏",
                fontSize = (width.value * 0.45f).sp,
                color = textColor.copy(alpha = 0.3f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // 普通牌 - 左上角显示数字和花色
            val textColor = remember(card.suit) { suitColor(card.suit) }
            val symbol = remember(card.suit) { suitSymbol(card.suit) }
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
                    text = symbol,
                    fontSize = (width.value * 0.2f).sp,
                    color = textColor
                )
            }
            // 中心大花色符号
            Text(
                text = symbol,
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

    // 缓存布局计算
    val layoutData = remember(cards.size, cardWidth, maxWidth, overlap) {
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
        Pair(step, totalWidth)
    }

    val (step, totalWidth) = layoutData

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
    isCurrentTurn: Boolean = false,
    screenSize: ScreenSize? = null
) {
    val labelTextSize = screenSize?.labelTextSize ?: MaterialTheme.typography.labelSmall.fontSize
    val chipPaddingH = screenSize?.chipPaddingHorizontal ?: 4.dp
    val chipPaddingV = screenSize?.chipPaddingVertical ?: 1.dp
    val spacingSmall = screenSize?.spacingSmall ?: 2.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = labelTextSize),
            fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacingSmall))
        if (cardCount > 0) {
            Box(contentAlignment = Alignment.Center) {
                val showCount = remember(cardCount) { cardCount.coerceAtMost(3) }
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
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = labelTextSize),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = chipPaddingH, vertical = chipPaddingV)
                )
            }
        } else {
            Text(
                text = "出完",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = labelTextSize),
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
    valueColor: Color = MaterialTheme.colorScheme.primary,
    screenSize: ScreenSize? = null
) {
    val chipPaddingH = screenSize?.chipPaddingHorizontal ?: 10.dp
    val chipPaddingV = screenSize?.chipPaddingVertical ?: 4.dp
    val chipSpacing = screenSize?.chipSpacing ?: 4.dp
    val labelSize = screenSize?.chipLabelSize ?: MaterialTheme.typography.labelSmall.fontSize
    val valueSize = screenSize?.chipValueSize ?: MaterialTheme.typography.labelMedium.fontSize

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = chipPaddingH, vertical = chipPaddingV)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(chipSpacing))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = valueSize),
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
