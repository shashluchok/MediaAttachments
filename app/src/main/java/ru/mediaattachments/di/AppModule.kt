package ru.mediaattachments.di


import androidx.room.Room
import org.koin.dsl.module
import ru.mediaattachments.db.MediaDatabase
import ru.mediaattachments.db.medianotes.MediaNotesRepository

val appModule = module {

    single {
        val db = Room.databaseBuilder(
            get(),
            MediaDatabase::class.java, "MediaAttachments.db"
        ).build()
        db
    }

    single { MediaNotesRepository(get()) }

}

