package com.yiting.app.home

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yiting.app.YiTingApplication
import com.yiting.core.data.db.PlayRecordEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(app: YiTingApplication) : AndroidViewModel(app) {

    val recentPlays: StateFlow<List<PlayRecordEntity>> =
        app.repository.observeRecent()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
