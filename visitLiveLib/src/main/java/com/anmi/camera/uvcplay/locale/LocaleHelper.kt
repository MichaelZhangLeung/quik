package com.anmi.camera.uvcplay.locale

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.preference.PreferenceManager
import com.base.MyLog
import java.util.Locale

object LocaleHelper {
    private const val KEY_LANG = "pref_lang"

    fun getSavedLanguage(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_LANG, null)
    }

    fun persistLanguage(context: Context, language: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(KEY_LANG, language).apply()
    }

    // Wrap the base context with the desired locale (returns new context)
    @SuppressLint("NewApi")
    fun wrapContext(base: Context, language: String?): Context {
        val lang = language ?: getSavedLanguage(base) ?: Locale.getDefault().language
        MyLog.e("#wrapContext lang:$lang", Throwable())
        val locale = Locale.forLanguageTag(lang) // supports "zh", "en", "zh-TW", "pt-BR"...
        MyLog.e("#wrapContext locale:$locale")
        Locale.setDefault(locale)

        val res = base.resources
        val config = Configuration(res.configuration)

        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    // one-step to change and persist language; returns context to use
    fun setNewLocale(context: Context, language: String): Context {
//        persistLanguage(context.applicationContext, language)
        return wrapContext(context.applicationContext, language)
    }
}
