package ru.mediaattachments.db.medianotes

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class AmplitudesConverter {
    @TypeConverter
    fun fromAmplitudes(ampl: List<Int>?): String {
        if (ampl == null) return ""
        return Gson().toJson(ampl)
    }

    @TypeConverter
    fun toAmplitudes(ampl: String): List<Int>? {
        val type: Type = object : TypeToken<List<Int>?>() {}.type
        if (ampl == "") return null
        return Gson().fromJson(ampl, type)
    }
}