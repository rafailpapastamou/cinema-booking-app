package com.example.cinemabookingapp

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class AdminSettings : BaseActivity() {

    private lateinit var user: FirebaseUser
    private lateinit var lvEmployees: ListView
    private lateinit var lvAdmins: ListView
    private lateinit var floatButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("en"));
        setContentView(R.layout.nav_admin_settings)
        title = getString(R.string.edit_users)

        // Set the toolbar as the support action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Users
        user = auth.currentUser!!
        val userId = user!!.uid
        val realTimeDatabaseUrl = resources.getString(R.string.realtimeDatabase_url) // Getting the Real Time Database URL from the config file
        val database = FirebaseDatabase.getInstance(realTimeDatabaseUrl)
        val rootRef = database.reference
        val usersRef = rootRef.child("users")

        val currentUserRef = rootRef.child("users").child(userId)
        currentUserRef.addListenerForSingleValueEvent(object : ValueEventListener { // If the user changes from admin or employee to something else: to be redirected
            override fun onDataChange(snapshot: DataSnapshot) {
                val userType = snapshot.child("userType").getValue(String::class.java)
                if (userType == "user") {
                    // Redirect to main activity
                    val intent = Intent(this@AdminSettings, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else if(userType == "employee"){
                    // Redirect to settings activity
                    val intent = Intent(this@AdminSettings, Admin::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminSettings, "Error checking user type: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        lvEmployees = findViewById(R.id.lvEmployees)
        lvAdmins = findViewById(R.id.lvAdmins)

        val adminsList = mutableListOf<String>()
        val employeesList = mutableListOf<String>()
        val usersList = mutableListOf<String>()

        // Retrieve the list of admins
        usersRef.orderByChild("userType").equalTo("admin").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (childSnapshot in dataSnapshot.children) {
                    val email = childSnapshot.child("email").value as String
                    adminsList.add(email)
                }
                // Create the adapter for the admins ListView
                val adminsAdapter = ArrayAdapter<String>(this@AdminSettings, android.R.layout.simple_list_item_1, adminsList)
                lvAdmins.adapter = adminsAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        // Retrieve the list of employees
        usersRef.orderByChild("userType").equalTo("employee").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (childSnapshot in dataSnapshot.children) {
                    val email = childSnapshot.child("email").value as String
                    employeesList.add(email)
                }
                // Create the adapter for the employees ListView
                val employeesAdapter = ArrayAdapter<String>(this@AdminSettings, android.R.layout.simple_list_item_1, employeesList)
                lvEmployees.adapter = employeesAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        // Create the adapter for the admins ListView
        val adminsAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, adminsList)
        lvAdmins.adapter = adminsAdapter

        // Create the adapter for the employees ListView
        val employeesAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, employeesList)
        lvEmployees.adapter = employeesAdapter

        // Gia to floating button
        floatButton = findViewById(R.id.floatButton)

        // Define the menu items
        val popupMenu: PopupMenu = PopupMenu(this, floatButton)
        popupMenu.menu.add(getString(R.string.add_an_admin))
        popupMenu.menu.add(getString(R.string.remove_an_admin))
        popupMenu.menu.add(getString(R.string.add_an_employee))
        popupMenu.menu.add(getString(R.string.remove_an_employee))

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                getString(R.string.add_an_admin) -> {
                    val dialog = Dialog(this)
                    dialog.setContentView(R.layout.dialog_admin_settings)
                    val submitButton = dialog.findViewById<Button>(R.id.submit_button) // Finding the submit button
                    val dialogTitle = dialog.findViewById<TextView>(R.id.tvTitle)
                    dialogTitle.text = getString(R.string.add_an_admin)
                    val emailEditText = dialog.findViewById<EditText>(R.id.email) // Finding the email EditText

                    submitButton.setOnClickListener {
                        val email = emailEditText.text.toString()
                        if (TextUtils.isEmpty(email)) {
                            Toast.makeText(this,getString(R.string.please_enter_an_email_address), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val usersRef = rootRef.child("users")
                        val query = usersRef.orderByChild("email").equalTo(email)
                        query.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (userSnapshot in snapshot.children) {
                                        val userId = userSnapshot.key
                                        val userType = userSnapshot.child("userType").getValue(String::class.java)
                                        if (userType == "admin") {
                                            Toast.makeText(this@AdminSettings, getString(R.string.user_is_already_an_admin), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        } else {
                                            // Update the user's admin status
                                            usersRef.child(userId!!).child("userType").setValue("admin")
                                            Toast.makeText(this@AdminSettings, getString(R.string.usser_set_as_admin), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@AdminSettings, getString(R.string.user_with_this_email_does_not_exist), Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@AdminSettings, "Error checking email: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    dialog.window?.setLayout(1000, 650)
                    dialog.show()
                }
                getString(R.string.remove_an_admin) -> {
                    val dialog = Dialog(this)
                    dialog.setContentView(R.layout.dialog_admin_settings)
                    val submitButton = dialog.findViewById<Button>(R.id.submit_button) // Finding the submit button
                    val dialogTitle = dialog.findViewById<TextView>(R.id.tvTitle)
                    dialogTitle.text = getString(R.string.remove_an_admin)
                    val emailEditText = dialog.findViewById<EditText>(R.id.email) // Finding the email EditText

                    submitButton.setOnClickListener {
                        val email = emailEditText.text.toString()
                        if (TextUtils.isEmpty(email)) {
                            Toast.makeText(this,getString(R.string.please_enter_an_email_address), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email // The currently logged in user can't change his own user type
                        if (email == currentUserEmail) {
                            Toast.makeText(this, getString(R.string.you_cannot_change_your_own_admin_status), Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@setOnClickListener
                        }

                        val usersRef = rootRef.child("users")
                        val query = usersRef.orderByChild("email").equalTo(email)
                        query.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (userSnapshot in snapshot.children) {
                                        val userId = userSnapshot.key
                                        val userType = userSnapshot.child("userType").getValue(String::class.java)
                                        if (userType != "admin") {
                                            Toast.makeText(this@AdminSettings, getString(R.string.user_is_not_an_admin), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        }else {
                                            // Update the user's admin status
                                            usersRef.child(userId!!).child("userType").setValue("user")
                                            Toast.makeText(this@AdminSettings, getString(R.string.user_is_no_longer_an_admin), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@AdminSettings, getString(R.string.user_with_this_email_does_not_exist), Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@AdminSettings, "Error checking email: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    dialog.window?.setLayout(1000, 650)
                    dialog.show()
                }
                getString(R.string.add_an_employee) -> {
                    val dialog = Dialog(this)
                    dialog.setContentView(R.layout.dialog_admin_settings)
                    val submitButton = dialog.findViewById<Button>(R.id.submit_button) // Finding the submit button
                    val dialogTitle = dialog.findViewById<TextView>(R.id.tvTitle)
                    dialogTitle.text = getString(R.string.add_an_employee)
                    val emailEditText = dialog.findViewById<EditText>(R.id.email) // Finding the email EditText

                    submitButton.setOnClickListener {
                        val email = emailEditText.text.toString()
                        if (TextUtils.isEmpty(email)) {
                            Toast.makeText(this,getString(R.string.please_enter_an_email_address), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email // The currently logged in user can't change his own user type
                        if (email == currentUserEmail) {
                            Toast.makeText(this, getString(R.string.you_cannot_change_your_own_admin_status), Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@setOnClickListener
                        }

                        val usersRef = rootRef.child("users")
                        val query = usersRef.orderByChild("email").equalTo(email)
                        query.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (userSnapshot in snapshot.children) {
                                        val userId = userSnapshot.key
                                        val userType = userSnapshot.child("userType").getValue(String::class.java)
                                        if (userType == "employee") {
                                            Toast.makeText(this@AdminSettings, getString(R.string.user_is_already_an_employee), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        } else {
                                            // Update the user's admin status
                                            usersRef.child(userId!!).child("userType").setValue("employee")
                                            Toast.makeText(this@AdminSettings, getString(R.string.user_set_as_employee), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@AdminSettings, getString(R.string.user_with_this_email_does_not_exist), Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@AdminSettings, "Error checking email: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    dialog.window?.setLayout(1000, 650)
                    dialog.show()
                }
                getString(R.string.remove_an_employee) -> {
                    val dialog = Dialog(this)
                    dialog.setContentView(R.layout.dialog_admin_settings)
                    val submitButton = dialog.findViewById<Button>(R.id.submit_button) // Finding the submit button
                    val dialogTitle = dialog.findViewById<TextView>(R.id.tvTitle)
                    dialogTitle.text = getString(R.string.remove_an_employee)
                    val emailEditText = dialog.findViewById<EditText>(R.id.email) // Finding the email EditText

                    submitButton.setOnClickListener {
                        val email = emailEditText.text.toString()
                        if (TextUtils.isEmpty(email)) {
                            Toast.makeText(this,getString(R.string.please_enter_an_email_address), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val usersRef = rootRef.child("users")
                        val query = usersRef.orderByChild("email").equalTo(email)
                        query.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (userSnapshot in snapshot.children) {
                                        val userId = userSnapshot.key
                                        val userType = userSnapshot.child("userType").getValue(String::class.java)
                                        if (userType != "employee") {
                                            Toast.makeText(this@AdminSettings, getString(R.string.user_is_not_an_employee), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        }else {
                                            // Update the user's admin status
                                            usersRef.child(userId!!).child("userType").setValue("user")
                                            Toast.makeText(this@AdminSettings, getString(R.string.user_is_no_longer_an_employee), Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                            recreate()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@AdminSettings, getString(R.string.user_with_this_email_does_not_exist), Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@AdminSettings, "Error checking email: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    dialog.window?.setLayout(1000, 650)
                    dialog.show()
                }
            }
            true
        }
        floatButton.setOnClickListener { popupMenu.show() }
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