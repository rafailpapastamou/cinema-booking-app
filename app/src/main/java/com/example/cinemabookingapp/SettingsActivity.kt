package com.example.cinemabookingapp

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        // Replace the default fragment with the SettingsFragment
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment())
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var auth: FirebaseAuth
        private lateinit var user: FirebaseUser

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Initialize Firebase Authentication and get the current user
            auth = FirebaseAuth.getInstance()
            user = auth.currentUser!!
            val userId = user!!.uid

            // Initialize Firebase Database and references
            val realTimeDatabaseUrl = resources.getString(R.string.realtimeDatabase_url) // Getting the Real Time Database URL from the config file
            val database = FirebaseDatabase.getInstance(realTimeDatabaseUrl)
            val rootRef = database.reference
            val usersRef = rootRef.child("users")

            // Find the preference for administrators
            val adminPref = findPreference<Preference>("administrators")

            // Show admin settings only to admins, not employees
            val userTypeRef = usersRef.child(userId).child("userType")
            userTypeRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userType = dataSnapshot.getValue(String::class.java)
                    if (userType == "admin") { // Show these settings to admins and employees only
                        adminPref?.isVisible = true // Show the admin preference to admins
                    } else if (userType == "employee") {
                        adminPref?.isVisible = false // Hide the admin preference from employees
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled event if needed
                }
            })

            // Set click listener for admin preference
            adminPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                // Open AdminSettings activity
                val intent = Intent(context, AdminSettings::class.java)
                startActivity(intent)
                true
            }

            // Set click listener for movies preference
            val moviesPref = findPreference<Preference>("movies")
            moviesPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                // Open MoviesSettings activity
                val intent = Intent(context, MoviesSettings::class.java)
                startActivity(intent)
                true
            }
        }
    }
}