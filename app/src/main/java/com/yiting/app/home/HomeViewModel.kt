package com.yiting.app.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yiting.app.YiTingApplication
import com.yiting.core.data.db.MediaItemEntity
import com.yiting.core.data.db.PlayRecordEntity
import com.yiting.core.data.db.ReadingProgressEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as YiTingApplication).repository

    val recentPlays: StateFlow<List<PlayRecordEntity>> =
        repository.observeRecent()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val readingProgress: StateFlow<List<ReadingProgressEntity>> =
        repository.observeReadingProgress()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val mediaItems: StateFlow<List<MediaItemEntity>> =
        repository.observeMediaItems()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    suspend fun getReadingProgress(documentUri: String): ReadingProgressEntity? =
        repository.getReadingProgress(documentUri)

    fun saveReadingProgress(progress: ReadingProgressEntity) {
        viewModelScope.launch {
            repository.saveReadingProgress(progress)
        }
    }

    fun saveMediaItems(items: List<MediaItemEntity>) {
        viewModelScope.launch {
            repository.replaceLocalMediaItems(items)
        }
    }
}
