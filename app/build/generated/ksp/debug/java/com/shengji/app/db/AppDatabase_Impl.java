package com.shengji.app.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile GameRecordDao _gameRecordDao;

  private volatile GameProgressDao _gameProgressDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `game_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `winnerTeam` INTEGER NOT NULL, `finalLevel` TEXT NOT NULL, `team0Score` INTEGER NOT NULL, `team1Score` INTEGER NOT NULL, `summary` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `game_progress` (`id` INTEGER NOT NULL, `phase` TEXT NOT NULL, `mainSuit` TEXT, `currentLevel` TEXT NOT NULL, `bottomCardsJson` TEXT NOT NULL, `isBottomDug` INTEGER NOT NULL, `teamScoresJson` TEXT NOT NULL, `roundNumber` INTEGER NOT NULL, `playersJson` TEXT NOT NULL, `currentTurnIndex` INTEGER NOT NULL, `dealerTeam` INTEGER NOT NULL, `isLeadPhase` INTEGER NOT NULL, `currentLeadComboJson` TEXT, `currentRoundPlaysJson` TEXT NOT NULL, `lastRoundPlaysJson` TEXT NOT NULL, `lastRoundWinner` INTEGER NOT NULL, `isBottomExchangePhase` INTEGER NOT NULL, `vmMainSuit` TEXT, `vmCallerIndex` INTEGER NOT NULL, `vmFirstCallerIndex` INTEGER NOT NULL, `vmDealerIndex` INTEGER NOT NULL, `vmTeamScoresJson` TEXT NOT NULL, `vmLastRoundComboType` TEXT NOT NULL, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '844cff82bcfe8f0285e757d2c5e83d31')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `game_records`");
        db.execSQL("DROP TABLE IF EXISTS `game_progress`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsGameRecords = new HashMap<String, TableInfo.Column>(8);
        _columnsGameRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameRecords.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameRecords.put("endTime", new TableInfo.Column("endTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameRecords.put("winnerTeam", new TableInfo.Column("winnerTeam", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameRecords.put("finalLevel", new TableInfo.Column("finalLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameRecords.put("team0Score", new TableInfo.Column("team0Score", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameRecords.put("team1Score", new TableInfo.Column("team1Score", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameRecords.put("summary", new TableInfo.Column("summary", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGameRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGameRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGameRecords = new TableInfo("game_records", _columnsGameRecords, _foreignKeysGameRecords, _indicesGameRecords);
        final TableInfo _existingGameRecords = TableInfo.read(db, "game_records");
        if (!_infoGameRecords.equals(_existingGameRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "game_records(com.shengji.app.db.GameRecordEntity).\n"
                  + " Expected:\n" + _infoGameRecords + "\n"
                  + " Found:\n" + _existingGameRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsGameProgress = new HashMap<String, TableInfo.Column>(24);
        _columnsGameProgress.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("phase", new TableInfo.Column("phase", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("mainSuit", new TableInfo.Column("mainSuit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("currentLevel", new TableInfo.Column("currentLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("bottomCardsJson", new TableInfo.Column("bottomCardsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("isBottomDug", new TableInfo.Column("isBottomDug", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("teamScoresJson", new TableInfo.Column("teamScoresJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("roundNumber", new TableInfo.Column("roundNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("playersJson", new TableInfo.Column("playersJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("currentTurnIndex", new TableInfo.Column("currentTurnIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("dealerTeam", new TableInfo.Column("dealerTeam", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("isLeadPhase", new TableInfo.Column("isLeadPhase", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("currentLeadComboJson", new TableInfo.Column("currentLeadComboJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("currentRoundPlaysJson", new TableInfo.Column("currentRoundPlaysJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("lastRoundPlaysJson", new TableInfo.Column("lastRoundPlaysJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("lastRoundWinner", new TableInfo.Column("lastRoundWinner", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("isBottomExchangePhase", new TableInfo.Column("isBottomExchangePhase", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("vmMainSuit", new TableInfo.Column("vmMainSuit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("vmCallerIndex", new TableInfo.Column("vmCallerIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("vmFirstCallerIndex", new TableInfo.Column("vmFirstCallerIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("vmDealerIndex", new TableInfo.Column("vmDealerIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("vmTeamScoresJson", new TableInfo.Column("vmTeamScoresJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("vmLastRoundComboType", new TableInfo.Column("vmLastRoundComboType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameProgress.put("savedAt", new TableInfo.Column("savedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGameProgress = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGameProgress = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGameProgress = new TableInfo("game_progress", _columnsGameProgress, _foreignKeysGameProgress, _indicesGameProgress);
        final TableInfo _existingGameProgress = TableInfo.read(db, "game_progress");
        if (!_infoGameProgress.equals(_existingGameProgress)) {
          return new RoomOpenHelper.ValidationResult(false, "game_progress(com.shengji.app.db.GameProgressEntity).\n"
                  + " Expected:\n" + _infoGameProgress + "\n"
                  + " Found:\n" + _existingGameProgress);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "844cff82bcfe8f0285e757d2c5e83d31", "1a9ff69dda85870631096c3300266eea");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "game_records","game_progress");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `game_records`");
      _db.execSQL("DELETE FROM `game_progress`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(GameRecordDao.class, GameRecordDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GameProgressDao.class, GameProgressDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public GameRecordDao gameRecordDao() {
    if (_gameRecordDao != null) {
      return _gameRecordDao;
    } else {
      synchronized(this) {
        if(_gameRecordDao == null) {
          _gameRecordDao = new GameRecordDao_Impl(this);
        }
        return _gameRecordDao;
      }
    }
  }

  @Override
  public GameProgressDao gameProgressDao() {
    if (_gameProgressDao != null) {
      return _gameProgressDao;
    } else {
      synchronized(this) {
        if(_gameProgressDao == null) {
          _gameProgressDao = new GameProgressDao_Impl(this);
        }
        return _gameProgressDao;
      }
    }
  }
}
