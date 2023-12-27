package ru.mediaattachments.db

import androidx.room.*
import ru.mediaattachments.db.medianotes.AmplitudesConverter
import ru.mediaattachments.db.medianotes.DbMediaNotes
import ru.mediaattachments.db.medianotes.MediaTypeConverter
import ru.mediaattachments.db.medianotes.MediaNotesDao


@Database(
    entities = [
        DbMediaNotes::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    AmplitudesConverter::class,
    MediaTypeConverter::class
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaNotesDao(): MediaNotesDao
}