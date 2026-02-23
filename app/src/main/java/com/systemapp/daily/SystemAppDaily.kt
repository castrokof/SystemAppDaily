package com.systemapp.daily

import android.app.Application

class SystemAppDaily : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SystemAppDaily
            private set
    }
}
