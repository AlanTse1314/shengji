package com.shengji.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 游戏进度数据访问对象
 */
@Dao
interface GameProgressDao {

    /**
     * 获取保存的游戏进度（只有一条记录）
     */
    @Query("SELECT * FROM game_progress WHERE id = 1")
    suspend fun getProgress(): GameProgressEntity?

    /**
     * 保存游戏进度（覆盖更新）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: GameProgressEntity)

    /**
     * 删除游戏进度
     */
    @Query("DELETE FROM game_progress WHERE id = 1")
    suspend fun clearProgress()

    /**
     * 检查是否有保存的进度
     */
    @Query("SELECT COUNT(*) FROM game_progress WHERE id = 1")
    suspend fun hasProgress(): Int
}
