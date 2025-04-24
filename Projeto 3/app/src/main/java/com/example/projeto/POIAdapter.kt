package com.example.projeto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class POIAdapter(private val poiList: List<POI>, private val onItemClick: (POI) -> Unit) : RecyclerView.Adapter<POIAdapter.POIViewHolder>() {

    class POIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.poi_title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.poi_description)
        val distanceTextView: TextView = itemView.findViewById(R.id.poi_distance)
        val imageView: ImageView = itemView.findViewById(R.id.poi_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poi, parent, false)
        return POIViewHolder(view)
    }

    override fun onBindViewHolder(holder: POIViewHolder, position: Int) {
        val poi = poiList[position]
        holder.titleTextView.text = poi.title
        holder.descriptionTextView.text = poi.description
        holder.distanceTextView.text = "${poi.distance} km"

        // Carregar a imagem, se disponível
        poi.imageBase64?.let {
            val imageBytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.imageView.setImageBitmap(bitmap)
        } ?: run {
            // Se não houver imagem, você pode exibir um placeholder ou deixar o ImageView vazio
            holder.imageView.setImageResource(R.drawable.placeholder_image) // Placeholder
        }

        holder.itemView.setOnClickListener { onItemClick(poi) }
    }
    fun updatePOIs(newPOIs: List<POI>) {
        (poiList as MutableList).clear()
        poiList.addAll(newPOIs)
        notifyDataSetChanged()
    }

    override fun getItemCount() = poiList.size
}