package ru.mediaattachments

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import ru.mediaattachments.domain.di.appModule
import ru.mediaattachments.domain.di.viewModelsModule

class MediaAttachmentsApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            printLogger(level = Level.ERROR)
            androidContext(this@MediaAttachmentsApp)
            modules(listOf(appModule, viewModelsModule))
        }

    }
}