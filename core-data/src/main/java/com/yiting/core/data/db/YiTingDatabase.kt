package com.yiting.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaItemEntity::class, PlayRecordEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class YiTingDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playRecordDao(): PlayRecordDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var instance: YiTingDatabase? = null

        fun get(context: Context): YiTingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    YiTingDatabase::class.java,
                    "yiting.db"
                ).build().also { instance = it }
            }
    }
}
