package com.systemapp.daily

import android.app.Application
import com.systemapp.daily.data.sync.SyncWorker

class SystemAppDaily : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Programar sincronización periódica cada 15 min
        SyncWorker.programarSincronizacionPeriodica(this)
    }

    companion object {
        lateinit var instance: SystemAppDaily
            private set
    }
}
