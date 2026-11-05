package com.yiting.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val source: String,
    val uri: String,
    val durationMs: Long,
    val addedAtMs: Long
)

@Entity(tableName = "play_records")
data class PlayRecordEntity(
    @PrimaryKey val mediaId: String,
    val title: String,
    val category: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAtMs: Long
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: String,
    val title: String,
    val category: String,
    val pinned: Boolean,
    val createdAtMs: Long
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val documentUri: String,
    val title: String,
    val characterOffset: Int,
    val updatedAtMs: Long
)
