package com.example.piXelVault

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.demo1.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.*
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {
    private val AES_ALGORITHM = "AES/CBC/PKCS5Padding"

    private val REQUEST_CAMERA_PERMISSION = 1
    private val REQUEST_GALLERY_PERMISSION = 2
    private val PICK_IMAGE_REQUEST = 3
    private lateinit var imageUri: Uri
    private lateinit var addFromCameraBtn: FloatingActionButton
    private lateinit var chooseFromGalleryBtn: FloatingActionButton

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private lateinit var images: MutableList<Bitmap>  // Keep the list of images

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra("SKIP_AUTH_CHECK", false).not()) {
            if (isPinCreated()) {
                redirectToAuth()
                return
            } else {
                redirectToPinCreation()
                return
            }
        }
        setContentView(R.layout.activity_main)

        addFromCameraBtn = findViewById(R.id.addFromCamera)
        chooseFromGalleryBtn = findViewById(R.id.addFromGallery)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        images = loadDecryptedImages().toMutableList() // Load images at the start

        adapter = ImageAdapter(images,this) // Initialize the adapter with the loaded images
        recyclerView.adapter = adapter // Set the adapter

        addFromCameraBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            } else {
                openCamera()
            }
        }

        chooseFromGalleryBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_GALLERY_PERMISSION)
            } else {
                openGallery()
            }
        }
    }

    private fun getPaddedEncryptionKey(): String {
        // Retrieve the PIN from SharedPreferences
        val sharedPreferences = getSharedPreferences("SecureAppPrefs", Context.MODE_PRIVATE)
        val pin = sharedPreferences.getString("USER_PIN", "0000") ?: "0000" // Default to "0000" if PIN is not found

        return pin.padEnd(16, '0')
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else if (requestCode == REQUEST_GALLERY_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            try {
                val imageFile = File.createTempFile("captured_image_", ".jpg", getExternalFilesDir(null))
                imageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", imageFile)

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(takePictureIntent, REQUEST_CAMERA_PERMISSION)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error creating file for image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No Camera Available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAMERA_PERMISSION && resultCode == RESULT_OK) {
            encryptImage(imageUri)
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val selectedImageUri = data?.data
            if (selectedImageUri != null) {
                encryptImage(selectedImageUri)
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encryptImage(imageUri: Uri) {
        val imageByteArray = convertImageToByteArray(imageUri)
        val encryptedByteArray = encryptByteArray(imageByteArray)
        saveEncryptedDataToFile(encryptedByteArray)

        // Reload decrypted images after encryption
        images.clear()  // Clear the current image list
        images.addAll(loadDecryptedImages())  // Add the decrypted images again

        // Update the adapter with new images
        adapter.updateData(images)

        // Delete the original unencrypted file
        val originalFile = File(imageUri.path ?: "")
        if (originalFile.exists()) {
            originalFile.delete()
        }
    }

    private fun convertImageToByteArray(imageUri: Uri): ByteArray {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Toast.makeText(this, "Error reading image", Toast.LENGTH_SHORT).show()
                return ByteArray(0)
            }

            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            return byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error converting image to bytes", Toast.LENGTH_SHORT).show()
            return ByteArray(0)
        }
    }

    private fun encryptByteArray(inputData: ByteArray): ByteArray {
        return try {
            val secretKey = SecretKeySpec(getPaddedEncryptionKey().toByteArray(), "AES") // Use padded PIN as the key
            val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

            ByteArrayOutputStream().use { output ->
                output.write(iv)
                output.write(cipher.doFinal(inputData))
                output.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    private fun saveEncryptedDataToFile(encryptedData: ByteArray) {
        try {
            val encryptedImagesDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "EncryptedImages")
            if (!encryptedImagesDir.exists()) {
                encryptedImagesDir.mkdirs()
            }

            val encryptedFile = File(encryptedImagesDir, "encrypted_image_${System.currentTimeMillis()}.enc")
            FileOutputStream(encryptedFile).use { it.write(encryptedData) }

            Toast.makeText(this, "Encrypted file saved!", Toast.LENGTH_SHORT).show()
            Log.d("EncryptedFilePath", "Encrypted file saved at: ${encryptedFile.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving encrypted file", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadDecryptedImages(): List<Bitmap> {
        val files = listEncryptedFiles()
        return files.mapNotNull { file ->
            val decryptedData = decryptFileToByteArray(file)
            decryptedData?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } // Convert byte array to Bitmap
        }
    }


    fun listEncryptedFiles(): List<File> {
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "EncryptedImages")
        return if (directory.exists() && directory.isDirectory) {
            directory.listFiles { _, name -> name.endsWith(".enc") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun isPinCreated(): Boolean {
        val sharedPreferences = getSharedPreferences("SecureAppPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("IS_PIN_CREATED", false)
    }

    private fun redirectToAuth() {
        val intent = Intent(this, PinAuthActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun redirectToPinCreation() {
        val intent = Intent(this, CreatePinActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun decryptFileToBitmap(file: File): ByteArray? {
        return try {
            val encryptedData = file.readBytes()
            val iv = encryptedData.copyOfRange(0, 16) // The first 16 bytes are the IV
            val cipherText = encryptedData.copyOfRange(16, encryptedData.size) // The rest is the encrypted data

            val secretKey = SecretKeySpec(getPaddedEncryptionKey().toByteArray(), "AES") // Use padded PIN as the key
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            cipher.doFinal(cipherText) // Decrypt the data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decryptFileToByteArray(file: File): ByteArray? {
        return try {
            val encryptedData = file.readBytes()
            val iv = encryptedData.copyOfRange(0, 16) // The first 16 bytes are the IV
            val cipherText = encryptedData.copyOfRange(16, encryptedData.size) // The rest is the encrypted data

            val secretKey = SecretKeySpec(getPaddedEncryptionKey().toByteArray(), "AES") // Use padded PIN as the key
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(AES_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            cipher.doFinal(cipherText) // Decrypt the data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}

