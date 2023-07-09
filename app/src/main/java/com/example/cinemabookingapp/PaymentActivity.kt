package com.example.cinemabookingapp

// Import statements
import android.content.Context
import android.content.Intent
import android.content.RestrictionsManager.RESULT_ERROR
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import javax.mail.*
import javax.mail.internet.*


private var isGreekEnabled = false // Default to false
private val LANGUAGE_PREFERENCE_KEY = "language_preference"
private lateinit var user: FirebaseUser
private lateinit var tvFirstName: TextView
private lateinit var tvLastName: TextView
private lateinit var tvEmail: TextView
private lateinit var tvPhoneNumber: TextView
private lateinit var tvMovieTitle: TextView
private lateinit var tvMovieDate: TextView
private lateinit var btnSubmit: Button

class PaymentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("en")); // Set the default locale to English
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) // Get the language preference from SharedPreferences
        isGreekEnabled = sharedPrefs.getBoolean(LANGUAGE_PREFERENCE_KEY, false)
        setLocale(isGreekEnabled) // Set the locale based on the language preference
        setContentView(R.layout.nav_activity_payment)
        title = getString(R.string.payment)

        // Set the toolbar as the support action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get the current user from FirebaseAuth
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!

        // Set up Firebase database references
        val userId = user!!.uid
        val realTimeDatabaseUrl = resources.getString(R.string.realtimeDatabase_url) // Getting the Real Time Database URL from the config file
        val database = FirebaseDatabase.getInstance(realTimeDatabaseUrl)
        val rootRef = database.reference
        val usersRef = rootRef.child("users")

        //  Getting the info of each movie from the previous activity
        val firstName = intent.getStringExtra("firstName")
        val lastName = intent.getStringExtra("lastName")
        val email = intent.getStringExtra("email")
        val phoneNumber = intent.getStringExtra("phoneNumber")
        val seatInformationList = intent.getSerializableExtra("seatInformationList") as ArrayList<BookingInformation.Quadruple<Int, String, String, Int>>
        val movieTitle = intent.getStringExtra("movieTitle")
        val movieDate = intent.getStringExtra("movieDate")
        val movieYear = intent.getStringExtra("movieYear")
        val selectedSeats = intent.getIntArrayExtra("selectedSeats")

        // Find and initialize views
        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)
        tvMovieTitle = findViewById(R.id.tvMovieTitle)
        tvMovieDate = findViewById(R.id.tvMovieDate)

        // Set text on views based on the language preference
        if (isGreekEnabled) {
            tvFirstName.text = "Όνομα: $firstName"
            tvLastName.text = "Επίθετο: $lastName"
            tvEmail.text = "Διεύθυνση email: $email"
            tvPhoneNumber.text = "Αριθμός Τηλεφώνου: $phoneNumber"
            tvMovieTitle.text = "Ταινία: $movieTitle ($movieYear)"
            tvMovieDate.text = "Ημερομηνία: $movieDate"
        } else {
            tvFirstName.text = "First Name: $firstName"
            tvLastName.text = "Last Name: $lastName"
            tvEmail.text = "Email: $email"
            tvPhoneNumber.text = "Phone Number: $phoneNumber"
            tvMovieTitle.text = "Movie: $movieTitle ($movieYear)"
            tvMovieDate.text = "Date: $movieDate"
        }

        // Set up the RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.SeatsInfoRecyclerView)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        val seatInfoAdapter = SeatInfoAdapter(seatInformationList)
        recyclerView.adapter = seatInfoAdapter


        btnSubmit = findViewById(R.id.btnSubmit)

        // Calculate the total cost
        var totalCost: Int = 0
        for (item in seatInformationList) {
            totalCost += item.fourth
        }

        // Set the text on the submit button based on the language preference
        if(isGreekEnabled){
            btnSubmit.text = "Πληρώστε [$totalCost€]\n(Test version - Δε γίνονται πληρωμές)"
        } else{
            btnSubmit.text = "Pay [$totalCost€]\n(Test version - No payments)"
        }

        btnSubmit.setOnClickListener {
            val db = FirebaseFirestore.getInstance()

            // Query the movie to check seat availability
            db.collection("movies")
                .whereEqualTo("title", movieTitle)
                .whereEqualTo("year", movieYear)
                .whereEqualTo("date", movieDate)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    var seatUnavailable = false // initialize flag to false
                    for (document in querySnapshot.documents) {
                        val seats = document.get("seats") as? List<HashMap<String, Any>>
                        if (seats != null) {
                            for (selectedSeatId in selectedSeats ?: intArrayOf()) {
                                if (selectedSeatId - 1 >= seats.size) {
                                    return@addOnSuccessListener // Exit the loop if the seat index is out of range
                                }
                                val seat = seats[selectedSeatId - 1]
                                val availability = seat["availability"] as? Boolean
                                if (availability == false) {
                                    seatUnavailable =
                                        true // Set flag to true if selected seat is unavailable
                                    break // Exit the loop if any seat is unavailable
                                }
                            }
                        }
                    }
                    if (seatUnavailable) {
                        // Show error message and finish the activity with error result
                        Toast.makeText(this, getString(R.string.selected_seats_got_taken), Toast.LENGTH_LONG).show()
                        setResult(RESULT_ERROR)
                        finish()
                    } else {
                        // Update seat availability in the database
                        // Query the movie using its title, genre, year, and date fields
                        db.collection("movies")
                            .whereEqualTo("title", movieTitle)
                            .whereEqualTo("year", movieYear)
                            .whereEqualTo("date", movieDate)
                            .get()
                            .addOnSuccessListener { querySnapshot ->

                                val movieDoc = querySnapshot.documents.firstOrNull()

                                // Get the seats array from the movie document
                                val seats =
                                    (movieDoc?.get("seats") as? List<Map<String, Any>>)?.toMutableList()

                                // Update the availability of the selected seats
                                if (selectedSeats != null) {
                                    for (seatId in selectedSeats) {
                                        if (seats != null && seatId - 1 < seats.size) {
                                            val seatMap = seats[seatId - 1].toMutableMap()
                                            seatMap["availability"] = false
                                            val seatInformation =
                                                seatInformationList.find { it.first == seatId }
                                            if (seatInformation != null) {
                                                seatMap["customerInfo"] = mapOf(
                                                    "firstName" to firstName,
                                                    "lastName" to lastName,
                                                    "email" to email,
                                                    "phoneNumber" to phoneNumber,
                                                    "ticketType" to seatInformation.third,
                                                    "seatName" to seatInformation.second,
                                                    "seatNumber" to seatId
                                                )
                                            }
                                            seats[seatId - 1] = seatMap
                                        }
                                    }
                                }

                                // Update the seats array in the movie document
                                movieDoc?.reference?.update("seats", seats)
                                    ?.addOnSuccessListener {
                                        // Availability of the selected seats has been successfully updated
                                    }
                                    ?.addOnFailureListener {
                                        // An error occurred while updating the availability of the selected seats
                                    }

                                // Sending the email-ticket
                                val mailSender = JavaMailSender()
                                val emailUsername = resources.getString(R.string.emailUsername) // Getting the email username from the config file
                                val emailPassword = resources.getString(R.string.emailPassword) // Getting the email password from the config file
                                Thread {
                                    if (email != null) {
                                        if(isGreekEnabled){ // Greek email template
                                            var message = "Αγαπητέ/ή $firstName,<br><br>Σας ευχαριστούμε για τη προτίμηση! Η κράτησή σας έχει επεξεργαστεί επιτυχώς.<br><br>Πληροφορίες ταινίας:<br>Ταινία: $movieTitle ($movieYear)<br>Ημερομηνία: $movieDate<br><br>Πληροφορίες θέσεων:<br>"
                                            for (seatInfo in seatInformationList) {
                                                message += "Θέση: ${seatInfo.first}<br>Όνομα θέσης: ${seatInfo.second}<br>Τύπος εισιτηρίου: ${seatInfo.third}<br>Κόστος: ${seatInfo.fourth}€<br><br>"
                                            }
                                            message += "<br>Συνολικό κόστος: $totalCost€<br><br>Παρακαλούμε επιβεβαιώστε την κράτησή σας επιδεικνύοντας το παρόν e-mail στο ταμείο.<br><br>Καλή προβολή!"
                                            mailSender.sendEmail(
                                                emailUsername,
                                                emailPassword,
                                                email,
                                                getString(R.string.email_subject),
                                                message
                                            )
                                        }else{ // English email template
                                            var message = "Dear $firstName,<br><br>Thank you for booking your cinema tickets with us! Your reservation has been successfully processed.<br><br>Movie information::<br>Movie: $movieTitle ($movieYear)<br>Date: $movieDate<br>"
                                            for (seatInfo in seatInformationList) {
                                                message += "Seat Information: Seat: ${seatInfo.first}<br>Seat Name: ${seatInfo.second}<br>Ticket Type: ${seatInfo.third}<br>Price: ${seatInfo.fourth}€<br><br>"
                                            }
                                            message += "<br>Total price: $totalCost€<br><br>Please confirm your reservation by presenting this email at the box office.<br><br>Enjoy the movie!"
                                            mailSender.sendEmail(
                                                emailUsername,
                                                emailPassword,
                                                email,
                                                getString(R.string.email_subject),
                                                message
                                            )
                                        }
                                    }
                                }.start()

                                val intent = Intent(applicationContext, SuccessfullPayment::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finishAffinity()
                            }
                    }
                }
        }
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

    // RecyclerView adapter for displaying seat information
    class SeatInfoAdapter(private val seatInformationList: ArrayList<BookingInformation.Quadruple<Int, String, String, Int>>) :
    RecyclerView.Adapter<SeatInfoAdapter.SeatInfoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatInfoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.seat_info_item, parent, false)
            return SeatInfoViewHolder(view)
        }

        override fun onBindViewHolder(holder: SeatInfoViewHolder, position: Int) {
            val (seatNumber, seatName, ticketType, ticketTypePrice) = seatInformationList[position]
            holder.bind(seatNumber, seatName, ticketType, ticketTypePrice)
        }

        override fun getItemCount(): Int = seatInformationList.size

        class SeatInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvSeatNumber: TextView = itemView.findViewById(R.id.tvSeatNumber)
            private val tvSeatName: TextView = itemView.findViewById(R.id.tvSeatName)
            private val tvTicketCost: TextView = itemView.findViewById(R.id.tvTicketCost)
            private val tvTicketType: TextView = itemView.findViewById(R.id.tvTicketType)

            fun bind(seatNumber: Int, seatName: String, ticketType:String, ticketTypePrice: Int) {
                tvSeatNumber.text = seatNumber.toString()
                tvSeatName.text = seatName
                tvTicketType.text = ticketType
                tvTicketCost.text = "$ticketTypePrice€"
            }
        }
    }

    // Helper class for sending emails
    class JavaMailSender {
        fun sendEmail(username: String, password: String, recipientEmail: String, subject: String, body: String) {
            val props = Properties()
            props.setProperty("mail.transport.protocol", "smtp")
            props.setProperty("mail.smtp.host", "smtp.gmail.com")
            props.setProperty("mail.smtp.auth", "true")
            props.setProperty("mail.smtp.port", "587")
            props.setProperty("mail.smtp.starttls.enable", "true")

            val session = Session.getDefaultInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, null)
                }
            })

            try {
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(username))
                message.addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                message.subject = subject
                message.setContent(body, "text/html; charset=utf-8")

                val transport = session.getTransport("smtp")
                transport.connect("smtp.gmail.com", 587, username, password)

                transport.sendMessage(message, message.allRecipients)
                println("Email sent successfully")
            } catch (e: MessagingException) {
                println("Failed to send email")
                e.printStackTrace()
            }
        }
    }
}