package dev.alejandrorosas.apptemplate

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.anmi.camera.uvcplay.ui.BaseActivity

class SettingsActivity : BaseActivity(R.layout.activity_settings) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container_settings, SettingsFragment())
            .commit()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // 监听点击
        toolbar.setNavigationOnClickListener {
            // 处理返回
            finish()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }
}
