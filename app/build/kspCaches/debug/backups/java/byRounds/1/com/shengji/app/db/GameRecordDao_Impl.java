package com.shengji.app.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GameRecordDao_Impl implements GameRecordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GameRecordEntity> __insertionAdapterOfGameRecordEntity;

  private final EntityDeletionOrUpdateAdapter<GameRecordEntity> __deletionAdapterOfGameRecordEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public GameRecordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGameRecordEntity = new EntityInsertionAdapter<GameRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `game_records` (`id`,`startTime`,`endTime`,`winnerTeam`,`finalLevel`,`team0Score`,`team1Score`,`summary`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GameRecordEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStartTime());
        statement.bindLong(3, entity.getEndTime());
        statement.bindLong(4, entity.getWinnerTeam());
        statement.bindString(5, entity.getFinalLevel());
        statement.bindLong(6, entity.getTeam0Score());
        statement.bindLong(7, entity.getTeam1Score());
        statement.bindString(8, entity.getSummary());
      }
    };
    this.__deletionAdapterOfGameRecordEntity = new EntityDeletionOrUpdateAdapter<GameRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `game_records` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GameRecordEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM game_records";
        return _query;
      }
    };
  }

  @Override
  public Object insertRecord(final GameRecordEntity record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfGameRecordEntity.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecord(final GameRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfGameRecordEntity.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GameRecordEntity>> getAllRecords() {
    final String _sql = "SELECT * FROM game_records ORDER BY startTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_records"}, new Callable<List<GameRecordEntity>>() {
      @Override
      @NonNull
      public List<GameRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfWinnerTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "winnerTeam");
          final int _cursorIndexOfFinalLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "finalLevel");
          final int _cursorIndexOfTeam0Score = CursorUtil.getColumnIndexOrThrow(_cursor, "team0Score");
          final int _cursorIndexOfTeam1Score = CursorUtil.getColumnIndexOrThrow(_cursor, "team1Score");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final List<GameRecordEntity> _result = new ArrayList<GameRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final long _tmpEndTime;
            _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            final int _tmpWinnerTeam;
            _tmpWinnerTeam = _cursor.getInt(_cursorIndexOfWinnerTeam);
            final String _tmpFinalLevel;
            _tmpFinalLevel = _cursor.getString(_cursorIndexOfFinalLevel);
            final int _tmpTeam0Score;
            _tmpTeam0Score = _cursor.getInt(_cursorIndexOfTeam0Score);
            final int _tmpTeam1Score;
            _tmpTeam1Score = _cursor.getInt(_cursorIndexOfTeam1Score);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            _item = new GameRecordEntity(_tmpId,_tmpStartTime,_tmpEndTime,_tmpWinnerTeam,_tmpFinalLevel,_tmpTeam0Score,_tmpTeam1Score,_tmpSummary);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<GameRecordEntity>> getRecentRecords(final int limit) {
    final String _sql = "SELECT * FROM game_records ORDER BY startTime DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_records"}, new Callable<List<GameRecordEntity>>() {
      @Override
      @NonNull
      public List<GameRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfWinnerTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "winnerTeam");
          final int _cursorIndexOfFinalLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "finalLevel");
          final int _cursorIndexOfTeam0Score = CursorUtil.getColumnIndexOrThrow(_cursor, "team0Score");
          final int _cursorIndexOfTeam1Score = CursorUtil.getColumnIndexOrThrow(_cursor, "team1Score");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final List<GameRecordEntity> _result = new ArrayList<GameRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final long _tmpEndTime;
            _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            final int _tmpWinnerTeam;
            _tmpWinnerTeam = _cursor.getInt(_cursorIndexOfWinnerTeam);
            final String _tmpFinalLevel;
            _tmpFinalLevel = _cursor.getString(_cursorIndexOfFinalLevel);
            final int _tmpTeam0Score;
            _tmpTeam0Score = _cursor.getInt(_cursorIndexOfTeam0Score);
            final int _tmpTeam1Score;
            _tmpTeam1Score = _cursor.getInt(_cursorIndexOfTeam1Score);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            _item = new GameRecordEntity(_tmpId,_tmpStartTime,_tmpEndTime,_tmpWinnerTeam,_tmpFinalLevel,_tmpTeam0Score,_tmpTeam1Score,_tmpSummary);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRecordById(final long id,
      final Continuation<? super GameRecordEntity> $completion) {
    final String _sql = "SELECT * FROM game_records WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GameRecordEntity>() {
      @Override
      @Nullable
      public GameRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfWinnerTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "winnerTeam");
          final int _cursorIndexOfFinalLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "finalLevel");
          final int _cursorIndexOfTeam0Score = CursorUtil.getColumnIndexOrThrow(_cursor, "team0Score");
          final int _cursorIndexOfTeam1Score = CursorUtil.getColumnIndexOrThrow(_cursor, "team1Score");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final GameRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final long _tmpEndTime;
            _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            final int _tmpWinnerTeam;
            _tmpWinnerTeam = _cursor.getInt(_cursorIndexOfWinnerTeam);
            final String _tmpFinalLevel;
            _tmpFinalLevel = _cursor.getString(_cursorIndexOfFinalLevel);
            final int _tmpTeam0Score;
            _tmpTeam0Score = _cursor.getInt(_cursorIndexOfTeam0Score);
            final int _tmpTeam1Score;
            _tmpTeam1Score = _cursor.getInt(_cursorIndexOfTeam1Score);
            final String _tmpSummary;
            _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            _result = new GameRecordEntity(_tmpId,_tmpStartTime,_tmpEndTime,_tmpWinnerTeam,_tmpFinalLevel,_tmpTeam0Score,_tmpTeam1Score,_tmpSummary);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
