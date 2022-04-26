package ru.leadfrog

//import com.melegy.redscreenofdeath.RedScreenOfDeath
import androidx.multidex.MultiDexApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import ru.leadfrog.di.viewModelsModule
import ru.leadfrog.di.appModule

class MyApp : MultiDexApplication() {


    override fun onCreate() {
        super.onCreate()

        startKoin {
            printLogger(level = Level.ERROR)
            androidContext(this@MyApp)
            modules(listOf(appModule, viewModelsModule))
        }

        }
}