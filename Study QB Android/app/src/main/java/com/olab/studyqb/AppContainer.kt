package com.olab.studyqb

import android.content.Context
import com.olab.studyqb.data.db.StudyQBDatabase
import com.olab.studyqb.data.local.CachedClueDataSource
import com.olab.studyqb.data.network.NetworkModule
import com.olab.studyqb.data.network.groq.GroqModelRotator
import com.olab.studyqb.data.repository.AnalyzeRepository
import com.olab.studyqb.data.repository.CardRepository
import com.olab.studyqb.data.repository.SettingsRepository
import com.olab.studyqb.data.repository.StatsRepository
import com.olab.studyqb.data.repository.TossupRepository

/** Hand-rolled service locator (no Hilt/Koin — the app is small enough not to need one). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: StudyQBDatabase = StudyQBDatabase.getInstance(context)

    val cardRepository: CardRepository by lazy { CardRepository(database.cardDao()) }
    val statsRepository: StatsRepository by lazy { StatsRepository(database.buzzGameStatDao(), database.cardStatDao()) }

    val analyzeRepository: AnalyzeRepository by lazy {
        AnalyzeRepository(
            NetworkModule.qbReaderApi,
            NetworkModule.groqApi,
            GroqModelRotator(),
            CachedClueDataSource(appContext)
        )
    }
    val tossupRepository: TossupRepository by lazy {
        TossupRepository(NetworkModule.groqApi, cardRepository)
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
}
