package com.olab.studyqb.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.olab.studyqb.data.db.dao.BuzzGameStatDao
import com.olab.studyqb.data.db.dao.CardDao
import com.olab.studyqb.data.db.dao.CardStatDao
import com.olab.studyqb.data.db.entities.BuzzGameStatEntity
import com.olab.studyqb.data.db.entities.CardEntity
import com.olab.studyqb.data.db.entities.CardStatEntity

@Database(
    entities = [CardEntity::class, BuzzGameStatEntity::class, CardStatEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyQBDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun buzzGameStatDao(): BuzzGameStatDao
    abstract fun cardStatDao(): CardStatDao

    companion object {
        @Volatile
        private var instance: StudyQBDatabase? = null

        fun getInstance(context: Context): StudyQBDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyQBDatabase::class.java,
                    "study_qb.db"
                ).build().also { instance = it }
            }
    }
}
