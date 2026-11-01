package com.olab.studyqb

import android.app.Application
import com.olab.studyqb.notifications.NotificationChannels

class StudyQBApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensureCreated(this)
    }
}
