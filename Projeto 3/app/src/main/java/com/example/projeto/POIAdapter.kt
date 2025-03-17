package com.example.projeto

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class POIAdapter(private val poiList: List<POI>, private val onClick: (POI) -> Unit) : RecyclerView.Adapter<POIAdapter.POIViewHolder>() {
    class POIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.poi_image)
        val titleTextView: TextView = itemView.findViewById(R.id.poi_title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.poi_description)
        val distanceTextView: TextView = itemView.findViewById(R.id.poi_distance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poi, parent, false)
        return POIViewHolder(view)
    }

    override fun onBindViewHolder(holder: POIViewHolder, position: Int) {
        val poi = poiList[position]
        holder.titleTextView.text = poi.title
        holder.descriptionTextView.text = poi.description
        holder.distanceTextView.text = formatDistance(poi.distance)
        holder.itemView.setOnClickListener { onClick(poi) }

        poi.imageBase64?.let {
            val imageBytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.imageView.setImageBitmap(bitmap)
        } ?: holder.imageView.setImageResource(R.drawable.placeholder_image)
    }

    override fun getItemCount() = poiList.size

    private fun formatDistance(distance: Float): String {
        return if (distance >= 1000) {
            String.format("%.2f km", distance / 1000)
        } else {
            String.format("%.0f meters", distance)
        }
    }
}