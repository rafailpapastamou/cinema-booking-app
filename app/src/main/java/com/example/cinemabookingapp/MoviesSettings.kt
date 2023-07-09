package com.example.cinemabookingapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class MoviesSettings : BaseActivity() {

    private lateinit var floatButton: FloatingActionButton
    private lateinit var date: String
    val PICK_IMAGE_REQUEST = 1 // This is for opening the gallery and selecting an image as the movie cover
    private lateinit var imageUri: Uri
    private lateinit var ivFileUpload: ImageView
    private lateinit var tvFileUpload: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBar2: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("en"));
        setContentView(R.layout.nav_movies_settings)
        title = getString(R.string.movies_section)

        // Initializing the data
        date = "0"

        // Initializing the imageUri
        imageUri = Uri.EMPTY

        // Set the toolbar as the support action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // For the floating button
        floatButton = findViewById(R.id.floatButton)

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

        // Define the menu items
        val popupMenu: PopupMenu = PopupMenu(this, floatButton)
        popupMenu.menu.add(getString(R.string.add_a_movie))

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                getString(R.string.add_a_movie) -> {
                    val dialog = Dialog(this)
                    dialog.setContentView(R.layout.dialog_edit_movie)
                    val submitButton = dialog.findViewById<Button>(R.id.submit_button) // finding the submit button
                    val dialogTitle = dialog.findViewById<TextView>(R.id.tvTitle)
                    dialogTitle.setText(R.string.add_a_movie)
                    val movieNameEditText = dialog.findViewById<EditText>(R.id.ti_movie_name) // finding the EditTexts
                    val movieGenreEditText = dialog.findViewById<EditText>(R.id.ti_movie_genre)
                    val movieReleaseDateEditText = dialog.findViewById<EditText>(R.id.ti_movie_release_date)
                    val calendarButton = dialog.findViewById<ImageView>(R.id.ivCalendar)
                    val fileUploadButton = dialog.findViewById<ImageView>(R.id.ivFileUpload)
                    val tvDateTime = dialog.findViewById<TextView>(R.id.tvDateTime)
                    tvFileUpload = dialog.findViewById<TextView>(R.id.tvFileUpload)
                    ivFileUpload = dialog.findViewById<ImageView>(R.id.ivFileUpload) // initialize the variable here
                    progressBar2 = dialog.findViewById<ProgressBar>(R.id.progressBar2)

                    calendarButton.setOnClickListener{
                        // Create a calendar object to set the initial date and time
                        val calendar = Calendar.getInstance()

                        // Create a DatePickerDialog and set the initial date
                        val datePickerDialog = DatePickerDialog(
                            this,
                            { _, year, monthOfYear, dayOfMonth ->
                                // Set the year, month, and day of the calendar object
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, monthOfYear)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                // Create a TimePickerDialog and show it
                                val timePickerDialog = TimePickerDialog(
                                    this,
                                    { _, hourOfDay, minute ->
                                        // Set the hour and minute of the calendar object
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)

                                        // Update the text of the date and time textView
                                        val currentLocale = resources.configuration.locales.get(0)
                                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", currentLocale)
                                        tvDateTime.text = (dateFormat.format(calendar.time))
                                        date = (dateFormat.format(calendar.time)).toString()
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true // set to true to display in 24-hour format
                                )
                                timePickerDialog.show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )

                        // Show the DatePickerDialog
                        datePickerDialog.show()
                    }

                    tvDateTime.setOnClickListener{
                        // Create a calendar object to set the initial date and time
                        val calendar = Calendar.getInstance()

                        // Create a DatePickerDialog and set the initial date
                        val datePickerDialog = DatePickerDialog(
                            this,
                            { _, year, monthOfYear, dayOfMonth ->
                                // Set the year, month, and day of the calendar object
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, monthOfYear)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                // Create a TimePickerDialog and show it
                                val timePickerDialog = TimePickerDialog(
                                    this,
                                    { _, hourOfDay, minute ->
                                        // Set the hour and minute of the calendar object
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)

                                        // Update the text of the date and time textView
                                        val currentLocale = resources.configuration.locales.get(0)
                                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", currentLocale)
                                        tvDateTime.text = (dateFormat.format(calendar.time))
                                        date = (dateFormat.format(calendar.time)).toString()
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true // set to true to display in 24-hour format
                                )
                                timePickerDialog.show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )

                        // Show the DatePickerDialog
                        datePickerDialog.show()
                    }

                    // Uploading the image
                    fileUploadButton.setOnClickListener{
                        val intent = Intent(Intent.ACTION_PICK)
                        intent.type = "image/*"
                        startActivityForResult(intent, PICK_IMAGE_REQUEST)
                    }

                    tvFileUpload.setOnClickListener{
                        val intent = Intent(Intent.ACTION_PICK)
                        intent.type = "image/*"
                        startActivityForResult(intent, PICK_IMAGE_REQUEST)
                    }

                    submitButton.setOnClickListener {
                        val movieTitle = movieNameEditText.text.toString()
                        if (TextUtils.isEmpty(movieTitle)){
                            Toast.makeText(this,getString(R.string.please_enter_a_movie_title), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val movieGenre = movieGenreEditText.text.toString()
                        if (TextUtils.isEmpty(movieGenre)){
                            Toast.makeText(this,getString(R.string.please_enter_a_movie_genre), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val movieReleaseDate = movieReleaseDateEditText.text.toString()
                        if (TextUtils.isEmpty(movieReleaseDate)){
                            Toast.makeText(this,getString(R.string.please_enter_a_movie_release_date), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if(date=="0"){
                            Toast.makeText(this,getString(R.string.please_pick_a_date_and_time), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if(imageUri == Uri.EMPTY){
                            Toast.makeText(this,getString(R.string.please_select_an_image_to_upload), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        // Get a reference to the Firebase Storage instance
                        val storage = FirebaseStorage.getInstance()

                        // Create a reference to the image in Firebase Storage
                        val imageRef = storage.reference.child("images/${UUID.randomUUID()}")

                        progressBar2.visibility = View.VISIBLE

                        // Upload the image to Firebase Storage
                        imageRef.putFile(imageUri)
                            .addOnSuccessListener {
                                // Get the download URL of the uploaded image
                                imageRef.downloadUrl.addOnSuccessListener { uri ->
                                    val downloadUrl = uri.toString()

                                    val db = FirebaseFirestore.getInstance()

                                    val seats = mutableListOf<Map<String, Any>>()
                                    for (i in 1..50) {
                                            seats.add(mapOf("availability" to true))
                                    }

                                    val movie = Movie(movieTitle, movieGenre, movieReleaseDate, date, downloadUrl, seats)
                                    val movieDocRef = db.collection("movies").document()

                                    // Set the movie data in the document
                                    movieDocRef.set(movie.toMap())
                                        .addOnSuccessListener {
                                            // Data has been successfully written to Firestore
                                            Toast.makeText(this, getString(R.string.movie_added), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        }
                                        .addOnFailureListener {
                                            // An error occurred while writing the data to Firestore
                                            Toast.makeText(this, getString(R.string.failed_to_add_movie), Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                // An error occurred while uploading the image to Firebase Storage
                                Toast.makeText(this, getString(R.string.failed_to_upload_image), Toast.LENGTH_SHORT).show()
                            }

                    }
                    dialog.window?.setLayout(1000, 1300)
                    dialog.show()
                }
            }
            true
        }
        floatButton.setOnClickListener { popupMenu.show() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // The back arrow icon in the action bar to work as a back button
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data!!;
            ivFileUpload.setImageURI(imageUri)
            tvFileUpload.text = getString(R.string.image_uploaded)
        }
    }
}

data class Movie(val title: String, val genre: String, val year: String, val date: String, val image: String,var seats: List<Map<String, Any>>) {

    // default constructor
    constructor() : this( "", "", "", "", "", emptyList())

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["title"] = title
        map["genre"] = genre
        map["year"] = year
        map["date"] = date
        map["image"] = image
        map["seats"] = seats
        return map
    }
}

class MovieAdapter(private val movies: List<Movie>) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.movie_item, parent, false)
        return MovieViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int {
        return movies.size
    }

    class MovieViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val titleTextView: TextView = itemView.findViewById(R.id.movie_title)
        private val genreTextView: TextView = itemView.findViewById(R.id.movie_genre)
        private val releaseDateTextView: TextView = itemView.findViewById(R.id.movie_release_date)
        private val dateTextView: TextView = itemView.findViewById(R.id.movie_date_time)
        private val imageImageView: ImageView = itemView.findViewById(R.id.movie_image)
        private var currentMovie: Movie? = null
        private val db = FirebaseFirestore.getInstance()

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(movie: Movie) {
            currentMovie = movie
            titleTextView.text = movie.title
            genreTextView.text = movie.genre
            releaseDateTextView.text = movie.year
            dateTextView.text = movie.date

            Glide.with(itemView)
                .load(movie.image)
                .into(imageImageView)
        }

        override fun onClick(view: View?) {
            val popupMenu = PopupMenu(itemView.context, view)
            popupMenu.menu.add(Menu.NONE, 0, 0, context.getString(R.string.remove_this_movie))
            popupMenu.menu.add(Menu.NONE, 1, 1, context.getString(R.string.view_tickets_sold))
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    0 -> {
                        val builder = AlertDialog.Builder(context)
                        builder.setView(R.layout.dialog_delete_movie)

                        val dialog = builder.create()
                        dialog.show()

                        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
                        cancelButton.setOnClickListener {
                            dialog.dismiss()
                        }

                        val yesButton = dialog.findViewById<Button>(R.id.yes_button)
                        yesButton.setOnClickListener {
                            db.collection("movies")
                                .whereEqualTo("title", currentMovie?.title)
                                .whereEqualTo("genre", currentMovie?.genre)
                                .whereEqualTo("year", currentMovie?.year)
                                .whereEqualTo("date", currentMovie?.date)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    val documentSnapshot = querySnapshot.documents.firstOrNull()
                                    documentSnapshot?.let { document ->
                                        db.collection("movies")
                                            .document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(itemView.context, context.getString(R.string.successfully_removed_movie), Toast.LENGTH_SHORT).show()
                                                (itemView.context as Activity).recreate()
                                                dialog.dismiss()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(itemView.context, context.getString(R.string.failed_to_remove_movie), Toast.LENGTH_SHORT).show()
                                                dialog.dismiss()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(itemView.context, context.getString(R.string.failed_to_remove_movie), Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }
                        }

                        true
                    }
                    1 -> {
                        val intent = Intent(itemView.context, SoldTicketsActivity::class.java)
                        intent.putExtra("movieName", currentMovie?.title)
                        intent.putExtra("movieYear", currentMovie?.year)
                        intent.putExtra("movieDate", currentMovie?.date)
                        itemView.context.startActivity(intent)

                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }
}