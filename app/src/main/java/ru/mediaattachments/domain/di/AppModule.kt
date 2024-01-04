package ru.mediaattachments.domain.di


import androidx.room.Room
import org.koin.dsl.module
import ru.mediaattachments.domain.db.MediaDatabase
import ru.mediaattachments.domain.mediaattachments.MediaNotesRepository

private const val databaseName = "MediaAttachments.db"

val appModule = module {

    single {
        val db = Room.databaseBuilder(
            get(),
            MediaDatabase::class.java, databaseName
        ).build()
        db
    }

    single { MediaNotesRepository(get()) }

}

