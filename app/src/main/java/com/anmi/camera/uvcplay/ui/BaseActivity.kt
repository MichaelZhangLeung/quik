package com.anmi.camera.uvcplay.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.anmi.camera.uvcplay.locale.LocaleHelper

/**
 * 通用 BaseActivity
 *
 * 使用方式：
 * class UvcMainActivity : BaseActivity(R.layout.activity_main), SurfaceHolder.Callback, ServiceConnection, IServiceControlInterface { ... }
 *
 * Hilt 注意：
 * - 如果子类需要注入，请在子类上加 @AndroidEntryPoint（不要只依赖在 BaseActivity 上注解）。
 */
open class BaseActivity(@LayoutRes contentLayoutId: Int) : AppCompatActivity(contentLayoutId) {

    private val localeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 默认行为：重建 Activity。子类可 override onLocaleChanged() 控制行为
            onLocaleChanged()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // 统一从 LocaleHelper 读取保存的语言并包裹 context
        val savedLang = LocaleHelper.getSavedLanguage(newBase)
        val wrapped = LocaleHelper.wrapContext(newBase, savedLang)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 注册语言变更广播
        registerReceiver(localeReceiver, IntentFilter("LocaleHelper.ACTION_LOCALE_CHANGED"))
    }

    override fun onDestroy() {
        // 解除注册，避免泄露
        try {
            unregisterReceiver(localeReceiver)
        } catch (e: Exception) {
            // ignore
        }
        super.onDestroy()
    }

    /**
     * 当收到语言切换广播时调用。默认实现是重建当前 Activity。
     * 如果希望其他行为（如重启到主界面或不重建），请在子类 override。
     */
    protected open fun onLocaleChanged() {
        // 防止在不适合的时机重复 recreate 导致问题，可在子类加判断
        recreate()
    }

    /**
     * 可供子类/Settings 调用的便捷方法：清空任务栈并启动主 Activity
     */
    protected fun restartToMain() {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * 最彻底的重启：使用 Alarm + PendingIntent 后 killProcess。
     * 注意：体验上会重启整个应用，谨慎使用。
     */
    protected fun restartAppAndKillProcess() {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            android.app.PendingIntent.FLAG_CANCEL_CURRENT
        }
        val pending = android.app.PendingIntent.getActivity(this, 0, intent, pendingFlags)
        val am = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        am.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 150, pending)
        android.os.Process.killProcess(android.os.Process.myPid())
        kotlin.system.exitProcess(0)
    }
}
