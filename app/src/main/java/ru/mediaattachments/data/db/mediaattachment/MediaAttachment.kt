package ru.mediaattachments.data.db.mediaattachment

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.mediaattachments.domain.converters.AmplitudesConverter
import ru.mediaattachments.domain.converters.MediaTypeConverter

@Entity(tableName = "mediaNotes")
data class MediaAttachment(
    @PrimaryKey
    val id: String,
    var value: String,
    @TypeConverters(MediaTypeConverter::class)
    val mediaType: MediaType,
    val order: Long,
    var imageNoteText: String = "",
    @TypeConverters(AmplitudesConverter::class)
    val voiceAmplitudesList: List<Int>? = listOf(),
    var uploadPercent: Int,
    var downloadPercent: Int
)
