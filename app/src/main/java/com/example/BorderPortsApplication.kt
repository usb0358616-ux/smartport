package com.example

import android.app.Application
import com.example.di.appModule
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class BorderPortsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@BorderPortsApplication)
                modules(appModule)
            }
        }
    }
}
