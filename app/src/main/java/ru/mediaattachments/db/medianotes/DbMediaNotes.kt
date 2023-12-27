package ru.mediaattachments.db.medianotes

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.scheduled.mediaattachmentslibrary.data.MediaItemType

@Entity(tableName = "mediaNotes")
data class DbMediaNotes(
    @PrimaryKey
    val id: String,
    var value: String,
    @TypeConverters(MediaTypeConverter::class)
    val mediaType: MediaItemType,
    val order: Long,
    var recognizedSpeechText: String = "",
    var imageNoteText: String = "",
    @TypeConverters(AmplitudesConverter::class)
    val voiceAmplitudesList: List<Int>? = listOf(),
    var uploadPercent: Int,
    var downloadPercent: Int
)
