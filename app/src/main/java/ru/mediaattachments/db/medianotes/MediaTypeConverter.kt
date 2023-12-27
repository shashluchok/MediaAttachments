package ru.mediaattachments.db.medianotes

import androidx.room.TypeConverter
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType

class MediaTypeConverter {

    @TypeConverter
    fun toMediaType(value: String) = enumValueOf<MediaItemType>(value)

    @TypeConverter
    fun fromMediaType(value: MediaItemType) = value.name
}