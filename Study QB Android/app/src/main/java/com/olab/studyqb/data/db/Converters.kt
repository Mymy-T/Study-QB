package com.olab.studyqb.data.db

import android.util.Log
import androidx.room.TypeConverter
import com.olab.studyqb.data.fsrs.FsrsState
import com.olab.studyqb.domain.Category
import java.time.Instant

class Converters {

    @TypeConverter
    fun instantToEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(millis: Long?): Instant? = millis?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun categoryToString(category: Category): String = category.name

    @TypeConverter
    fun stringToCategory(value: String): Category =
        Category.entries.firstOrNull { it.name == value } ?: run {
            Log.w("Converters", "Unrecognized Category \"$value\" in DB, falling back to LITERATURE")
            Category.LITERATURE
        }

    @TypeConverter
    fun fsrsStateToString(state: FsrsState): String = state.name

    @TypeConverter
    fun stringToFsrsState(value: String): FsrsState =
        FsrsState.entries.firstOrNull { it.name == value } ?: run {
            Log.w("Converters", "Unrecognized FsrsState \"$value\" in DB, falling back to NEW")
            FsrsState.NEW
        }
}
