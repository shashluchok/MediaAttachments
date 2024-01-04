package ru.mediaattachments.domain.converters

import androidx.room.TypeConverter
import ru.mediaattachments.data.db.mediaattachment.MediaType

class MediaTypeConverter {

    @TypeConverter
    fun toMediaType(value: String) = enumValueOf<MediaType>(value)

    @TypeConverter
    fun fromMediaType(value: MediaType) = value.name
}