package com.example.cinemabookingapp

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.*

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        Locale.setDefault(Locale("en"));
        val sharedPrefs = newBase.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isGreekEnabled = sharedPrefs.getBoolean("language_preference", false)
        val locale = if (isGreekEnabled) Locale("el") else Locale.getDefault()
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}