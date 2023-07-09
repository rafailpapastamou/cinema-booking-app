package com.example.cinemabookingapp

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

// Activity for user registration
class Register : BaseActivity() {

    private lateinit var editTextEmail: TextInputEditText // Email input field
    private lateinit var editTextPassword: TextInputEditText // Password input field
    private lateinit var editTextConfirmPassword: TextInputEditText // Confirm password input field
    private lateinit var buttonReg: Button // Register button
    private lateinit var auth: FirebaseAuth // Firebase authentication instance
    private lateinit var progressBar: ProgressBar // Progress bar
    private lateinit var textView: TextView // "Log in" TextView
    private lateinit var btnLanguageSwitch: ImageView // Language switch button
    private var isGreekEnabled = false // Flag for Greek language preference
    private val LANGUAGE_PREFERENCE_KEY = "language_preference"

    public override fun onStart() {

        auth = FirebaseAuth.getInstance() // Initialize Firebase authentication

        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        Locale.setDefault(Locale("en")); // Set the default locale to English
        isGreekEnabled = sharedPrefs.getBoolean(LANGUAGE_PREFERENCE_KEY, false) // Get the language preference from shared preferences
        setLocale(isGreekEnabled) // set initial locale
        setContentView(R.layout.activity_register)

        // Making the color of the text "Log in" a different one
        val loginNowTextView = findViewById<TextView>(R.id.loginNow) // "Log in" text view
        val loginNowText = SpannableString(loginNowTextView.text)
        val color = ContextCompat.getColor(this, R.color.basic)
        if(!isGreekEnabled) { // If English is enabled
            val startIndex = loginNowText.indexOf("Log in")
            val endIndex = startIndex + "Log in".length
            loginNowText.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            loginNowText.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Make it bold
            loginNowTextView.text = loginNowText
        }
        else{ // If Greek is enabled
            val startIndex = loginNowText.indexOf("Συνδεθείτε")
            val endIndex = startIndex + "Συνδεθείτε".length
            loginNowText.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            loginNowText.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Make it bold
            loginNowTextView.text = loginNowText
        }

        editTextEmail = findViewById(R.id.email) // Initialize the objects
        editTextPassword = findViewById(R.id.password);
        editTextConfirmPassword = findViewById(R.id.confirm_password)
        buttonReg = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar)
        textView = findViewById(R.id.loginNow)
        textView.setOnClickListener{// When the "Log in" text view is clicked, navigate to the Login activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        buttonReg.setOnClickListener {
            progressBar.visibility = View.VISIBLE // Show the progress bar when the register button is clicked
            val auth: FirebaseAuth = FirebaseAuth.getInstance() // Initialize Firebase authentication
            val email: String = editTextEmail.text.toString()
            val password: String = editTextPassword.text.toString()
            val confirmedPassword = editTextConfirmPassword.text.toString()

            // Validate email address format using a regular expression
            val emailPattern = "[a-zA-Z0-9._-]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}"
            if (!email.matches(emailPattern.toRegex())) {
                Toast.makeText(this, getString(R.string.email_error), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if(TextUtils.isEmpty(email)){ // If the email field is empty, show an error toast
                Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if(TextUtils.isEmpty(password)){ // If the password field is empty, show an error toast
                Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if(TextUtils.isEmpty(confirmedPassword)){ // If the confirm password field is empty, show an error toast
                Toast.makeText(this, getString(R.string.re_enter_pass), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password != confirmedPassword) { // If the passwords do not match, show an error toast
                Toast.makeText(baseContext, getString(R.string.do_not_match), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            // Validate password format using a regular expression
            val passwordPattern = "^[a-zA-Z0-9!@#\$%^&*()_+~`|}{\\[\\]\\\\:;\\\"'<>,.?/-]+\$"
            if (!password.matches(passwordPattern.toRegex())) {
                Toast.makeText(this, getString(R.string.password_error), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(baseContext, getString(R.string.must_contain), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        val realTimeDatabaseUrl = resources.getString(R.string.realtimeDatabase_url) // Getting the Real Time Database URL from the config file
                        val database = FirebaseDatabase.getInstance(realTimeDatabaseUrl)
                        val rootRef = database.reference

                        // Create a new user object
                        val user = HashMap<String, Any>()
                        user["email"] = email
                        user["userType"] = "user"

                        // Write the user object to the database
                        val usersRef = rootRef.child("users")
                        usersRef.child(userId!!).setValue(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, getString(R.string.registrationSuccessful), Toast.LENGTH_SHORT)
                                    .show()
                                val intent = Intent(this, Login::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.failedToRegisterUser), Toast.LENGTH_SHORT)
                                    .show()
                            }
                    } else if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.already_exists),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.registration_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


        }
        btnLanguageSwitch = findViewById(R.id.btn_language)
        btnLanguageSwitch.setOnClickListener{
            isGreekEnabled = !isGreekEnabled // Toggle the language preference
            setLocale(isGreekEnabled) // set new locale
            sharedPrefs.edit().putBoolean(LANGUAGE_PREFERENCE_KEY, isGreekEnabled).apply()
            val intent = Intent(this, Welcome::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        updateBaseContextLocale(newBase)?.let { super.attachBaseContext(it) }
    }

    private fun updateBaseContextLocale(context: Context?): Context? {
        val sharedPrefs = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        isGreekEnabled = sharedPrefs?.getBoolean(LANGUAGE_PREFERENCE_KEY, false) ?: false
        val locale = if (isGreekEnabled) Locale("el") else Locale.getDefault()
        return updateBaseContextLocale(context, locale)
    }

    private fun updateBaseContextLocale(context: Context?, locale: Locale): Context? {
        val configuration = Configuration(context?.resources?.configuration)
        configuration.setLocale(locale)
        return context?.createConfigurationContext(configuration)
    }

    private fun setLocale(isGreekEnabled: Boolean) {
        val newLocale = if (isGreekEnabled) Locale("el") else Locale.getDefault()
        val config = Configuration(resources.configuration)
        config.setLocale(newLocale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}