package com.example.cinemabookingapp

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.collections.HashMap

private var isGreekEnabled = false // default to false
private val LANGUAGE_PREFERENCE_KEY = "language_preference"
private lateinit var tvMovieInfo: TextView
private lateinit var progressBar: ProgressBar

class SoldTicketsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("en"));
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) // Retrieve language preference from shared preferences
        isGreekEnabled = sharedPrefs.getBoolean(LANGUAGE_PREFERENCE_KEY, false)
        setLocale(isGreekEnabled) // set initial locale
        setContentView(R.layout.nav_activity_sold_tickets)
        title = getString(R.string.sold_tickets)

        // Set the toolbar as the support action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val movieTitle = intent.getStringExtra("movieName")
        val movieYear = intent.getStringExtra("movieYear")
        val movieDate = intent.getStringExtra("movieDate")
        tvMovieInfo = findViewById(R.id.tvMovieInfo)
        tvMovieInfo.text = "$movieTitle ($movieYear) - $movieDate"

        // Show progress bar while loading data
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // Retrieve sold ticket information from Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("movies")
            .whereEqualTo("title", movieTitle)
            .whereEqualTo("year", movieYear)
            .whereEqualTo("date", movieDate)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val boughtTicketItems = mutableListOf<BoughtTicketItem>()
                for (document in querySnapshot.documents) {
                    val seats = document.get("seats") as List<HashMap<String, Any>>
                    for (seat in seats) {
                        val availability = seat["availability"] as Boolean
                        if (!availability) {
                            val customerInfo = seat["customerInfo"] as HashMap<String, Any>
                            val seatName = customerInfo["seatName"] as String
                            val ticketType = customerInfo["ticketType"] as String
                            val firstName = customerInfo["firstName"] as String
                            val lastName = customerInfo["lastName"] as String
                            val email = customerInfo["email"] as String
                            val phoneNumber = customerInfo["phoneNumber"] as String
                            val seatNumber = customerInfo["seatNumber"] as Number
                            val boughtTicketItem = BoughtTicketItem(seatName, ticketType, firstName, lastName, email, phoneNumber, seatNumber)
                            boughtTicketItems.add(boughtTicketItem)
                        }
                    }
                }
                // Set up RecyclerView to display the bought ticket items
                val recyclerView: RecyclerView = findViewById(R.id.boughtTicketsRecyclerView)
                val layoutManager = LinearLayoutManager(this)
                recyclerView.layoutManager = layoutManager
                recyclerView.adapter = BoughtTicketsAdapter(boughtTicketItems)

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load sold seats", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
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

    data class BoughtTicketItem(
        val seatName: String,
        val ticketType: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val phoneNumber: String,
        val seatNumber: Number
    )

    class BoughtTicketsAdapter(private val boughtTicketList: List<BoughtTicketItem>) :
        RecyclerView.Adapter<BoughtTicketsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Inflate the layout for each item in the RecyclerView
            val view = LayoutInflater.from(parent.context).inflate(R.layout.sold_ticket_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val currentBoughtTicket = boughtTicketList[position]

            // Set the data for each item in the RecyclerView
            holder.seatNumberTextView.text = currentBoughtTicket.seatNumber.toString()
            if(isGreekEnabled){ // If the locale is Greek
                holder.seatNameTextView.text = "Όνομα Θέσης: " + currentBoughtTicket.seatName
                holder.ticketTypeTextView.text = "Είδος Εισιτηρίου: " + currentBoughtTicket.ticketType
                holder.firstNameTextView.text = "Όνομα: " + currentBoughtTicket.firstName
                holder.lastNameTextView.text = "Επίθετο: " + currentBoughtTicket.lastName
                holder.emailTextView.text = "Email: " + currentBoughtTicket.email
                holder.phoneNumberTextView.text = "Αριθμός Τηλεφώνου: " + currentBoughtTicket.phoneNumber
            } else { // If the locale is English
                holder.seatNameTextView.text = "Seat Name: " + currentBoughtTicket.seatName
                holder.ticketTypeTextView.text = "Ticket Type: " + currentBoughtTicket.ticketType
                holder.firstNameTextView.text = "First Name: " + currentBoughtTicket.firstName
                holder.lastNameTextView.text = "Last Name: " + currentBoughtTicket.lastName
                holder.emailTextView.text = "Email: " + currentBoughtTicket.email
                holder.phoneNumberTextView.text = "Phone Number: " + currentBoughtTicket.phoneNumber
            }
        }

        override fun getItemCount() = boughtTicketList.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val seatNameTextView: TextView = itemView.findViewById(R.id.tvSeatName)
            val ticketTypeTextView: TextView = itemView.findViewById(R.id.tvTicketType)
            val firstNameTextView: TextView = itemView.findViewById(R.id.tvFirstName)
            val lastNameTextView: TextView = itemView.findViewById(R.id.tvLastName)
            val emailTextView: TextView = itemView.findViewById(R.id.tvEmail)
            val phoneNumberTextView: TextView = itemView.findViewById(R.id.tvPhoneNumber)
            val seatNumberTextView: TextView = itemView.findViewById(R.id.tvSeatNumber)
        }
    }
}