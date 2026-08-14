package com.shengji.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库
 */
@Database(
    entities = [GameRecordEntity::class, GameProgressEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameRecordDao(): GameRecordDao
    abstract fun gameProgressDao(): GameProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 从版本1升级到版本2：添加game_progress表
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS game_progress (
                        id INTEGER NOT NULL PRIMARY KEY,
                        phase TEXT NOT NULL,
                        mainSuit TEXT,
                        currentLevel TEXT NOT NULL,
                        bottomCardsJson TEXT NOT NULL,
                        isBottomDug INTEGER NOT NULL,
                        teamScoresJson TEXT NOT NULL,
                        roundNumber INTEGER NOT NULL,
                        playersJson TEXT NOT NULL,
                        currentTurnIndex INTEGER NOT NULL,
                        dealerTeam INTEGER NOT NULL,
                        isLeadPhase INTEGER NOT NULL,
                        currentLeadComboJson TEXT,
                        currentRoundPlaysJson TEXT NOT NULL,
                        lastRoundPlaysJson TEXT NOT NULL,
                        lastRoundWinner INTEGER NOT NULL,
                        isBottomExchangePhase INTEGER NOT NULL,
                        vmMainSuit TEXT,
                        vmCallerIndex INTEGER NOT NULL,
                        vmFirstCallerIndex INTEGER NOT NULL,
                        vmDealerIndex INTEGER NOT NULL,
                        vmTeamScoresJson TEXT NOT NULL,
                        vmLastRoundComboType TEXT NOT NULL,
                        savedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shengji_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
