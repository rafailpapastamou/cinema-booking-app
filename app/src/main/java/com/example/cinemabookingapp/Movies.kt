package com.example.cinemabookingapp

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Movies : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var tvUserEmail: TextView
    private lateinit var navigationView: NavigationView
    private lateinit var tvHello: TextView
    private var isGreekEnabled = false // default to false
    private val LANGUAGE_PREFERENCE_KEY = "language_preference"
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Locale.setDefault(Locale("en")); // Set the default locale to English
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        isGreekEnabled = sharedPrefs.getBoolean(LANGUAGE_PREFERENCE_KEY, false)
        setLocale(isGreekEnabled) // set initial locale

        setContentView(R.layout.nav_activity_movies)
        title = getString(R.string.movies);

        // Initialize the Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get the DrawerLayout and set a click listener on the toolbar
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set a back button listener to close the navigation drawer
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!

        // Users
        val userId = user!!.uid
        val realTimeDatabaseUrl = resources.getString(R.string.realtimeDatabase_url) // Getting the Real Time Database URL from the config file
        val database = FirebaseDatabase.getInstance(realTimeDatabaseUrl)
        val rootRef = database.reference
        val usersRef = rootRef.child("users")

        // For the movie list
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val moviesCollection = FirebaseFirestore.getInstance().collection("movies")

        val recyclerView: RecyclerView = findViewById(R.id.moviesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        moviesCollection.get().addOnSuccessListener { result ->
            progressBar.visibility = View.GONE
            val movies = mutableListOf<Movie>()
            for (document in result) {
                val movie = document.toObject(Movie::class.java)
                movies.add(movie)
            }
            val sortedMovies = movies.sortedBy { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(it.date) }
            val movieAdapter = MovieAdapter(sortedMovies)
            recyclerView.adapter = movieAdapter
        }

        navigationView = findViewById(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        tvUserEmail = headerView.findViewById(R.id.userEmail)
        tvHello = headerView.findViewById(R.id.tvHello)

        user = auth.currentUser!!

        if (user==null){
            val intent = Intent(applicationContext, Welcome::class.java)
            startActivity(intent)
            finish()
        }
        else {
            tvUserEmail.text = user.email // Show the user's email in the navigation drawer header
        }

        val navView = findViewById<NavigationView>(R.id.nav_view)
        val adminItem = navView.menu.findItem(R.id.nav_admin)

        // Read the user type from the database
        val userTypeRef = usersRef.child(userId).child("userType")
        userTypeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userType = dataSnapshot.getValue(String::class.java)
                if (userType == "admin") { // Show admin settings only to admins and employees
                    adminItem?.isVisible = true
                    tvHello.text = getString(R.string.admin)
                } else if(userType == "user"){
                    adminItem?.isVisible = false
                    tvHello.text = getString(R.string.hello)
                } else if(userType == "employee"){
                    adminItem?.isVisible = true
                    tvHello.text = getString(R.string.employee)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(ContentValues.TAG, "onCancelled: ${databaseError.message}")
            }
        })

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_sign_out -> {
                    // Sign out from Firebase
                    auth.signOut()

                    // Start the Welcome activity and finish this activity
                    startActivity(Intent(this, Welcome::class.java))
                    finish()
                }
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                R.id.nav_admin -> {
                    startActivity(Intent(this, Admin::class.java))
                }
                R.id.nav_info -> {
                    showInfoDialog()
                }
                R.id.nav_language -> {
                    isGreekEnabled = !isGreekEnabled
                    setLocale(isGreekEnabled) // set new locale
                    sharedPrefs.edit().putBoolean(LANGUAGE_PREFERENCE_KEY, isGreekEnabled).apply()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_delete_account -> {
                    val dialog = Dialog(this)
                    dialog.setContentView(R.layout.dialog_delete_account)

                    // Find the "Confirm" button in the dialog
                    val yesButton = dialog.findViewById<Button>(R.id.yes_button)

                    // Set an OnClickListener for the "Yes" button to open the new dialog box
                    yesButton.setOnClickListener {
                        val passwordDialog = Dialog(this)
                        passwordDialog.setContentView(R.layout.dialog_delete_account_password)

                        // Find the "Confirm" button in the password dialog
                        val confirmButton = passwordDialog.findViewById<Button>(R.id.delete_button)

                        // Set an OnClickListener for the "Confirm" button to delete the user's account
                        confirmButton.setOnClickListener {
                            val user = auth.currentUser

                            val passwordEditText = passwordDialog.findViewById<EditText>(R.id.password)
                            val password = passwordEditText.text.toString()

                            if (password.isEmpty()) {
                                Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }

                            val credential = EmailAuthProvider.getCredential(user?.email!!, password)

                            user?.reauthenticate(credential)
                                ?.addOnCompleteListener { reauthTask ->
                                    if (reauthTask.isSuccessful) {
                                        val userRef = usersRef.child(userId) // Τo delete the user from the realtime database as well
                                        userRef.removeValue()
                                        userRef.removeValue()
                                        user.delete()
                                            .addOnCompleteListener { deleteTask ->
                                                if (deleteTask.isSuccessful) {
                                                    // Account deleted successfully
                                                    Toast.makeText(this, getString(R.string.acc_deleted), Toast.LENGTH_SHORT).show()
                                                    startActivity(Intent(this, Welcome::class.java))
                                                    finish()
                                                } else {
                                                    // Account deletion failed
                                                    Toast.makeText(this, getString(R.string.failed_to_delete_acc), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    } else {
                                        // Reauthentication failed
                                        Toast.makeText(this, getString(R.string.wrong_password), Toast.LENGTH_SHORT).show()
                                        passwordEditText.setText("")
                                        passwordEditText.clearFocus()
                                        passwordDialog.show()
                                    }
                                }

                            passwordDialog.dismiss()
                        }

                        // Find the "Cancel" button in the password dialog
                        val cancelButton = passwordDialog.findViewById<Button>(R.id.cancel_button)

                        // Set an OnClickListener for the "Cancel" button to dismiss the password dialog
                        cancelButton.setOnClickListener {
                            passwordDialog.dismiss()
                        }

                        // Show the password dialog
                        passwordDialog.window?.setLayout(900, 650)
                        passwordDialog.show()
                        dialog.dismiss() // Na kleinei to arxiko dialog otan anoigei to deytero
                    }

                    // Find the "Cancel" button in the dialog
                    val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)

                    // Set an OnClickListener for the "Cancel" button to dismiss the dialog
                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }

                    // Show the dialog
                    dialog.window?.setLayout(900, 600)
                    dialog.show()
                }
                R.id.nav_change_password -> {
                    val dialog = Dialog(this)
                    dialog.setContentView(R.layout.dialog_change_password)

                    val oldPasswordEditText =
                        dialog.findViewById<EditText>(R.id.old_password_edit_text)
                    val newPasswordEditText =
                        dialog.findViewById<EditText>(R.id.new_password_edit_text)
                    val confirmNewPasswordEditText =
                        dialog.findViewById<EditText>(R.id.confirm_new_password_edit_text)

                    val submitButton = dialog.findViewById<Button>(R.id.submit_button)
                    submitButton.setOnClickListener {
                        val oldPassword = oldPasswordEditText.text.toString()
                        val newPassword = newPasswordEditText.text.toString()
                        val confirmPassword = confirmNewPasswordEditText.text.toString()

                        // Check if all fields are not empty
                        if (oldPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {

                            // Check if new password and confirm password fields match
                            if (newPassword == confirmPassword) {

                                // Validate password format using a regular expression
                                val passwordPattern = "^[a-zA-Z0-9!@#\$%^&*()_+~`|}{\\[\\]\\\\:;\\\"'<>,.?/-]+\$"
                                if (!newPassword.matches(passwordPattern.toRegex())) {
                                    Toast.makeText(this, getString(R.string.password_error), Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }

                                val user = auth.currentUser
                                val credential =
                                    EmailAuthProvider.getCredential(user?.email!!, oldPassword)

                                // Prompt the user to re-provide their sign-in credentials
                                user.reauthenticate(credential)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            user.updatePassword(newPassword)
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        // Password updated successfully
                                                        Toast.makeText(
                                                            this,
                                                            getString(R.string.password_updated_successfully),
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    } else {
                                                        // Password update failed
                                                        Toast.makeText(
                                                            this,
                                                            getString(R.string.must_contain),
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        newPasswordEditText.setText("")
                                                        confirmNewPasswordEditText.setText("")
                                                        oldPasswordEditText.clearFocus()
                                                        newPasswordEditText.clearFocus()
                                                        confirmNewPasswordEditText.clearFocus()
                                                        dialog.show()
                                                    }
                                                }
                                        } else {
                                            // Re-authentication failed
                                            Toast.makeText(this, getString(R.string.wrong_old_password), Toast.LENGTH_LONG).show()
                                            dialog.show()
                                        }
                                    }
                                dialog.dismiss()
                            } else {
                                // Passwords do not match
                                Toast.makeText(this, getString(R.string.do_not_match), Toast.LENGTH_LONG).show()
                                newPasswordEditText.clearFocus()
                                confirmNewPasswordEditText.clearFocus()
                                dialog.show()
                            }
                        } else {
                            // Show error message if any field is empty
                            Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_LONG).show()
                        }
                    }

                    dialog.window?.setLayout(1000, 1000)
                    dialog.show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
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

    private fun showInfoDialog() { // Function to show the info dialog box
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_info)
        // Get the OK button and set an onClickListener to dismiss the dialog
        val tvOk = dialog.findViewById<TextView>(R.id.tvOk)
        tvOk.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    data class Movie(val title: String, val genre: String, val year: String, val date: String, val image: String) {

        // default constructor
        constructor() : this("", "", "", "", "")

        fun toMap(): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            map["title"] = title
            map["genre"] = genre
            map["year"] = year
            map["date"] = date
            map["image"] = image
            return map
        }
    }

    class MovieAdapter(private val movies: List<Movie>) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.movie_item, parent, false)
            return MovieViewHolder(view)
        }

        override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
            val movie = movies[position]
            holder.bind(movie)

            // Set an OnClickListener for the item view to navigate to the seat selection activity
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, SeatSelectionActivity::class.java)
                intent.putExtra("movieTitle", movie.title)
                intent.putExtra("movieGenre", movie.genre)
                intent.putExtra("movieYear", movie.year)
                intent.putExtra("movieDate", movie.date)

                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return movies.size
        }

        class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTextView: TextView = itemView.findViewById(R.id.movie_title)
            private val genreTextView: TextView = itemView.findViewById(R.id.movie_genre)
            private val releaseDateTextView: TextView = itemView.findViewById(R.id.movie_release_date)
            private val dateTextView: TextView = itemView.findViewById(R.id.movie_date_time)
            private val imageImageView: ImageView = itemView.findViewById(R.id.movie_image)

            fun bind(movie: Movie) {
                titleTextView.text = movie.title
                genreTextView.text = movie.genre
                releaseDateTextView.text = movie.year
                dateTextView.text = movie.date

                // Load the movie image using Glide library
                Glide.with(itemView)
                    .load(movie.image)
                    .into(imageImageView)
            }
        }
    }
}