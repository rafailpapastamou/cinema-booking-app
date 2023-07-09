package com.example.cinemabookingapp

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.os.IResultReceiver.Default
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import java.util.*

// Declare the buttons and other views
private lateinit var buttonLogIn: Button
private lateinit var buttonSignUp: Button
lateinit var auth: FirebaseAuth
private lateinit var btnLanguageSwitch: ImageView
private var isGreekEnabled = false // default to false
private val LANGUAGE_PREFERENCE_KEY = "language_preference"

class Welcome : AppCompatActivity() {

    // Check if user is already signed in and redirect to MainActivity
    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("en"));
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) // Retrieve language preference from shared preferences
        isGreekEnabled = sharedPrefs.getBoolean(LANGUAGE_PREFERENCE_KEY, false)
        setLocale(isGreekEnabled) // set initial locale
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()

        // Find and set click listeners for buttons
        buttonLogIn = findViewById(R.id.btnLogIn);
        buttonSignUp = findViewById(R.id.btnSignUp);

        buttonLogIn.setOnClickListener{
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        buttonSignUp.setOnClickListener{
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        // Find and set click listener for language switch button
        btnLanguageSwitch = findViewById(R.id.btn_languageWelcome)
        btnLanguageSwitch.setOnClickListener{
            isGreekEnabled = !isGreekEnabled
            setLocale(isGreekEnabled) // set new locale
            sharedPrefs.edit().putBoolean(LANGUAGE_PREFERENCE_KEY, isGreekEnabled).apply()
            recreate() // apply the configuration changes
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