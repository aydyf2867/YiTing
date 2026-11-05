package com.yiting.core.data

import com.yiting.core.data.db.FavoriteEntity
import com.yiting.core.data.db.PlayRecordEntity
import com.yiting.core.data.db.ReadingProgressEntity
import com.yiting.core.data.db.YiTingDatabase
import kotlinx.coroutines.flow.Flow

class PlayRepository(private val db: YiTingDatabase) {

    fun observeRecent(): Flow<List<PlayRecordEntity>> =
        db.playRecordDao().observeRecent()

    fun observeFavorites(): Flow<List<FavoriteEntity>> =
        db.favoriteDao().observeAll()

    fun observeReadingProgress(): Flow<List<ReadingProgressEntity>> =
        db.readingProgressDao().observeAll()

    suspend fun getReadingProgress(documentUri: String): ReadingProgressEntity? =
        db.readingProgressDao().getByDocument(documentUri)

    suspend fun saveReadingProgress(progress: ReadingProgressEntity) =
        db.readingProgressDao().upsert(progress)

    suspend fun isFavorite(mediaId: String): Boolean =
        db.favoriteDao().getById(mediaId) != null

    suspend fun toggleFavorite(mediaId: String, title: String, category: String) {
        val existing = db.favoriteDao().getById(mediaId)
        if (existing == null) {
            db.favoriteDao().upsert(
                FavoriteEntity(
                    mediaId = mediaId,
                    title = title,
                    category = category,
                    pinned = false,
                    createdAtMs = System.currentTimeMillis()
                )
            )
        } else {
            db.favoriteDao().delete(mediaId)
        }
    }

    suspend fun savePlayRecord(record: PlayRecordEntity) {
        db.playRecordDao().upsert(record)
    }
}
