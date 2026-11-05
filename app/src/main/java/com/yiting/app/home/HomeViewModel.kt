package com.yiting.app.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yiting.app.YiTingApplication
import com.yiting.core.data.db.PlayRecordEntity
import com.yiting.core.data.db.ReadingProgressEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as YiTingApplication).repository

    val recentPlays: StateFlow<List<PlayRecordEntity>> = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val readingProgress: StateFlow<List<ReadingProgressEntity>> = repository.observeReadingProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun getReadingProgress(uri: String): ReadingProgressEntity? =
        repository.getReadingProgress(uri)

    fun saveReadingProgress(progress: ReadingProgressEntity) {
        viewModelScope.launch { repository.saveReadingProgress(progress) }
    }
}
