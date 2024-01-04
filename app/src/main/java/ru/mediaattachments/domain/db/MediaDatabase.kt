package ru.mediaattachments.domain.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.mediaattachments.data.db.mediaattachment.MediaAttachment
import ru.mediaattachments.domain.converters.AmplitudesConverter
import ru.mediaattachments.domain.converters.MediaTypeConverter
import ru.mediaattachments.domain.db.dao.MediaNotesDao


@Database(
    entities = [
        MediaAttachment::class
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