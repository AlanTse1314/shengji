package com.shengji.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 屏幕尺寸工具类 - 统一管理手机/平板适配
 *
 * 设备分类：
 * - 小屏手机: width < 360dp (如 iPhone SE)
 * - 普通手机: 360dp <= width < 480dp (如 iPhone 14)
 * - 大屏手机: 480dp <= width < 600dp (如 iPhone 14 Pro Max)
 * - 小平板: 600dp <= width < 800dp (如 iPad Mini)
 * - 大平板: width >= 800dp (如 iPad Pro)
 */
data class ScreenSize(
    val screenWidth: Dp,
    val screenHeight: Dp,
    val density: Float
) {
    // 设备类型判断
    val isSmallPhone: Boolean get() = screenWidth < 360.dp
    val isNormalPhone: Boolean get() = screenWidth in 360.dp..479.dp
    val isLargePhone: Boolean get() = screenWidth in 480.dp..599.dp
    val isSmallTablet: Boolean get() = screenWidth in 600.dp..799.dp
    val isLargeTablet: Boolean get() = screenWidth >= 800.dp
    val isTablet: Boolean get() = screenWidth >= 600.dp

    // ═══ 卡牌尺寸 ═══

    /** 人类玩家手牌宽度 */
    val cardWidth: Dp = when {
        isSmallPhone -> 36.dp
        isNormalPhone -> 44.dp
        isLargePhone -> 52.dp
        isSmallTablet -> 60.dp
        else -> 68.dp
    }

    /** 出牌区卡牌宽度 */
    val playAreaCardWidth: Dp = when {
        isSmallPhone -> 30.dp
        isNormalPhone -> 36.dp
        isLargePhone -> 42.dp
        isSmallTablet -> 48.dp
        else -> 54.dp
    }

    /** AI手牌（牌背）宽度 */
    val aiCardWidth: Dp = when {
        isSmallPhone -> 24.dp
        isNormalPhone -> 28.dp
        isLargePhone -> 32.dp
        isSmallTablet -> 36.dp
        else -> 40.dp
    }

    /** 底牌宽度 */
    val bottomCardWidth: Dp = when {
        isSmallPhone -> 28.dp
        isNormalPhone -> 32.dp
        isLargePhone -> 38.dp
        isSmallTablet -> 44.dp
        else -> 50.dp
    }

    /** 换底牌区卡牌宽度 */
    val exchangeCardWidth: Dp = when {
        isSmallPhone -> 30.dp
        isNormalPhone -> 36.dp
        isLargePhone -> 42.dp
        isSmallTablet -> 48.dp
        else -> 54.dp
    }

    /** 飞牌动画卡牌宽度 */
    val flyingCardWidth: Dp = when {
        isSmallPhone -> 34.dp
        isNormalPhone -> 40.dp
        isLargePhone -> 46.dp
        isSmallTablet -> 52.dp
        else -> 58.dp
    }

    /** 中央牌堆卡牌宽度 */
    val centralDeckCardWidth: Dp = when {
        isSmallPhone -> 32.dp
        isNormalPhone -> 38.dp
        isLargePhone -> 44.dp
        isSmallTablet -> 50.dp
        else -> 56.dp
    }

    /** 亮主展示卡牌宽度 */
    val callMainCardWidth: Dp = when {
        isSmallPhone -> 36.dp
        isNormalPhone -> 42.dp
        isLargePhone -> 48.dp
        isSmallTablet -> 54.dp
        else -> 60.dp
    }

    // ═══ 手牌重叠 ═══

    /** 手牌重叠比例 */
    val cardOverlapRatio: Float = when {
        isSmallPhone -> 0.45f
        isNormalPhone -> 0.5f
        else -> 0.5f
    }

    // ═══ 按钮尺寸 ═══

    /** 主按钮高度 */
    val buttonHeightLarge: Dp = when {
        isSmallPhone -> 44.dp
        isNormalPhone -> 48.dp
        isLargePhone -> 52.dp
        isSmallTablet -> 56.dp
        else -> 60.dp
    }

    /** 普通按钮高度 */
    val buttonHeightMedium: Dp = when {
        isSmallPhone -> 36.dp
        isNormalPhone -> 40.dp
        isLargePhone -> 44.dp
        isSmallTablet -> 48.dp
        else -> 52.dp
    }

    /** 小按钮高度 */
    val buttonHeightSmall: Dp = when {
        isSmallPhone -> 30.dp
        isNormalPhone -> 34.dp
        isLargePhone -> 38.dp
        isSmallTablet -> 42.dp
        else -> 46.dp
    }

    /** 按钮内边距 - 水平 */
    val buttonPaddingHorizontal: Dp = when {
        isSmallPhone -> 10.dp
        isNormalPhone -> 12.dp
        isLargePhone -> 14.dp
        isSmallTablet -> 16.dp
        else -> 18.dp
    }

    /** 按钮内边距 - 垂直 */
    val buttonPaddingVertical: Dp = when {
        isSmallPhone -> 4.dp
        isNormalPhone -> 6.dp
        isLargePhone -> 6.dp
        isSmallTablet -> 8.dp
        else -> 8.dp
    }

    // ═══ 文字尺寸 ═══

    /** 状态栏文字大小 */
    val statusTextSize: TextUnit = when {
        isSmallPhone -> 10.sp
        isNormalPhone -> 12.sp
        isLargePhone -> 13.sp
        isSmallTablet -> 14.sp
        else -> 15.sp
    }

    /** 按钮文字大小 */
    val buttonTextSize: TextUnit = when {
        isSmallPhone -> 13.sp
        isNormalPhone -> 14.sp
        isLargePhone -> 15.sp
        isSmallTablet -> 16.sp
        else -> 17.sp
    }

    /** 标题文字大小 */
    val titleTextSize: TextUnit = when {
        isSmallPhone -> 15.sp
        isNormalPhone -> 16.sp
        isLargePhone -> 18.sp
        isSmallTablet -> 20.sp
        else -> 22.sp
    }

    /** 标签文字大小 */
    val labelTextSize: TextUnit = when {
        isSmallPhone -> 9.sp
        isNormalPhone -> 10.sp
        isLargePhone -> 11.sp
        isSmallTablet -> 12.sp
        else -> 13.sp
    }

    /** 分数文字大小 */
    val scoreTextSize: TextUnit = when {
        isSmallPhone -> 11.sp
        isNormalPhone -> 12.sp
        isLargePhone -> 13.sp
        isSmallTablet -> 14.sp
        else -> 15.sp
    }

    // ═══ 间距 ═══

    /** 通用小间距 */
    val spacingSmall: Dp = when {
        isSmallPhone -> 2.dp
        isNormalPhone -> 4.dp
        else -> 4.dp
    }

    /** 通用中间距 */
    val spacingMedium: Dp = when {
        isSmallPhone -> 4.dp
        isNormalPhone -> 6.dp
        isLargePhone -> 8.dp
        isSmallTablet -> 8.dp
        else -> 10.dp
    }

    /** 通用大间距 */
    val spacingLarge: Dp = when {
        isSmallPhone -> 8.dp
        isNormalPhone -> 12.dp
        isLargePhone -> 12.dp
        isSmallTablet -> 16.dp
        else -> 16.dp
    }

    // ═══ AI手牌区域 ═══

    /** AI手牌区域宽度（左右） */
    val aiHandAreaWidth: Dp = when {
        isSmallPhone -> 50.dp
        isNormalPhone -> 60.dp
        isLargePhone -> 70.dp
        isSmallTablet -> 80.dp
        else -> 90.dp
    }

    /** 出牌区中间信息宽度 */
    val playAreaInfoWidth: Dp = when {
        isSmallPhone -> 40.dp
        isNormalPhone -> 50.dp
        isLargePhone -> 55.dp
        isSmallTablet -> 60.dp
        else -> 70.dp
    }

    // ═══ 分数显示 ═══

    /** 闲家分数显示内边距 */
    val scorePaddingHorizontal: Dp = when {
        isSmallPhone -> 8.dp
        isNormalPhone -> 10.dp
        isLargePhone -> 12.dp
        isSmallTablet -> 14.dp
        else -> 16.dp
    }

    val scorePaddingVertical: Dp = when {
        isSmallPhone -> 3.dp
        isNormalPhone -> 4.dp
        isLargePhone -> 5.dp
        isSmallTablet -> 6.dp
        else -> 6.dp
    }

    // ═══ 信息芯片 ═══

    /** InfoChip 内边距 */
    val chipPaddingHorizontal: Dp = when {
        isSmallPhone -> 8.dp
        isNormalPhone -> 10.dp
        isLargePhone -> 10.dp
        isSmallTablet -> 12.dp
        else -> 12.dp
    }

    val chipPaddingVertical: Dp = when {
        isSmallPhone -> 3.dp
        isNormalPhone -> 4.dp
        isLargePhone -> 4.dp
        isSmallTablet -> 5.dp
        else -> 5.dp
    }

    val chipSpacing: Dp = when {
        isSmallPhone -> 3.dp
        isNormalPhone -> 4.dp
        else -> 4.dp
    }

    val chipLabelSize: TextUnit = when {
        isSmallPhone -> 9.sp
        isNormalPhone -> 10.sp
        isLargePhone -> 11.sp
        isSmallTablet -> 12.sp
        else -> 12.sp
    }

    val chipValueSize: TextUnit = when {
        isSmallPhone -> 11.sp
        isNormalPhone -> 12.sp
        isLargePhone -> 13.sp
        isSmallTablet -> 14.sp
        else -> 14.sp
    }
}

/**
 * 获取当前屏幕尺寸配置
 */
@Composable
fun rememberScreenSize(): ScreenSize {
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    return remember(config.screenWidthDp, config.screenHeightDp) {
        ScreenSize(
            screenWidth = config.screenWidthDp.dp,
            screenHeight = config.screenHeightDp.dp,
            density = density.density
        )
    }
}
