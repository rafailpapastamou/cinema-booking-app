package com.example.cinemabookingapp

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import java.util.*

class Login : BaseActivity() {

    private lateinit var editTextEmail: TextInputEditText // Declaring the editTextEmail, editTextPassword, buttonLogin objects
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonLogin: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var textView: TextView
    private lateinit var tvForgot: TextView
    private lateinit var btnLanguageSwitch: ImageView
    private var isGreekEnabled = false // Default to false
    private val LANGUAGE_PREFERENCE_KEY = "language_preference"

    public override fun onStart() {
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
        Locale.setDefault(Locale("en"));
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        isGreekEnabled = sharedPrefs.getBoolean(LANGUAGE_PREFERENCE_KEY, false)
        setLocale(isGreekEnabled) // Set initial locale
        setContentView(R.layout.activity_login)

        val registerNowTextView = findViewById<TextView>(R.id.registerNow) // Making the color of the text "Sign up" a different one
        val registerNowText = SpannableString(registerNowTextView.text)
        val color = ContextCompat.getColor(this, R.color.basic)
        if(!isGreekEnabled){ // For English
            val startIndex = registerNowText.indexOf("Sign up")
            val endIndex = startIndex + "Sign up".length
            registerNowText.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            registerNowText.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Making it bold
            registerNowTextView.text = registerNowText
        }
        else { // For Greek
            val startIndex = registerNowText.indexOf("Κάντε εγγραφή")
            val endIndex = startIndex + "Κάντε εγγραφή".length
            registerNowText.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            registerNowText.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // Making it bold
            registerNowTextView.text = registerNowText
        }



        auth = FirebaseAuth.getInstance()

        editTextEmail = findViewById(R.id.email) // Initializing the objects
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar)

        textView = findViewById(R.id.registerNow)
        textView.setOnClickListener{// When the "Register now" text view is clicked, navigate to the Register activity
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
            finish()
        }

        tvForgot = findViewById(R.id.tvForgot)
        tvForgot.setOnClickListener {

            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_forgot_password)

            val etEmail = dialog.findViewById<TextView>(R.id.et_email)
            val btnSubmit = dialog.findViewById<Button>(R.id.btn_submit)

            btnSubmit.setOnClickListener {
                val email: String = etEmail.text.toString()

                // Validate email address format using a regular expression
                val emailPattern = "[a-zA-Z0-9._-]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}"
                if (!email.matches(emailPattern.toRegex())) {
                    Toast.makeText(this, getString(R.string.email_error), Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    return@setOnClickListener
                }

                if (email.isEmpty()) {
                    Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            editTextPassword.setText("")
                            Toast.makeText(this, getString(R.string.reset_email_sent), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, getString(R.string.non_existant), Toast.LENGTH_LONG).show()
                            etEmail.text = ""
                            etEmail.clearFocus()
                            dialog.show()
                        }
                    }
                dialog.dismiss()
            }
            if(isGreekEnabled) dialog.window?.setLayout(1000, 700)
            else dialog.window?.setLayout(1000, 650)
            dialog.show()
        }

        buttonLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE // Make the progress bar visible when the button is clicked
            val auth: FirebaseAuth = FirebaseAuth.getInstance() // Initialize auth
            val email: String = editTextEmail.text.toString()
            val password: String = editTextPassword.text.toString()

            // Validate email address format using a regular expression
            val emailPattern = "[a-zA-Z0-9._-]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}"
            if (!email.matches(emailPattern.toRegex())) {
                Toast.makeText(this, getString(R.string.email_error), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(email)) { // Show an error toast if the email text input is empty
                Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(password)) { // Show an error toast if the password text input is empty
                Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(
                email,
                password
            ) // Ready-made Firebase code for the sign in function
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Closing the welcome activity too
                        startActivity(intent)
                        finish()
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthInvalidUserException) {
                            // If sign in fails due to non-existing email, display a message to the user.
                            Toast.makeText(this, getString(R.string.non_existant), Toast.LENGTH_SHORT).show()
                        } else {
                            // If sign in fails due to any other reason, display a generic message to the user.
                            Toast.makeText(this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        btnLanguageSwitch = findViewById(R.id.btn_language)
        btnLanguageSwitch.setOnClickListener{
            isGreekEnabled = !isGreekEnabled
            setLocale(isGreekEnabled) // Set new locale
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