package com.anmi.camera.uvcplay.api

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import com.anmi.camera.uvcplay.locale.LocaleHelper
import com.anmi.camera.uvcplay.utils.AppUncaughtExceptionHandler
import com.base.MyLog
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import com.tencent.smtt.sdk.TbsListener
import dev.alejandrorosas.streamlib.UsbUtils
import com.anmi.camera.uvcplay.manager.BroadcastManager
import dev.alejandrorosas.apptemplate.UvcMainActivity
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean


object VisitLiveModule {

    var appRef: WeakReference<Context>? = null
    private const val TAG = "[VisitLiveModule]"

    private val initialized = AtomicBoolean(false)

    fun startVisit(context: Context, params: VisitParams){
        try {
            val intent = UvcMainActivity.createIntent(context, params)
            if (context !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            MyLog.e("[VisitLiveModule] failed to start visit: ${e.message}")
            throw e
        }
    }

    fun getApp(): Context? {
        return appRef?.get()
    }

    @SuppressLint("NewApi")
    fun init(context: Context) {

        if (initialized.get()) {
            MyLog.e("$TAG already initialized, skip......")
            return
        }

        appRef = WeakReference(context.applicationContext)

        initialized.set(true)

        AppUncaughtExceptionHandler.getInstance().init()
        val savedLanguage = LocaleHelper.getSavedLanguage(context)
        val wrapped = LocaleHelper.wrapContext(context, savedLanguage)

        MyLog.e("#onCreate savedLanguage:${LocaleHelper.getSavedLanguage(context)}")

        UsbUtils.setContext(context)

        if (Application.getProcessName().equals(context.packageName)) {
            BroadcastManager.getInstance().init(context)
        }

        initX5(context)

        MyLog.d("$TAG initialized success")
    }
   private fun initX5(context: Context) {
        QbSdk.setDownloadWithoutWifi(true)
        QbSdk.setTbsListener(
            object : TbsListener {
                /**
                 * @param stateCode 用户可处理错误码请参考[com.tencent.smtt.sdk.TbsCommonCode]
                 */
                override fun onDownloadFinish(stateCode: Int) {
                    MyLog.e(TAG +"onDownloadFinished: $stateCode")
                }

                /**
                 * @param stateCode 用户可处理错误码请参考[com.tencent.smtt.sdk.TbsCommonCode]
                 */
                override fun onInstallFinish(stateCode: Int) {
                    MyLog.e(TAG +"onInstallFinished: $stateCode")
                }

                /**
                 * 首次安装应用，会触发内核下载，此时会有内核下载的进度回调。
                 * @param progress 0 - 100
                 */
                override fun onDownloadProgress(progress: Int) {
                    MyLog.e(TAG +"Core Downloading: $progress")
                }
            },
        )

        // 在调用TBS初始化、创建WebView之前进行如下配置
        QbSdk.initTbsSettings(
            hashMapOf(
                TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER to true,
                TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE to true,
            ) as Map<String, Any>?,
        )

        // 初始化X5内核
        QbSdk.initX5Environment(
            context,
            object : PreInitCallback {
                override fun onCoreInitFinished() {
                    // 内核初始化完成，可能为系统内核，也可能为系统内核
                    MyLog.e("onCoreInitFinished")
                }
                /**
                 * 预初始化结束
                 * 由于X5内核体积较大，需要依赖wifi网络下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
                 * 内核下发请求发起有24小时间隔，卸载重装、调整系统时间24小时后都可重置
                 * 调试阶段建议通过 WebView 访问 debugtbs.qq.com -> 安装线上内核 解决
                 * @param isX5 是否使用X5内核
                 */
                override fun onViewInitFinished(isX5: Boolean) {
                    MyLog.e("isX5 success:$isX5")/* 内核加载成功与否 */
                }
            },
        )
    }
}
