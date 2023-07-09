package com.example.cinemabookingapp

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import java.util.*

private var isGreekEnabled = false // default to false
private val LANGUAGE_PREFERENCE_KEY = "language_preference"
private lateinit var tvGoBack: TextView

class SuccessfullPayment : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("en"));
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) // Retrieve language preference from shared preferences
        isGreekEnabled = sharedPrefs.getBoolean(LANGUAGE_PREFERENCE_KEY, false)
        setLocale(isGreekEnabled) // set initial locale
        setContentView(R.layout.activity_successfull_payment)

        // Find and set click listener for "Go Back" TextView
        tvGoBack = findViewById(R.id.tvGoBack)
        tvGoBack.setOnClickListener{
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
    }

    // Update the base context locale
    private fun updateBaseContextLocale(context: Context?): Context? {
        val sharedPrefs = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        isGreekEnabled = sharedPrefs?.getBoolean(LANGUAGE_PREFERENCE_KEY, false) ?: false
        val locale = if (isGreekEnabled) Locale("el") else Locale.getDefault()
        return updateBaseContextLocale(context, locale)
    }

    // Update the base context locale with the specified locale
    private fun updateBaseContextLocale(context: Context?, locale: Locale): Context? {
        val configuration = Configuration(context?.resources?.configuration)
        configuration.setLocale(locale)
        return context?.createConfigurationContext(configuration)
    }

    // Set the locale for the activity
    private fun setLocale(isGreekEnabled: Boolean) {
        val newLocale = if (isGreekEnabled) Locale("el") else Locale.getDefault()
        val config = Configuration(resources.configuration)
        config.setLocale(newLocale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}