package ru.leadfrog.db

import androidx.room.*
import ru.leadfrog.db.media_uris.AmplitudesConverter
import ru.leadfrog.db.media_uris.MediaUrisDao
import ru.leadfrog.db.media_uris.DbMediaNotes


@Database(
        entities = [
            DbMediaNotes::class
        ],
        version = 1,
        exportSchema = true
)
@TypeConverters(
    AmplitudesConverter::class
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaUrisDao(): MediaUrisDao
}