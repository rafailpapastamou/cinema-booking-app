package com.example.cinemabookingapp

import android.content.Context
import android.content.Intent
import android.content.RestrictionsManager.RESULT_ERROR
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import org.w3c.dom.Text
import java.io.Serializable
import java.util.*

private var isGreekEnabled = false // Default to false
private val LANGUAGE_PREFERENCE_KEY = "language_preference"
private lateinit var user: FirebaseUser
private lateinit var etFirstName: EditText
private lateinit var etLastName: EditText
private lateinit var etEmail: EditText
private lateinit var etPhoneNumber: EditText
private lateinit var btnSubmit: Button


class BookingInformation : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("en"));
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        isGreekEnabled = sharedPrefs.getBoolean(LANGUAGE_PREFERENCE_KEY, false)
        setLocale(isGreekEnabled) // Set initial locale
        setContentView(R.layout.nav_activity_booking_information)
        title = getString(R.string.enter_information)

        // Set the toolbar as the support action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!

        // Users
        val userId = user!!.uid
        val realTimeDatabaseUrl = resources.getString(R.string.realtimeDatabase_url) // Getting the Real Time Database URL from the config file
        val database = FirebaseDatabase.getInstance(realTimeDatabaseUrl)
        val rootRef = database.reference
        val usersRef = rootRef.child("users")

        // Accessing the info of each movie from the previous activity
        val movieTitle = intent.getStringExtra("movieTitle")
        val movieDate = intent.getStringExtra("movieDate")
        val selectedSeatIds = intent.getIntArrayExtra("selectedSeatIds")
        val movieYear = intent.getStringExtra("movieYear")

        var selectedSeats: List<Int> = emptyList()
        if (selectedSeatIds != null) {
            selectedSeats = selectedSeatIds.map { it + 1 } // add 1 to each seat index
                .sorted()
        }  // sort the list in ascending order


        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)

        // Get the values from the EditText views
        val firstName = etFirstName.text.toString()
        val lastName = etLastName.text.toString()
        val email = etEmail.text.toString()
        val phoneNumber = etPhoneNumber.text.toString()

        // For the seats list
        val recyclerView: RecyclerView = findViewById(R.id.SeatsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SeatAdapter(selectedSeats)

        btnSubmit = findViewById(R.id.btnSubmit)
        btnSubmit.setOnClickListener{
            // Validate first name
            val firstName = etFirstName?.text?.toString()?.trim()
            if(firstName.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.enter_all_the_contact_info), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if(firstName.contains(" ")) {
                Toast.makeText(this, getString(R.string.first_name_cannot_contain_spaces), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Validate last name
            val lastName = etLastName?.text?.toString()?.trim()
            if(lastName.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.enter_all_the_contact_info), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if(lastName.contains(" ")) {
                Toast.makeText(this, getString(R.string.last_name_cannot_contain_spaces), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Validate email
            val email = etEmail?.text?.toString()?.trim()
            if(email.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.enter_all_the_contact_info), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if(!isValidEmail(email)) {
                Toast.makeText(this, getString(R.string.please_enter_a_valid_email_adress), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Validate phone number
            val phoneNumber = etPhoneNumber?.text?.toString()?.trim()
            if(phoneNumber.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.enter_all_the_contact_info), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if(!isValidPhoneNumber(phoneNumber)) {
                Toast.makeText(this, getString(R.string.please_enter_a_valid_phone_number), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Validate seat name
            val seatInformationList = mutableListOf<Quadruple<Int, String, String, Int>>()
            for (i in selectedSeats.indices) {
                val holder = recyclerView.findViewHolderForAdapterPosition(i) as SeatAdapter.SeatViewHolder
                val seatNumber = selectedSeats[i]
                holder.etSeatName.error = null
                val seatName = holder.etSeatName.text.toString().trim() // Get the value of etSeatName and trim it
                if (seatName.isEmpty()) { // Check if it's empty
                    holder.etSeatName.error = getString(R.string.enter_the_first_and_last_name) // Show an error message
                    return@setOnClickListener // Return to prevent the seat information from being saved
                }
                val ticketType = holder.spn_ticket_type.selectedItem.toString()
                val ticketTypePrice = holder.tvTicketCost.text.toString().replace("0", "").toInt()
                seatInformationList.add(Quadruple(seatNumber, seatName, ticketType, ticketTypePrice))
            }
            // Checking if any seat got taken by someone else
            val db = FirebaseFirestore.getInstance()
            var seatUnavailable = false // Initialize flag to false
            db.collection("movies")
                .whereEqualTo("title", movieTitle)
                .whereEqualTo("year", movieYear)
                .whereEqualTo("date", movieDate)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        val seats = document.get("seats") as List<HashMap<String, Any>>
                        selectedSeatIds?.forEach { selectedSeatId ->
                            if (selectedSeatId >= seats.size) {
                                return@addOnSuccessListener // Exit the loop if the seat index is out of range
                            }
                            val seat = seats[selectedSeatId]
                            val availability = seat["availability"] as Boolean
                            if (!availability) {
                                Toast.makeText(this, getString(R.string.selected_seats_got_taken), Toast.LENGTH_LONG).show()
                                setResult(RESULT_ERROR)
                                finish()
                                return@addOnSuccessListener // Exit the loop if any seat is unavailable
                            }
                        }
                    }
                    if (seatUnavailable) {
                        Toast.makeText(this, getString(R.string.selected_seats_got_taken), Toast.LENGTH_LONG).show()
                        recreate()
                    } else {
                        // Start the next activity
                        // Passing some info onto the next activity
                        val intent = Intent(this, PaymentActivity::class.java)
                        intent.putExtra("firstName", firstName)
                        intent.putExtra("lastName", lastName)
                        intent.putExtra("email", email)
                        intent.putExtra("phoneNumber", phoneNumber)
                        intent.putExtra("seatInformationList", ArrayList(seatInformationList))
                        intent.putExtra("movieTitle", movieTitle)
                        intent.putExtra("movieDate", movieDate)
                        intent.putExtra("movieYear", movieYear)
                        intent.putExtra("selectedSeats", selectedSeats.toIntArray())
                        startActivityForResult(intent, SeatSelectionActivity.REQUEST_CODE)
                    }
                }
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

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Function to validate phone number format
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return android.util.Patterns.PHONE.matcher(phoneNumber).matches()
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

    class SeatAdapter(private val selectedSeats: List<Int>) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

        // Create a mutable list to save the seat information in the format of Triple<Int, String, Int>
        private val seatInformationList = MutableList(selectedSeats.size) { Quadruple(selectedSeats[it], "", "",0) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.seat_item, parent, false)
            return SeatViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
            val seatNumber = selectedSeats[position]
            holder.seatNumberTextView.text = "$seatNumber"
        }

        override fun getItemCount(): Int {
            return selectedSeats.size
        }

        inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val seatNumberTextView: TextView = itemView.findViewById(R.id.tvSeatNumber)
            val etSeatName: EditText = itemView.findViewById(R.id.et_seat_name)
            val tvTicketCost: TextView = itemView.findViewById(R.id.tvTicketCost)
            val spn_ticket_type: Spinner = itemView.findViewById(R.id.spn_ticket_type)

            init {
                // Define the spinner options
                val spinnerOptions = listOf(
                    itemView.context.getString(R.string.adult_ticket),
                    itemView.context.getString(R.string.student_ticket),
                    itemView.context.getString(R.string.kid_ticket)
                )

                // Create an ArrayAdapter using the spinner options and a default spinner layout
                val spinnerAdapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, spinnerOptions)

                // Specify the layout to use when the dropdown options appear
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                // Set the spinner adapter
                spn_ticket_type.adapter = spinnerAdapter

                // Set an OnItemSelectedListener to the spinner
                spn_ticket_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        // Get the selected option from the spinner
                        val selectedOption = parent?.getItemAtPosition(position).toString()

                        var ticketTypePrice = 0
                        var ticketType = ""
                        when (selectedOption) {
                            itemView.resources.getString(R.string.adult_ticket) -> {
                                ticketType = "Adult"
                                tvTicketCost.text = "7"
                                ticketTypePrice = 7
                            }
                            itemView.resources.getString(R.string.student_ticket) -> {
                                ticketType = "Student"
                                tvTicketCost.text = "5"
                                ticketTypePrice = 5
                            }
                            itemView.resources.getString(R.string.kid_ticket) -> {
                                ticketType = "Kid (under 12)"
                                tvTicketCost.text = "4"
                                ticketTypePrice = 4
                            }
                        }

                        // Update the corresponding seat information with the selected ticket type and its price
                        val seatNumber = selectedSeats[adapterPosition]
                        val seatName = etSeatName.text.toString().trim()
                        val seatInformation = Quadruple(seatNumber, seatName, ticketType, ticketTypePrice)
                        seatInformationList[adapterPosition] = seatInformation
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Do nothing when nothing is selected
                    }
                }
            }
        }
    }

    class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D) : Serializable {
        operator fun component1(): A {
            return first
        }

        operator fun component2(): B {
            return second
        }

        operator fun component3(): C {
            return third
        }

        operator fun component4(): D {
            return fourth
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_ERROR) {
            setResult(RESULT_ERROR)
            finish()
        }
    }

    companion object {
        const val REQUEST_CODE = 1234
    }
}