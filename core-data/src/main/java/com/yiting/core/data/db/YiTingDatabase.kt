package com.yiting.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MediaItemEntity::class,
        PlayRecordEntity::class,
        FavoriteEntity::class,
        ReadingProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class YiTingDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playRecordDao(): PlayRecordDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `reading_progress` (
                        `documentUri` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `characterOffset` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`documentUri`)
                    )""".trimIndent()
                )
            }
        }

        @Volatile
        private var instance: YiTingDatabase? = null

        fun get(context: Context): YiTingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    YiTingDatabase::class.java,
                    "yiting.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
