package com.yiting.app

import android.app.Application
import com.yiting.core.data.PlayRepository
import com.yiting.core.data.db.YiTingDatabase

class YiTingApplication : Application() {

    lateinit var repository: PlayRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = PlayRepository(YiTingDatabase.get(this))
    }
}
