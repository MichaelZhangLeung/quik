package dev.alejandrorosas.apptemplate

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.pedro.rtplibrary.view.OpenGlView
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AndroidApplication : Application(){

    companion object {

        @SuppressLint("StaticFieldLeak")
        var app: Context? = null
    }
    override fun onCreate() {
        super.onCreate()
        app = this
    }
}
