package com.shengji.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 对局记录数据访问对象
 */
@Dao
interface GameRecordDao {

    @Query("SELECT * FROM game_records ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records ORDER BY startTime DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records WHERE id = :id")
    suspend fun getRecordById(id: Long): GameRecordEntity?

    @Insert
    suspend fun insertRecord(record: GameRecordEntity): Long

    @Delete
    suspend fun deleteRecord(record: GameRecordEntity)

    @Query("DELETE FROM game_records")
    suspend fun clearAll()
}
