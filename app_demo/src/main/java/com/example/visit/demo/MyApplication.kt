package com.example.visit.demo

import android.app.Application
import android.util.Log
import com.anmi.camera.uvcplay.api.VisitLiveModule
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 1. 这里进行全局初始化
        initSDK()
        
        Log.d("MyApplication", "Application onCreate: 全局初始化完成")
    }

    private fun initSDK() {
        // 在这里调用你 visitLiveLib AAR 里的初始化方法
        // 例如：VisitLiveSDK.init(this)
        VisitLiveModule.init(this)
    }
}
