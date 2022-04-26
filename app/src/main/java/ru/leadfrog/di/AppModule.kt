package ru.leadfrog.di


import androidx.room.Room
import org.koin.dsl.module
import ru.leadfrog.db.media_uris.MediaNotesRepository
import ru.leadfrog.db.MediaDatabase

val appModule = module {

    single<MediaDatabase> {
        Room.databaseBuilder(get(), MediaDatabase::class.java, "LFS.db")
        .build()
    }

    single<MediaNotesRepository> { MediaNotesRepository(get()) }

}

