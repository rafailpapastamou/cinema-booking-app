package com.example.cinemabookingapp

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import java.util.*

class Admin : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("en"));
        setContentView(R.layout.nav_activity_admin)
        title = getString(R.string.admin_settings)

        // Set the toolbar as the support action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsActivity.SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Making the back arrow icon in the action bar to work as a back button
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}