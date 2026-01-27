package dev.alejandrorosas.apptemplate

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.anmi.camera.uvcplay.locale.LocaleHelper
import com.base.ThreadHelper
import com.base.log.Dlog
import dev.alejandrorosas.streamlib.StreamBridge

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey)
        val langPref = findPreference<ListPreference>("pref_lang")
        // 如果使用 app:useSimpleSummaryProvider="true"，系统会自动显示 entry 作为 summary
        // 这里我们仍然手动设置 listener，用来立即应用语言并触发刷新
        langPref?.setOnPreferenceChangeListener { preference, newValue ->
            Dlog.e("#onPreferenceChange preference:$preference, newValue:$newValue")
            val newLang = newValue as String // e.g. "zh", "en", "zh-CN"

            if (newLang == LocaleHelper.getSavedLanguage(requireContext())){
                return@setOnPreferenceChangeListener false
            }
//            val index = (preference as ListPreference).findIndexOfValue(newLang)
//            if (index >= 0) {
//                preference.summary = preference.entries[index]
//            }

            LocaleHelper.setNewLocale(requireContext(), newLang)

            ThreadHelper.getInstance().postToUIThread(
                {
//                    restartAppAndKillProcess(requireContext())
                    val intent = Intent().apply {
                        putExtra("setting_result", 1000)//1000 表示重启
                    }
                    requireActivity().setResult(Activity.RESULT_OK, intent)
                    requireActivity().finish()
                },
                100L,
            )
            // 返回 true 表示允许 Preference 保存新值
            true
        }
    }

    private fun restartAppAndKillProcess(context: Context) {
//        val pm = context.packageManager
//        val intent = pm.getLaunchIntentForPackage(context.packageName) ?: return
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
//        val pendingFlags =
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
//                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            else
//                PendingIntent.FLAG_CANCEL_CURRENT
//        val pending = PendingIntent.getActivity(context, 0, intent, pendingFlags)
//        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        am.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, pending)
        android.os.Process.killProcess(android.os.Process.myPid())
        kotlin.system.exitProcess(0)
    }

}
