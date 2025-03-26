package com.example.projeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.graphics.BitmapFactory
import android.util.Base64

data class FavoritePOI(val id: String, val title: String, val imageBase64: String?, val rating: Float)

class FavoritePOIAdapter(private val favoritePOIs: List<FavoritePOI>) : RecyclerView.Adapter<FavoritePOIAdapter.FavoritePOIViewHolder>() {

    class FavoritePOIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.poi_title)
        val ratingTextView: TextView = itemView.findViewById(R.id.poi_rating)
        val imageView: ImageView = itemView.findViewById(R.id.poi_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritePOIViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite_poi, parent, false)
        return FavoritePOIViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoritePOIViewHolder, position: Int) {
        val poi = favoritePOIs[position]
        holder.titleTextView.text = poi.title
        holder.ratingTextView.text = "Rating: ${poi.rating}"

        poi.imageBase64?.let {
            val imageBytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.imageView.setImageBitmap(bitmap)
        } ?: run {
            holder.imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    override fun getItemCount() = favoritePOIs.size
}