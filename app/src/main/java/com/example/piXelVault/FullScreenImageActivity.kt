package com.example.piXelVault

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.demo1.R
import java.io.File

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val fullScreenImageView: ImageView = findViewById(R.id.fullScreenImageView)

        // Retrieve the image file path from the intent
        val imagePath = intent.getStringExtra("image_uri")

        // Ensure the path is not null and the file exists
        if (imagePath != null) {
            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                // Decode the image file into a Bitmap
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                fullScreenImageView.setImageBitmap(bitmap)
            }
        }

        // Set a click listener to close the activity when the image is clicked
        fullScreenImageView.setOnClickListener {
            finish() // Close the full-screen activity
        }
    }
}
