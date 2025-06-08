package com.example.piXelVault

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.demo1.R
import java.io.File

class ImageAdapter(private var images: List<Bitmap>, private val context: Context) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = images[position]
        holder.bind(image)

        holder.imageView.setOnClickListener {
            try {
                // Save the image to a temporary file
                val file = File(context.cacheDir, "image_$position.jpg")
                file.outputStream().use { outStream ->
                    image.compress(Bitmap.CompressFormat.JPEG, 80, outStream) // Compress image to save space
                }

                // Pass the file URI to the full-screen activity
                val intent = Intent(context, FullScreenImageActivity::class.java)
                intent.putExtra("image_uri", file.absolutePath) // Send file path instead of bitmap
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun getItemCount(): Int {
        return images.size
    }

    // Method to update the image data in the adapter
    fun updateData(newImages: List<Bitmap>) {
        images = newImages
        notifyDataSetChanged() // Notify the adapter to refresh the RecyclerView
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(image: Bitmap) {
            imageView.setImageBitmap(image)
        }
    }
}
