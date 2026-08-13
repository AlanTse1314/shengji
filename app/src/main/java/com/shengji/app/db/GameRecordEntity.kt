package com.shengji.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 历史对局记录实体
 *
 * 用于 Room 本地存储，记录每局游戏的结果。
 */
@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 对局开始时间（毫秒时间戳） */
    val startTime: Long,

    /** 对局结束时间（毫秒时间戳） */
    val endTime: Long,

    /** 获胜队伍索引 */
    val winnerTeam: Int,

    /** 庄家最终级别 */
    val finalLevel: String,

    /** 队伍 0 总分 */
    val team0Score: Int,

    /** 队伍 1 总分 */
    val team1Score: Int,

    /** 对局摘要 / 备注 */
    val summary: String = ""
)
