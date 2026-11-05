package com.yiting.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Upsert
    suspend fun upsertAll(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_items ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE category = :category ORDER BY title COLLATE NOCASE")
    fun observeByCategory(category: String): Flow<List<MediaItemEntity>>

    @Query("DELETE FROM media_items WHERE source = :source")
    suspend fun deleteBySource(source: String)
}

@Dao
interface PlayRecordDao {
    @Upsert
    suspend fun upsert(record: PlayRecordEntity)

    @Query("SELECT * FROM play_records ORDER BY lastPlayedAtMs DESC LIMIT 20")
    fun observeRecent(): Flow<List<PlayRecordEntity>>

    @Query("SELECT * FROM play_records WHERE mediaId = :mediaId")
    suspend fun getById(mediaId: String): PlayRecordEntity?

    @Query("DELETE FROM play_records")
    suspend fun clear()
}

@Dao
interface FavoriteDao {
    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: String)

    @Query("SELECT * FROM favorites WHERE mediaId = :mediaId")
    suspend fun getById(mediaId: String): FavoriteEntity?

    @Query("SELECT * FROM favorites ORDER BY pinned DESC, createdAtMs DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("DELETE FROM favorites")
    suspend fun clear()
}

@Dao
interface ReadingProgressDao {
    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE documentUri = :documentUri")
    suspend fun getByDocument(documentUri: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<ReadingProgressEntity>>

    @Query("DELETE FROM reading_progress WHERE documentUri = :documentUri")
    suspend fun delete(documentUri: String)
}
