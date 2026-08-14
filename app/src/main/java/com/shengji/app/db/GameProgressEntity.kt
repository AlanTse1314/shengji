package com.shengji.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 游戏进度实体
 *
 * 用于保存当前游戏状态，支持退出后继续游戏。
 * 只保存一条记录（id=1），每次保存覆盖更新。
 */
@Entity(tableName = "game_progress")
data class GameProgressEntity(
    @PrimaryKey
    val id: Int = 1,  // 固定ID，只保存一条记录

    /** 游戏阶段 */
    val phase: String,

    /** 主花色 */
    val mainSuit: String?,

    /** 当前级牌 */
    val currentLevel: String,

    /** 底牌（JSON序列化的deckIndex列表） */
    val bottomCardsJson: String,

    /** 是否已抠底 */
    val isBottomDug: Boolean,

    /** 各队得分（JSON数组） */
    val teamScoresJson: String,

    /** 当前轮次 */
    val roundNumber: Int,

    /** 四位玩家（JSON序列化） */
    val playersJson: String,

    /** 当前出牌玩家索引 */
    val currentTurnIndex: Int,

    /** 庄家队伍 */
    val dealerTeam: Int,

    /** 是否首出阶段 */
    val isLeadPhase: Boolean,

    /** 当前轮首出牌型（JSON） */
    val currentLeadComboJson: String?,

    /** 当前轮出牌记录（JSON） */
    val currentRoundPlaysJson: String,

    /** 上一轮出牌记录（JSON） */
    val lastRoundPlaysJson: String,

    /** 上一轮赢家 */
    val lastRoundWinner: Int,

    /** 是否换底牌阶段 */
    val isBottomExchangePhase: Boolean,

    /** ViewModel内部状态：当前主花色 */
    val vmMainSuit: String?,

    /** ViewModel内部状态：当前叫主者索引 */
    val vmCallerIndex: Int,

    /** ViewModel内部状态：第一个亮牌者 */
    val vmFirstCallerIndex: Int,

    /** ViewModel内部状态：当前庄家索引 */
    val vmDealerIndex: Int,

    /** ViewModel内部状态：队伍得分 */
    val vmTeamScoresJson: String,

    /** ViewModel内部状态：上一轮牌型 */
    val vmLastRoundComboType: String,

    /** 保存时间 */
    val savedAt: Long = System.currentTimeMillis()
)
