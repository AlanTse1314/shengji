package com.shengji.app.ui.navigation

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    /** 主页 */
    data object Home : Screen("home")

    /** 联机大厅 */
    data object Multiplayer : Screen("multiplayer")

    /** 历史记录 */
    data object History : Screen("history")

    /** 游戏主界面（单机） */
    data object Game : Screen("game")

    /** 游戏主界面（联机） */
    data object NetworkGame : Screen("network_game")
}
