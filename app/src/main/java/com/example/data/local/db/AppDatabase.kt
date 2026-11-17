package com.example.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.MatchRecordDao
import com.example.data.local.entity.MatchRecordEntity

@Database(
  entities = [MatchRecordEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun matchRecordDao(): MatchRecordDao
}
