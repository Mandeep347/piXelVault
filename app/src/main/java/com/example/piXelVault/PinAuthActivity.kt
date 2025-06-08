package com.example.piXelVault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.demo1.R

class PinAuthActivity : AppCompatActivity() {

    private lateinit var enterPinBox: EditText
    private lateinit var submitBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_auth)

        // Initialize views
        enterPinBox = findViewById(R.id.enterPinBox)
        submitBtn = findViewById(R.id.submitBtn1)

        submitBtn.setOnClickListener {
            validatePin()
        }
    }

    private fun validatePin() {
        // Retrieve the saved PIN from SharedPreferences
        val sharedPreferences = getSharedPreferences("SecureAppPrefs", Context.MODE_PRIVATE)
        val savedPin = sharedPreferences.getString("USER_PIN", "")  // Get the PIN from SharedPreferences

        val enteredPin = enterPinBox.text.toString()

        if (enteredPin == savedPin) {
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

            // Redirect to MainActivity and finish PinAuthActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("SKIP_AUTH_CHECK", true)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show()
            enterPinBox.text.clear()
        }
    }
}
