package com.example.piXelVault

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.demo1.R

class CreatePinActivity : AppCompatActivity() {

    private lateinit var pinEditText: EditText
    private lateinit var confirmPinEditText: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_pin)

        pinEditText = findViewById(R.id.createPinEditText)
        confirmPinEditText = findViewById(R.id.confirmPinEditText)
        submitButton = findViewById(R.id.createPinSubmitBtn)

        submitButton.setOnClickListener {
            val pin = pinEditText.text.toString()
            val confirmPin = confirmPinEditText.text.toString()

            if (pin.length != 4) {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin != confirmPin) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            savePin(pin)

            Toast.makeText(this, "PIN created successfully!", Toast.LENGTH_SHORT).show()

            // Redirect to PinAuthActivity
            val intent = Intent(this, PinAuthActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun savePin(pin: String) {
        val sharedPreferences = getSharedPreferences("SecureAppPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("USER_PIN", pin)
            putBoolean("IS_PIN_CREATED", true)
            apply()
        }
    }
}
