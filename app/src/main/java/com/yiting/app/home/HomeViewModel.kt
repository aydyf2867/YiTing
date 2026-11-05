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

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val yiTingApp = app as YiTingApplication

    val recentPlays: StateFlow<List<PlayRecordEntity>> =
        yiTingApp.repository.observeRecent()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val readingProgress: StateFlow<List<ReadingProgressEntity>> =
        yiTingApp.repository.observeReadingProgress()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}